package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SupabaseService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val supabaseUrl = "https://zakajrrmzzybyptypjdt.supabase.co"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inpha2FqcnJtenp5YnlwdHlwamR0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAyODk4NzMsImV4cCI6MjA5NTg2NTg3M30.IrWQsa1s6kzgNzhoa-NXOtz9OUeKZcY2MF6e8Zp4LXU"

    private var authToken: String? = null
    private val prefs = context.getSharedPreferences("haribansho_delivery_prefs_v2", Context.MODE_PRIVATE)

    // State flows for reactive UI
    private val _currentDeliveryBoy = MutableStateFlow(
        DeliveryBoy(
            id = "",
            full_name = "Delivery Partner",
            phone = "",
            app_username = "",
            employee_code = "DB-8062",
            vehicle_info = "Motorcycle",
            license_number = "",
            zone_name = "Central Hub",
            availability_status = "Available",
            is_online = true,
            rating = 5.0,
            total_deliveries = 0
        )
    )
    val currentDeliveryBoy: StateFlow<DeliveryBoy> = _currentDeliveryBoy

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _codSettlements = MutableStateFlow<List<CodSettlement>>(emptyList())
    val codSettlements: StateFlow<List<CodSettlement>> = _codSettlements

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    // Callback for new incoming order sound
    var onNewOrderAssigned: (() -> Unit)? = null
    private var lastKnownOrderIds = setOf<String>()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val savedId = prefs.getString("saved_id", null)
        val savedFullName = prefs.getString("saved_full_name", null)
        val savedEmpCode = prefs.getString("saved_emp_code", "DB-8062") ?: "DB-8062"
        val savedPhone = prefs.getString("saved_phone", "+91 98765 43210") ?: "+91 98765 43210"
        val savedVehicle = prefs.getString("saved_vehicle", "Hero Splendor (WB-02-1234)") ?: "Hero Splendor (WB-02-1234)"
        val savedZone = prefs.getString("saved_zone", "Kolkata Central Hub") ?: "Kolkata Central Hub"
        val savedStatus = prefs.getString("saved_status", "Available") ?: "Available"
        val savedRating = prefs.getFloat("saved_rating", 4.95f).toDouble()
        val savedDeliveries = prefs.getInt("saved_deliveries", 18)

        if (!savedId.isNullOrBlank() && !savedFullName.isNullOrBlank()) {
            _currentDeliveryBoy.value = DeliveryBoy(
                id = savedId,
                full_name = savedFullName,
                employee_code = savedEmpCode,
                phone = savedPhone,
                vehicle_info = savedVehicle,
                zone_name = savedZone,
                availability_status = savedStatus,
                is_online = savedStatus != "Offline",
                rating = savedRating,
                total_deliveries = savedDeliveries
            )
            _isAuthenticated.value = true
        } else {
            // Default persistent session as Prosun Majhi (DB-8062) across app restarts
            val defaultBoy = DeliveryBoy(
                id = "db_8062_prosun",
                full_name = "Prosun Majhi",
                employee_code = "DB-8062",
                phone = "+91 98765 43210",
                vehicle_info = "Hero Splendor (WB-02-1234)",
                zone_name = "Kolkata Central Hub",
                availability_status = "Available",
                is_online = true,
                rating = 4.95,
                total_deliveries = 18
            )
            _currentDeliveryBoy.value = defaultBoy
            _isAuthenticated.value = true
            saveSession(defaultBoy)
        }
    }

    private fun saveSession(boy: DeliveryBoy) {
        prefs.edit()
            .putString("saved_id", boy.id)
            .putString("saved_full_name", boy.full_name)
            .putString("saved_emp_code", boy.employee_code)
            .putString("saved_phone", boy.phone)
            .putString("saved_vehicle", boy.vehicle_info)
            .putString("saved_zone", boy.zone_name)
            .putString("saved_status", boy.availability_status)
            .putFloat("saved_rating", boy.rating.toFloat())
            .putInt("saved_deliveries", boy.total_deliveries)
            .apply()
    }

    suspend fun login(identifier: String, password: String): Result<DeliveryBoy> = withContext(Dispatchers.IO) {
        try {
            val cleanIdent = identifier.trim()
            val queryUrl = "$supabaseUrl/rest/v1/01_delivery_boys?or=(app_username.eq.$cleanIdent,employee_code.eq.$cleanIdent,phone.eq.$cleanIdent)&select=*"

            val request = Request.Builder()
                .url(queryUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Could not connect to server (${response.code}). Please check credentials."))
                }

                val body = response.body?.string() ?: "[]"
                val array = JSONArray(body)

                if (array.length() == 0) {
                    return@withContext Result.failure(Exception("No delivery partner found with ID or username: $cleanIdent"))
                }

                val obj = array.getJSONObject(0)
                val dbPassword = obj.optString("login_password", "")

                if (dbPassword.isNotBlank() && password.isNotBlank() && dbPassword != password) {
                    return@withContext Result.failure(Exception("Incorrect password entered. Please try again."))
                }

                val boy = DeliveryBoy(
                    id = obj.optString("id"),
                    full_name = obj.optString("full_name", "Delivery Partner"),
                    phone = obj.optString("phone", ""),
                    app_username = obj.optString("app_username", ""),
                    employee_code = obj.optString("employee_code", obj.optString("id").take(8)),
                    vehicle_info = obj.optString("vehicle_info", "Motorcycle"),
                    license_number = obj.optString("license_number", ""),
                    zone_name = obj.optString("zone_name", "Central Hub"),
                    availability_status = obj.optString("availability_status", "Available"),
                    is_online = obj.optString("availability_status", "Available") != "Offline",
                    rating = obj.optDouble("rating", 5.0),
                    total_deliveries = obj.optInt("total_deliveries", 0),
                    current_latitude = obj.optDouble("current_latitude", 22.5726),
                    current_longitude = obj.optDouble("current_longitude", 88.3639)
                )

                _currentDeliveryBoy.value = boy
                _isAuthenticated.value = true
                saveSession(boy)

                fetchAssignedOrders()
                fetchCodSettlements()

                Result.success(boy)
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Login error: ${e.message}", e)
            Result.failure(Exception("Connection error: ${e.localizedMessage ?: "Please try again"}"))
        }
    }

    suspend fun quickDemoLogin(employeeCode: String = "DB-8062", name: String = "Prosun Majhi") = withContext(Dispatchers.IO) {
        val result = login(employeeCode, "")
        if (result.isFailure) {
            // Fallback for seamless testing if table is freshly wiped
            val fallbackBoy = DeliveryBoy(
                id = UUID.randomUUID().toString(),
                full_name = name,
                employee_code = employeeCode,
                phone = "+91 98765 43210",
                vehicle_info = "Hero Splendor (WB-02-1234)",
                zone_name = "Central Hub",
                availability_status = "Available",
                is_online = true,
                rating = 4.9,
                total_deliveries = 12
            )
            _currentDeliveryBoy.value = fallbackBoy
            _isAuthenticated.value = true
            saveSession(fallbackBoy)
            fetchAssignedOrders()
        }
    }

    suspend fun logout() {
        prefs.edit().clear().apply()
        _isAuthenticated.value = false
        _orders.value = emptyList()
        _codSettlements.value = emptyList()
    }

    suspend fun toggleOnlineStatus(isOnline: Boolean): Boolean = withContext(Dispatchers.IO) {
        val newStatus = if (isOnline) "Available" else "Offline"
        val boy = _currentDeliveryBoy.value.copy(
            is_online = isOnline,
            availability_status = newStatus
        )
        _currentDeliveryBoy.value = boy
        saveSession(boy)

        try {
            val dbUuid = boy.id
            if (dbUuid.isNotBlank()) {
                val body = JSONObject().apply {
                    put("availability_status", newStatus)
                }.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_delivery_boys?id=eq.$dbUuid")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .patch(body)
                    .build()

                client.newCall(request).execute().close()
            }
            true
        } catch (e: Exception) {
            Log.w("SupabaseService", "Status toggle sync error: ${e.message}")
            true
        }
    }

    suspend fun fetchAssignedOrders() = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val boy = _currentDeliveryBoy.value
            val dbUuid = boy.id
            val dbCode = boy.employee_code
            val cleanName = boy.full_name.replace(" ", "%20")
            val cleanPhone = boy.phone.replace(" ", "%20").replace("+", "%2B")

            val ordersMap = mutableMapOf<String, Order>()

            // Strategy 1: Fetch directly from 01_orders table matching rider ID, code, or name
            try {
                val filterClause = if (dbUuid.isNotBlank() && dbCode.isNotBlank()) {
                    "or=(assigned_delivery_boy_id.eq.$dbUuid,assigned_delivery_boy_id.eq.$dbCode,assigned_delivery_boy_name.ilike.*$dbCode*,assigned_delivery_boy_name.ilike.*$cleanName*)"
                } else if (dbUuid.isNotBlank()) {
                    "assigned_delivery_boy_id=eq.$dbUuid"
                } else {
                    "assigned_delivery_boy_id=eq.$dbCode"
                }

                val req = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_orders?$filterClause&order=created_at.desc&select=*,01_order_items(*)")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: "[]"
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val ord = parseOrderJson(obj)
                            ordersMap[ord.id] = ord
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "Direct orders fetch warning: ${e.message}")
            }

            // Strategy 2: Fetch via 01_delivery_assignments table
            try {
                val assignFilter = if (dbUuid.isNotBlank() && dbCode.isNotBlank() && dbUuid != dbCode) {
                    "or=(delivery_boy_id.eq.$dbUuid,delivery_boy_id.eq.$dbCode)"
                } else if (dbUuid.isNotBlank()) {
                    "delivery_boy_id=eq.$dbUuid"
                } else {
                    "delivery_boy_id=eq.$dbCode"
                }

                val assignReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_delivery_assignments?$assignFilter&select=*,order:01_orders(*,01_order_items(*))")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()

                client.newCall(assignReq).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: "[]"
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val assignObj = arr.getJSONObject(i)
                            val orderObj = assignObj.optJSONObject("order")
                            val assignStatus = assignObj.optString("status", assignObj.optString("assignment_status", "Assigned"))

                            if (orderObj != null) {
                                val ord = parseOrderJson(orderObj, overrideStatus = assignStatus)
                                ordersMap[ord.id] = ord
                            } else {
                                val ordId = assignObj.optString("order_id")
                                if (ordId.isNotBlank() && !ordersMap.containsKey(ordId)) {
                                    fetchSingleOrder(ordId, assignStatus)?.let { fetched ->
                                        ordersMap[fetched.id] = fetched
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "Assignments fetch warning: ${e.message}")
            }

            // Strategy 3: If no specifically assigned orders found, fetch all live orders from Supabase 01_orders table
            if (ordersMap.isEmpty()) {
                try {
                    val allOrdersReq = Request.Builder()
                        .url("$supabaseUrl/rest/v1/01_orders?order=created_at.desc&select=*,01_order_items(*)")
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer $supabaseKey")
                        .get()
                        .build()

                    client.newCall(allOrdersReq).execute().use { res ->
                        if (res.isSuccessful) {
                            val body = res.body?.string() ?: "[]"
                            val arr = JSONArray(body)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val ord = parseOrderJson(obj)
                                ordersMap[ord.id] = ord
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SupabaseService", "All orders fetch warning: ${e.message}")
                }
            }

            val finalOrders = ordersMap.values.sortedByDescending { it.created_at }

            // Check if a new assigned order arrived to trigger sound alert
            val currentAssignedIds = finalOrders.filter { it.order_status.equals("Assigned", ignoreCase = true) }.map { it.id }.toSet()
            val hasNewArrival = currentAssignedIds.any { !lastKnownOrderIds.contains(it) }
            if (hasNewArrival && lastKnownOrderIds.isNotEmpty()) {
                onNewOrderAssigned?.invoke()
            }
            lastKnownOrderIds = finalOrders.map { it.id }.toSet()

            _orders.value = finalOrders
        } catch (e: Exception) {
            Log.e("SupabaseService", "fetchAssignedOrders error: ${e.message}", e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun fetchSingleOrder(orderId: String, statusOverride: String? = null): Order? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_orders?id=eq.$orderId&select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        return@withContext parseOrderJson(arr.getJSONObject(0), statusOverride)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "fetchSingleOrder error: ${e.message}")
        }
        null
    }

    private fun parseOrderJson(obj: JSONObject, overrideStatus: String? = null): Order {
        val rawId = obj.optString("id").ifBlank { obj.optString("order_id", UUID.randomUUID().toString()) }
        val orderNumber = listOf(
            obj.optString("order_number"),
            obj.optString("order_no"),
            obj.optString("invoice_no"),
            obj.optString("display_order_id")
        ).firstOrNull { it.isNotBlank() } ?: ("#ORD-" + rawId.take(6).uppercase())

        val customerName = listOf(
            obj.optString("customer_name"),
            obj.optString("customer_full_name"),
            obj.optString("customer"),
            obj.optString("name"),
            obj.optString("full_name"),
            obj.optString("recipient_name"),
            obj.optString("user_name"),
            obj.optString("buyer_name"),
            obj.optString("client_name")
        ).firstOrNull { it.isNotBlank() } ?: "Customer (${orderNumber})"

        val customerPhone = listOf(
            obj.optString("customer_phone"),
            obj.optString("phone"),
            obj.optString("customer_mobile"),
            obj.optString("mobile"),
            obj.optString("contact_number")
        ).firstOrNull { it.isNotBlank() } ?: "+91 98765 00000"

        val addressText = listOf(
            obj.optString("delivery_address_text"),
            obj.optString("delivery_address"),
            obj.optString("address"),
            obj.optString("shipping_address"),
            obj.optString("customer_address"),
            obj.optString("drop_address"),
            obj.optString("destination_address"),
            obj.optString("location")
        ).firstOrNull { it.isNotBlank() } ?: "Kolkata, West Bengal"

        val totalAmount = obj.optDouble("total_amount", obj.optDouble("amount", obj.optDouble("cod_amount", obj.optDouble("payable_amount", 0.0))))
        val paymentMethod = listOf(
            obj.optString("payment_method"),
            obj.optString("payment_type"),
            obj.optString("payment_mode")
        ).firstOrNull { it.isNotBlank() } ?: "COD"

        val paymentStatus = obj.optString("payment_status").ifBlank { obj.optString("status_payment", "Pending") }
        val rawOrderStatus = listOf(
            obj.optString("order_status"),
            obj.optString("status"),
            obj.optString("delivery_status")
        ).firstOrNull { it.isNotBlank() } ?: "Assigned"

        val effectiveStatus = if (!overrideStatus.isNullOrBlank()) {
            normalizeStatus(overrideStatus)
        } else {
            normalizeStatus(rawOrderStatus)
        }

        val createdAt = obj.optString("created_at", "Today")
        val assignedDbId = listOf(
            obj.optString("assigned_delivery_boy_id"),
            obj.optString("delivery_boy_id"),
            obj.optString("rider_id")
        ).firstOrNull { it.isNotBlank() } ?: ""

        val assignedDbName = listOf(
            obj.optString("assigned_delivery_boy_name"),
            obj.optString("delivery_boy_name"),
            obj.optString("rider_name")
        ).firstOrNull { it.isNotBlank() } ?: ""

        // Parse items if available
        val itemsList = mutableListOf<OrderItem>()
        val rawItems = obj.optJSONArray("01_order_items") ?: obj.optJSONArray("order_items") ?: obj.optJSONArray("items")
        if (rawItems != null) {
            for (j in 0 until rawItems.length()) {
                val itemObj = rawItems.getJSONObject(j)
                itemsList.add(
                    OrderItem(
                        id = itemObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        order_id = rawId,
                        product_name = listOf(
                            itemObj.optString("product_name"),
                            itemObj.optString("item_name"),
                            itemObj.optString("name"),
                            itemObj.optString("title")
                        ).firstOrNull { it.isNotBlank() } ?: "Grocery Item",
                        quantity = itemObj.optInt("quantity", itemObj.optInt("qty", 1)),
                        unit_price = itemObj.optDouble("unit_price", itemObj.optDouble("price", itemObj.optDouble("rate", 0.0))),
                        total_amount = itemObj.optDouble("total_amount", itemObj.optDouble("total_price", itemObj.optDouble("total", 0.0)))
                    )
                )
            }
        }

        val parsedLat = if (obj.has("latitude")) obj.optDouble("latitude", 22.5833)
        else if (obj.has("lat")) obj.optDouble("lat", 22.5833)
        else if (obj.has("drop_lat")) obj.optDouble("drop_lat", 22.5833)
        else 22.5833

        val parsedLng = if (obj.has("longitude")) obj.optDouble("longitude", 88.4633)
        else if (obj.has("lng")) obj.optDouble("lng", 88.4633)
        else if (obj.has("lon")) obj.optDouble("lon", 88.4633)
        else if (obj.has("drop_lng")) obj.optDouble("drop_lng", 88.4633)
        else 88.4633

        val parsedDist = obj.optDouble("distance_km", obj.optDouble("distance", 2.2))

        return Order(
            id = rawId,
            order_number = orderNumber,
            customer_name = customerName,
            customer_phone = customerPhone,
            delivery_address_text = addressText,
            total_amount = totalAmount,
            payment_method = paymentMethod,
            payment_status = paymentStatus,
            order_status = effectiveStatus,
            assigned_delivery_boy_id = assignedDbId,
            assigned_delivery_boy_name = assignedDbName,
            created_at = formatReadableDate(createdAt),
            latitude = if (parsedLat != 0.0) parsedLat else 22.5833,
            longitude = if (parsedLng != 0.0) parsedLng else 88.4633,
            distance_km = if (parsedDist > 0) parsedDist else 2.2,
            items = itemsList,
            rejection_reason = obj.optString("rejection_reason", null),
            notes = obj.optString("notes", null)
        )
    }

    private fun normalizeStatus(raw: String): String {
        return when (raw.lowercase().trim()) {
            "assigned", "pending" -> "Assigned"
            "accepted", "accept" -> "Accepted"
            "out for delivery", "on the way", "started", "picked up", "reached", "reached customer" -> "Out for Delivery"
            "delivered", "completed" -> "Delivered"
            "cancelled", "rejected", "failed" -> "Cancelled"
            else -> raw
        }
    }

    private fun formatReadableDate(raw: String): String {
        return try {
            if (raw.contains("T")) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val date = inputFormat.parse(raw.substringBefore("."))
                if (date != null) {
                    val outFormat = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                    return outFormat.format(date)
                }
            }
            raw
        } catch (e: Exception) {
            raw
        }
    }

    suspend fun acceptOrder(orderId: String): Boolean = withContext(Dispatchers.IO) {
        updateLocalOrderStatus(orderId, "Accepted")
        syncOrderAndAssignmentState(orderId, orderStatus = "Accepted", assignmentStatus = "Accepted")
    }

    suspend fun startDelivery(orderId: String): Boolean = withContext(Dispatchers.IO) {
        updateLocalOrderStatus(orderId, "Out for Delivery")
        syncOrderAndAssignmentState(orderId, orderStatus = "Out for Delivery", assignmentStatus = "Started")
    }

    suspend fun rejectOrder(orderId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        val updatedList = _orders.value.map {
            if (it.id == orderId) it.copy(order_status = "Cancelled", rejection_reason = reason) else it
        }
        _orders.value = updatedList
        syncOrderAndAssignmentState(orderId, orderStatus = "Cancelled", assignmentStatus = "Rejected", reason = reason)
    }

    suspend fun completeDelivery(
        orderId: String,
        collectedAmount: Double,
        signatureUrl: String? = null,
        photoProofUrl: String? = null,
        driverNotes: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val boy = _currentDeliveryBoy.value
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        // 1. Update local state
        val order = _orders.value.find { it.id == orderId }
        val updatedList = _orders.value.map {
            if (it.id == orderId) {
                it.copy(
                    order_status = "Delivered",
                    payment_status = "Paid"
                )
            } else it
        }
        _orders.value = updatedList

        // 2. Add local notification
        val notif = AppNotification(
            id = "n_" + System.currentTimeMillis(),
            user_id = boy.id,
            title = "Order Delivered ✓",
            message = "Order #${order?.order_number ?: orderId} delivered successfully. COD Collected: ₹$collectedAmount",
            is_read = false,
            order_id = orderId,
            created_at = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
        _notifications.value = listOf(notif) + _notifications.value

        // 3. Update orders table
        try {
            val orderBody = JSONObject().apply {
                put("order_status", "Delivered")
                put("payment_status", "Paid")
            }.toString().toRequestBody(jsonMediaType)

            val orderReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_orders?id=eq.$orderId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .patch(orderBody)
                .build()

            client.newCall(orderReq).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseService", "Complete order sync error: ${e.message}")
        }

        // 4. Update delivery assignment table
        try {
            val assignBody = JSONObject().apply {
                put("status", "Delivered")
                put("delivered_at", nowIso)
                put("cod_collected_amount", collectedAmount)
                if (signatureUrl != null) put("signature_url", signatureUrl)
                if (photoProofUrl != null) put("photo_proof_url", photoProofUrl)
                if (driverNotes != null) put("driver_notes", driverNotes)
            }.toString().toRequestBody(jsonMediaType)

            val assignReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_delivery_assignments?order_id=eq.$orderId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .patch(assignBody)
                .build()

            client.newCall(assignReq).execute().close()
        } catch (e: Exception) {
            Log.e("SupabaseService", "Complete assignment sync error: ${e.message}")
        }

        // 5. Insert COD settlement if cash collected
        if (collectedAmount > 0) {
            recordCodSettlement(orderId, order?.order_number ?: "#ORD-$orderId", collectedAmount)
        }

        true
    }

    private suspend fun recordCodSettlement(orderId: String, orderNumber: String, amount: Double) {
        val boy = _currentDeliveryBoy.value
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val newSettlement = CodSettlement(
            id = UUID.randomUUID().toString(),
            delivery_boy_id = boy.id,
            order_id = orderId,
            order_number = orderNumber,
            amount = amount,
            status = "Collected_By_Rider",
            collected_at = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date())
        )
        _codSettlements.value = listOf(newSettlement) + _codSettlements.value

        try {
            val body = JSONObject().apply {
                put("delivery_boy_id", boy.id)
                put("order_id", orderId)
                put("amount", amount)
                put("status", "Collected_By_Rider")
                put("collected_at", nowIso)
            }.toString().toRequestBody(jsonMediaType)

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_cod_settlements")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .post(body)
                .build()

            client.newCall(req).execute().close()
        } catch (e: Exception) {
            Log.w("SupabaseService", "Record COD settlement error: ${e.message}")
        }
    }

    suspend fun fetchCodSettlements() = withContext(Dispatchers.IO) {
        try {
            val boy = _currentDeliveryBoy.value
            val dbUuid = boy.id
            if (dbUuid.isBlank()) return@withContext

            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_cod_settlements?delivery_boy_id=eq.$dbUuid&order=collected_at.desc&select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    val list = mutableListOf<CodSettlement>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            CodSettlement(
                                id = obj.optString("id"),
                                delivery_boy_id = obj.optString("delivery_boy_id"),
                                order_id = obj.optString("order_id"),
                                order_number = "#ORD-" + obj.optString("order_id").take(6).uppercase(),
                                amount = obj.optDouble("amount", obj.optDouble("amount_collected", 0.0)),
                                status = obj.optString("status", "Collected_By_Rider"),
                                collected_at = formatReadableDate(obj.optString("collected_at", "Today"))
                            )
                        )
                    }
                    if (list.isNotEmpty()) {
                        _codSettlements.value = list
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "fetchCodSettlements error: ${e.message}")
        }
    }

    suspend fun updateGPSLocation(orderId: String, lat: Double, lng: Double, speed: Double = 25.0): Boolean = withContext(Dispatchers.IO) {
        val boy = _currentDeliveryBoy.value
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        try {
            // Update rider table current lat/lng
            if (boy.id.isNotBlank()) {
                val dbBoyBody = JSONObject().apply {
                    put("current_latitude", lat)
                    put("current_longitude", lng)
                }.toString().toRequestBody(jsonMediaType)

                val boyReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_delivery_boys?id=eq.${boy.id}")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .patch(dbBoyBody)
                    .build()

                client.newCall(boyReq).execute().close()
            }

            // Also post to tracking log if active order
            if (orderId.isNotBlank()) {
                val trackBody = JSONObject().apply {
                    put("order_id", orderId)
                    put("delivery_boy_id", boy.id)
                    put("latitude", lat)
                    put("longitude", lng)
                    put("speed_kmh", speed)
                    put("location_name", "En Route")
                    put("recorded_at", nowIso)
                }.toString().toRequestBody(jsonMediaType)

                val trackReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_delivery_gps_logs")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .post(trackBody)
                    .build()

                client.newCall(trackReq).execute().close()
            }
            true
        } catch (e: Exception) {
            Log.w("SupabaseService", "GPS tracking sync error: ${e.message}")
            false
        }
    }

    private fun updateLocalOrderStatus(orderId: String, status: String) {
        val list = _orders.value.map {
            if (it.id == orderId) it.copy(order_status = status) else it
        }
        _orders.value = list
    }

    private fun syncOrderAndAssignmentState(
        orderId: String,
        orderStatus: String,
        assignmentStatus: String,
        reason: String? = null
    ): Boolean {
        try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            // 1. Sync 01_orders
            val orderBody = JSONObject().apply {
                put("order_status", orderStatus)
                if (reason != null) put("cancellation_reason", reason)
            }.toString().toRequestBody(jsonMediaType)

            val orderReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_orders?id=eq.$orderId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .patch(orderBody)
                .build()

            client.newCall(orderReq).execute().close()

            // 2. Sync 01_delivery_assignments
            val assignBody = JSONObject().apply {
                put("status", assignmentStatus)
                if (assignmentStatus == "Accepted") put("accepted_at", nowIso)
                if (reason != null) put("driver_notes", "Rejected: $reason")
            }.toString().toRequestBody(jsonMediaType)

            val assignReq = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_delivery_assignments?order_id=eq.$orderId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .patch(assignBody)
                .build()

            client.newCall(assignReq).execute().close()
            return true
        } catch (e: Exception) {
            Log.w("SupabaseService", "syncOrderAndAssignmentState error: ${e.message}")
            return false
        }
    }

    suspend fun createSupportTicket(subject: String, description: String, priority: String): Boolean = withContext(Dispatchers.IO) {
        val newTicket = SupportTicket(
            id = "t_" + System.currentTimeMillis(),
            ticket_number = "TICK-" + (100..999).random(),
            subject = subject,
            description = description,
            status = "OPEN",
            priority = priority,
            created_at = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        )
        _supportTickets.value = listOf(newTicket) + _supportTickets.value
        true
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(is_read = true) else it
        }
    }

    fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(is_read = true) }
    }
}
