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
    private var isInitialOrderSyncDone = false
    private val lastKnownOrderIds = mutableSetOf<String>()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val savedId = prefs.getString("saved_id", null)
        val savedFullName = prefs.getString("saved_full_name", null)
        val savedEmpCode = prefs.getString("saved_emp_code", null)
        val savedPhone = prefs.getString("saved_phone", null)
        val savedUsername = prefs.getString("saved_username", null)
        val savedVehicle = prefs.getString("saved_vehicle", "Motorcycle") ?: "Motorcycle"
        val savedZone = prefs.getString("saved_zone", "Central Hub") ?: "Central Hub"
        val savedStatus = prefs.getString("saved_status", "Available") ?: "Available"
        val savedRating = prefs.getFloat("saved_rating", 5.0f).toDouble()
        val savedDeliveries = prefs.getInt("saved_deliveries", 0)

        if (!savedId.isNullOrBlank() && !savedFullName.isNullOrBlank()) {
            _currentDeliveryBoy.value = DeliveryBoy(
                id = savedId,
                full_name = savedFullName,
                employee_code = savedEmpCode ?: savedId.take(8),
                phone = savedPhone ?: "",
                app_username = savedUsername ?: "",
                vehicle_info = savedVehicle,
                zone_name = savedZone,
                availability_status = savedStatus,
                is_online = savedStatus != "Offline",
                rating = savedRating,
                total_deliveries = savedDeliveries
            )
            _isAuthenticated.value = true
        } else {
            // Unauthenticated by default - force user login to ensure strict data scoping
            _isAuthenticated.value = false
            _orders.value = emptyList()
        }
    }

    private fun saveSession(boy: DeliveryBoy) {
        prefs.edit()
            .putString("saved_id", boy.id)
            .putString("saved_full_name", boy.full_name)
            .putString("saved_emp_code", boy.employee_code)
            .putString("saved_phone", boy.phone)
            .putString("saved_username", boy.app_username)
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
            // Clear stale orders cache immediately during login/account switch
            _orders.value = emptyList()

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
                    return@withContext Result.failure(Exception("No delivery partner found matching username or ID: $cleanIdent"))
                }

                val obj = array.getJSONObject(0)
                val dbPassword = obj.optString("login_password", "")

                if (dbPassword.isNotBlank() && password.isNotBlank() && dbPassword != password) {
                    return@withContext Result.failure(Exception("Incorrect password entered. Please try again."))
                }

                val boyId = obj.optString("id").ifBlank { obj.optString("delivery_boy_id", UUID.randomUUID().toString()) }

                val boy = DeliveryBoy(
                    id = boyId,
                    full_name = obj.optString("full_name", obj.optString("name", "Delivery Partner")),
                    phone = obj.optString("phone", ""),
                    app_username = obj.optString("app_username", cleanIdent),
                    employee_code = obj.optString("employee_code", boyId.take(8)),
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
        _orders.value = emptyList()
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
        _currentDeliveryBoy.value = DeliveryBoy()
        isInitialOrderSyncDone = false
        lastKnownOrderIds.clear()
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
            if (!_isAuthenticated.value || boy.id.isBlank()) {
                _orders.value = emptyList()
                return@withContext
            }

            val rawOrderJsonMap = mutableMapOf<String, JSONObject>()
            val statusOverrideMap = mutableMapOf<String, String>()

            val driverPhoneDigits = boy.normalizedPhoneDigits

            val searchKeys = listOfNotNull(
                boy.id.takeIf { it.isNotBlank() },
                boy.employee_code.takeIf { it.isNotBlank() && it != boy.id },
                boy.app_username.takeIf { it.isNotBlank() },
                boy.phone.takeIf { it.isNotBlank() },
                driverPhoneDigits.takeIf { it.isNotBlank() && it.length >= 10 }
            )

            // 1. Fetch assignments from 01_delivery_assignments
            for (key in searchKeys) {
                try {
                    val url = "$supabaseUrl/rest/v1/01_delivery_assignments?delivery_boy_id=eq.$key&select=*"
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer $supabaseKey")
                        .get()
                        .build()

                    client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) {
                            val body = res.body?.string() ?: "[]"
                            val arr = JSONArray(body)
                            for (i in 0 until arr.length()) {
                                val assignObj = arr.getJSONObject(i)
                                val ordId = assignObj.optString("order_id").trim()
                                val assignStatus = assignObj.optString("status", assignObj.optString("assignment_status", "Assigned"))
                                if (ordId.isNotBlank()) {
                                    statusOverrideMap[ordId] = assignStatus
                                    if (!rawOrderJsonMap.containsKey(ordId)) {
                                        fetchRawOrderJson(ordId)?.let { rawOrderJsonMap[ordId] = it }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SupabaseService", "Assignment query error for $key: ${e.message}")
                }
            }

            // 2. Query 01_orders directly by driver identifiers
            for (key in searchKeys) {
                try {
                    val queryParam = if (key.length == 10 && key.all { it.isDigit() }) {
                        "assigned_delivery_boy_phone=ilike.*$key*"
                    } else {
                        "assigned_delivery_boy_id=eq.$key"
                    }
                    val url = "$supabaseUrl/rest/v1/01_orders?$queryParam&order=created_at.desc&select=*"
                    val req = Request.Builder()
                        .url(url)
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
                                val rawId = obj.optString("id").ifBlank { obj.optString("order_id") }
                                if (rawId.isNotBlank()) {
                                    rawOrderJsonMap[rawId] = obj
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SupabaseService", "Direct order query error for $key: ${e.message}")
                }
            }

            // 3. Also fetch recent orders to ensure matching across potential column variations
            try {
                val url = "$supabaseUrl/rest/v1/01_orders?order=created_at.desc&limit=50&select=*"
                val req = Request.Builder()
                    .url(url)
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
                            val rawId = obj.optString("id").ifBlank { obj.optString("order_id") }
                            if (rawId.isNotBlank()) {
                                rawOrderJsonMap[rawId] = obj
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "General orders query error: ${e.message}")
            }

            // 4. Resolve full relational entities for customer_id and delivery_address_id
            val resolvedOrders = resolveAndBuildOrders(rawOrderJsonMap.values.toList(), statusOverrideMap)

            // 5. Driver Scoping
            val finalOrders = resolvedOrders
                .filter { isOrderAssignedToDriver(it, boy) }
                .sortedByDescending { it.created_at }

            // Check if a new assigned order arrived to trigger sound alert
            val currentAssignedIds = finalOrders.filter { it.order_status.equals("Assigned", ignoreCase = true) }.map { it.id }.toSet()
            if (isInitialOrderSyncDone) {
                val newlyAssigned = currentAssignedIds.filter { !lastKnownOrderIds.contains(it) }
                if (newlyAssigned.isNotEmpty()) {
                    onNewOrderAssigned?.invoke()
                }
            } else {
                isInitialOrderSyncDone = true
            }
            lastKnownOrderIds.addAll(finalOrders.map { it.id })

            _orders.value = finalOrders
        } catch (e: Exception) {
            Log.e("SupabaseService", "fetchAssignedOrders error: ${e.message}", e)
            _orders.value = emptyList()
        } finally {
            _isSyncing.value = false
        }
    }

    private fun isOrderAssignedToDriver(order: Order, driver: DeliveryBoy): Boolean {
        val driverId = driver.id.trim()
        val driverCode = driver.employee_code.trim()
        val driverUsername = driver.app_username.trim()
        val driverPhoneDigits = driver.normalizedPhoneDigits // Extract last 10 digits

        val orderDriverId = (order.assigned_delivery_boy_id ?: order.delivery_boy_id ?: "").trim()
        val orderDriverPhoneDigits = order.assignedDriverPhoneDigits // Extract last 10 digits

        // Rule A: Match by ID / Employee Code / Username (case-insensitive)
        val matchesId = orderDriverId.isNotBlank() && (
            orderDriverId.equals(driverId, ignoreCase = true) ||
            orderDriverId.equals(driverCode, ignoreCase = true) ||
            orderDriverId.equals(driverUsername, ignoreCase = true)
        )

        // Rule B: Match by 10-digit Phone
        val matchesPhone = driverPhoneDigits.isNotBlank() &&
            driverPhoneDigits.length == 10 &&
            orderDriverPhoneDigits.length == 10 &&
            driverPhoneDigits == orderDriverPhoneDigits

        if (matchesId || matchesPhone) {
            return true
        }

        // Rule C: Hide Other Drivers' Orders -> If explicitly assigned to another non-blank ID that doesn't match this driver, discard immediately
        if (orderDriverId.isNotBlank()) {
            return false
        }

        return false
    }

    private suspend fun fetchRawOrderJson(orderId: String): JSONObject? = withContext(Dispatchers.IO) {
        if (orderId.isBlank()) return@withContext null
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
                    if (arr.length() > 0) return@withContext arr.getJSONObject(0)
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "fetchRawOrderJson error for $orderId: ${e.message}")
        }
        null
    }

    private suspend fun fetchCustomerById(customerId: String): JSONObject? = withContext(Dispatchers.IO) {
        if (customerId.isBlank()) return@withContext null
        val candidates = listOf(
            "$supabaseUrl/rest/v1/01_customers?id=eq.$customerId&select=*",
            "$supabaseUrl/rest/v1/01_customers?customer_id=eq.$customerId&select=*",
            "$supabaseUrl/rest/v1/customers?id=eq.$customerId&select=*"
        )
        for (url in candidates) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: "[]"
                        val arr = JSONArray(body)
                        if (arr.length() > 0) return@withContext arr.getJSONObject(0)
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "fetchCustomerById error for $customerId: ${e.message}")
            }
        }
        null
    }

    private suspend fun fetchAddressById(addressId: String, customerId: String? = null): JSONObject? = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<String>()
        if (addressId.isNotBlank()) {
            candidates.add("$supabaseUrl/rest/v1/01_customer_addresses?id=eq.$addressId&select=*")
            candidates.add("$supabaseUrl/rest/v1/01_customer_addresses?address_id=eq.$addressId&select=*")
            candidates.add("$supabaseUrl/rest/v1/customer_addresses?id=eq.$addressId&select=*")
        }
        if (!customerId.isNullOrBlank()) {
            candidates.add("$supabaseUrl/rest/v1/01_customer_addresses?customer_id=eq.$customerId&order=created_at.desc&limit=1&select=*")
            candidates.add("$supabaseUrl/rest/v1/customer_addresses?customer_id=eq.$customerId&order=created_at.desc&limit=1&select=*")
        }
        for (url in candidates) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()

                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: "[]"
                        val arr = JSONArray(body)
                        if (arr.length() > 0) return@withContext arr.getJSONObject(0)
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "fetchAddressById error for $addressId: ${e.message}")
            }
        }
        null
    }

    private suspend fun fetchOrderItemsForOrder(orderId: String): List<OrderItem> = withContext(Dispatchers.IO) {
        if (orderId.isBlank()) return@withContext emptyList()
        val itemsList = mutableListOf<OrderItem>()
        try {
            val req = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_order_items?order_id=eq.$orderId&select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    for (j in 0 until arr.length()) {
                        val itemObj = arr.getJSONObject(j)
                        itemsList.add(
                            OrderItem(
                                id = itemObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                order_id = orderId,
                                product_name = listOf(
                                    itemObj.optString("product_name"),
                                    itemObj.optString("item_name"),
                                    itemObj.optString("name"),
                                    itemObj.optString("title")
                                ).firstOrNull { it.isNotBlank() } ?: "Item",
                                quantity = itemObj.optInt("quantity", itemObj.optInt("qty", 1)),
                                unit_price = itemObj.optDouble("unit_price", itemObj.optDouble("price", itemObj.optDouble("rate", 0.0))),
                                total_amount = itemObj.optDouble("total_amount", itemObj.optDouble("total_price", itemObj.optDouble("total", 0.0)))
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "fetchOrderItems error for $orderId: ${e.message}")
        }
        itemsList
    }

    private suspend fun resolveAndBuildOrders(
        rawOrders: List<JSONObject>,
        statusOverrideMap: Map<String, String> = emptyMap()
    ): List<Order> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<Order>()
        val customerCache = mutableMapOf<String, JSONObject>()
        val addressCache = mutableMapOf<String, JSONObject>()

        // Collect all distinct IDs needed
        val customerIds = rawOrders.mapNotNull { it.optString("customer_id").trim().takeIf { id -> id.isNotBlank() } }.distinct()
        val addressIds = rawOrders.mapNotNull { it.optString("delivery_address_id").trim().takeIf { id -> id.isNotBlank() } }.distinct()

        // Batch fetch customers if feasible
        if (customerIds.isNotEmpty()) {
            try {
                val filter = "id=in.(${customerIds.joinToString(",")})"
                val req = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_customers?$filter&select=*")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val arr = JSONArray(res.body?.string() ?: "[]")
                        for (i in 0 until arr.length()) {
                            val c = arr.getJSONObject(i)
                            val cId = c.optString("id").trim()
                            if (cId.isNotBlank()) customerCache[cId] = c
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "Batch customer query error: ${e.message}")
            }
        }

        // Batch fetch addresses if feasible
        if (addressIds.isNotEmpty()) {
            try {
                val filter = "id=in.(${addressIds.joinToString(",")})"
                val req = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_customer_addresses?$filter&select=*")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val arr = JSONArray(res.body?.string() ?: "[]")
                        for (i in 0 until arr.length()) {
                            val a = arr.getJSONObject(i)
                            val aId = a.optString("id").trim()
                            if (aId.isNotBlank()) addressCache[aId] = a
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "Batch address query error: ${e.message}")
            }
        }

        for (obj in rawOrders) {
            val rawId = obj.optString("id").ifBlank { obj.optString("order_id", UUID.randomUUID().toString()) }
            val custId = obj.optString("customer_id").trim().takeIf { it.isNotBlank() }
            val addrId = obj.optString("delivery_address_id").trim().takeIf { it.isNotBlank() }

            val customerObj = custId?.let { id -> customerCache[id] ?: fetchCustomerById(id)?.also { customerCache[id] = it } }
            val addressObj = if (!addrId.isNullOrBlank()) {
                addressCache[addrId] ?: fetchAddressById(addrId, custId)?.also { addressCache[addrId] = it }
            } else if (!custId.isNullOrBlank()) {
                fetchAddressById("", custId)
            } else null

            val items = fetchOrderItemsForOrder(rawId)
            val overrideStatus = statusOverrideMap[rawId]

            val order = parseOrderJson(
                obj = obj,
                overrideStatus = overrideStatus,
                customerLookup = customerObj,
                addressLookup = addressObj,
                fetchedItems = items
            )
            resultList.add(order)
        }

        resultList
    }

    suspend fun fetchSingleOrder(orderId: String, statusOverride: String? = null): Order? = withContext(Dispatchers.IO) {
        try {
            val rawObj = fetchRawOrderJson(orderId) ?: return@withContext null
            val custId = rawObj.optString("customer_id").trim().takeIf { it.isNotBlank() }
            val addrId = rawObj.optString("delivery_address_id").trim().takeIf { it.isNotBlank() }

            val customerObj = custId?.let { fetchCustomerById(it) }
            val addressObj = if (!addrId.isNullOrBlank()) {
                fetchAddressById(addrId, custId)
            } else if (!custId.isNullOrBlank()) {
                fetchAddressById("", custId)
            } else null
            val items = fetchOrderItemsForOrder(orderId)

            return@withContext parseOrderJson(
                obj = rawObj,
                overrideStatus = statusOverride,
                customerLookup = customerObj,
                addressLookup = addressObj,
                fetchedItems = items
            )
        } catch (e: Exception) {
            Log.w("SupabaseService", "fetchSingleOrder error: ${e.message}")
        }
        null
    }

    private fun JSONObject?.getCleanString(vararg keys: String): String? {
        if (this == null) return null
        for (key in keys) {
            if (!has(key) || isNull(key)) continue
            val v = optString(key, "").trim()
            if (v.isNotBlank() &&
                !v.equals("null", ignoreCase = true) &&
                !v.equals("undefined", ignoreCase = true) &&
                !v.equals("n/a", ignoreCase = true)
            ) {
                return v
            }
        }
        return null
    }

    private fun sanitizeAddressString(raw: String?): String {
        if (raw.isNullOrBlank() || raw.equals("null", ignoreCase = true) || raw.equals("undefined", ignoreCase = true)) {
            return "Address not found"
        }
        var clean = raw
        clean = clean.replace(Regex("(?i)\\bLandmark:\\s*null\\b,?\\s*"), "")
        clean = clean.replace(Regex("(?i)\\bnull\\b,?\\s*"), "")
        clean = clean.replace(Regex("(?i),?\\s*\\bnull\\b"), "")
        clean = clean.replace(Regex("(?i)\\bundefined\\b,?\\s*"), "")
        clean = clean.replace(Regex(",\\s*,+"), ", ")
        clean = clean.trim().removePrefix(",").removeSuffix(",").trim()
        return if (clean.isBlank()) "Address not found" else clean
    }

    private fun parseOrderJson(
        obj: JSONObject,
        overrideStatus: String? = null,
        customerLookup: JSONObject? = null,
        addressLookup: JSONObject? = null,
        fetchedItems: List<OrderItem> = emptyList()
    ): Order {
        val rawId = obj.getCleanString("id") ?: obj.getCleanString("order_id") ?: UUID.randomUUID().toString()
        val orderNumber = obj.getCleanString("order_number", "order_no", "invoice_no", "display_order_id")
            ?: ("#ORD-" + rawId.take(6).uppercase())

        val customerId = obj.getCleanString("customer_id")
        val deliveryAddressId = obj.getCleanString("delivery_address_id")

        val customerObj = customerLookup
            ?: obj.optJSONObject("customer")
            ?: obj.optJSONObject("01_customers")
            ?: obj.optJSONObject("customer_details")

        val addressObj = addressLookup
            ?: obj.optJSONObject("delivery_address")
            ?: obj.optJSONObject("01_customer_addresses")
            ?: obj.optJSONObject("shipping_address")
            ?: obj.optJSONObject("address")

        // 1. Resolve Customer Name from 01_customers (first_name + last_name, or name/full_name)
        var customerName: String? = null
        if (customerObj != null) {
            val fName = customerObj.getCleanString("first_name") ?: ""
            val lName = customerObj.getCleanString("last_name") ?: ""
            if (fName.isNotBlank() || lName.isNotBlank()) {
                customerName = "$fName $lName".trim()
            } else {
                customerName = customerObj.getCleanString(
                    "name",
                    "full_name",
                    "customer_name",
                    "display_name",
                    "user_name",
                    "username"
                )
            }
        }

        // Fallback to address recipient_name if customer table had no valid name
        if ((customerName.isNullOrBlank() || customerName.equals("Customer", ignoreCase = true)) && addressObj != null) {
            val recipient = addressObj.getCleanString(
                "recipient_name",
                "customer_name",
                "contact_name",
                "name"
            )
            if (!recipient.isNullOrBlank() && !recipient.equals("Customer", ignoreCase = true)) {
                customerName = recipient
            }
        }

        // Fallback to fields on order payload if not resolved from relations
        if (customerName.isNullOrBlank() || customerName.equals("Customer", ignoreCase = true)) {
            val fromOrder = obj.getCleanString(
                "customer_name",
                "customer_full_name",
                "name",
                "full_name",
                "recipient_name",
                "buyer_name"
            )
            if (!fromOrder.isNullOrBlank() && !fromOrder.equals("Customer", ignoreCase = true)) {
                customerName = fromOrder
            }
        }

        // 2. Resolve Customer Phone from 01_customers / 01_customer_addresses
        var customerPhone: String? = null
        if (customerObj != null) {
            customerPhone = customerObj.getCleanString(
                "phone",
                "mobile",
                "phone_number",
                "contact_number",
                "mobile_no",
                "mobile_number"
            )
        }

        if (customerPhone.isNullOrBlank() && addressObj != null) {
            customerPhone = addressObj.getCleanString(
                "phone",
                "mobile",
                "contact_number",
                "phone_number",
                "recipient_phone"
            )
        }

        if (customerPhone.isNullOrBlank()) {
            customerPhone = obj.getCleanString(
                "customer_phone",
                "phone",
                "mobile",
                "contact_number",
                "customer_mobile"
            )
        }

        // 3. Resolve Delivery Address from 01_customer_addresses
        var addressText: String? = null
        if (addressObj != null) {
            val flatAddress = addressObj.getCleanString(
                "address",
                "formatted_address",
                "full_address",
                "street_address"
            )

            if (flatAddress != null) {
                addressText = flatAddress
            } else {
                val line1 = addressObj.getCleanString("address_line_1", "address_line1", "street", "house_flat_no")
                val line2 = addressObj.getCleanString("address_line_2", "address_line2", "area", "locality")
                val landmark = addressObj.getCleanString("landmark")
                val city = addressObj.getCleanString("city", "district")
                val state = addressObj.getCleanString("state", "province")
                val pincode = addressObj.getCleanString("pincode", "pin_code", "zip_code", "postal_code")

                val parts = mutableListOf<String>()
                line1?.let { parts.add(it) }
                line2?.let { if (!it.equals(line1, ignoreCase = true)) parts.add(it) }
                landmark?.let { parts.add("Landmark: $it") }
                city?.let { if (!it.equals(line1, ignoreCase = true) && !it.equals(line2, ignoreCase = true)) parts.add(it) }
                state?.let { parts.add(it) }
                pincode?.let { parts.add(it) }

                if (parts.isNotEmpty()) {
                    addressText = parts.joinToString(", ")
                }
            }
        }

        if (addressText.isNullOrBlank() && customerObj != null) {
            addressText = customerObj.getCleanString("address", "delivery_address", "formatted_address")
        }

        if (addressText.isNullOrBlank()) {
            addressText = obj.getCleanString(
                "delivery_address_text",
                "delivery_address",
                "address",
                "shipping_address",
                "customer_address",
                "drop_address"
            )
        }

        val finalCustomerName = customerName?.takeIf { it.isNotBlank() && !it.equals("Customer", ignoreCase = true) } ?: "Customer not found"
        val finalCustomerPhone = customerPhone?.takeIf { it.isNotBlank() } ?: "Phone not found"
        val finalAddressText = sanitizeAddressString(addressText)

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

        val assignedDbPhone = listOf(
            obj.optString("assigned_delivery_boy_phone"),
            obj.optString("delivery_boy_phone"),
            obj.optString("rider_phone")
        ).firstOrNull { it.isNotBlank() } ?: ""

        // Use fetched items or parse from json array
        val itemsList = mutableListOf<OrderItem>()
        if (fetchedItems.isNotEmpty()) {
            itemsList.addAll(fetchedItems)
        } else {
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
                            ).firstOrNull { it.isNotBlank() } ?: "Item",
                            quantity = itemObj.optInt("quantity", itemObj.optInt("qty", 1)),
                            unit_price = itemObj.optDouble("unit_price", itemObj.optDouble("price", itemObj.optDouble("rate", 0.0))),
                            total_amount = itemObj.optDouble("total_amount", itemObj.optDouble("total_price", itemObj.optDouble("total", 0.0)))
                        )
                    )
                }
            }
        }

        val parsedLat = if (addressObj != null && addressObj.has("latitude") && addressObj.optDouble("latitude", 0.0) != 0.0) {
            addressObj.optDouble("latitude")
        } else if (obj.has("latitude")) obj.optDouble("latitude", 22.5833)
        else if (obj.has("lat")) obj.optDouble("lat", 22.5833)
        else 22.5833

        val parsedLng = if (addressObj != null && addressObj.has("longitude") && addressObj.optDouble("longitude", 0.0) != 0.0) {
            addressObj.optDouble("longitude")
        } else if (obj.has("longitude")) obj.optDouble("longitude", 88.4633)
        else if (obj.has("lng")) obj.optDouble("lng", 88.4633)
        else 88.4633

        val parsedDist = obj.optDouble("distance_km", obj.optDouble("distance", 2.2))

        return Order(
            id = rawId,
            order_number = orderNumber,
            customer_id = customerId,
            delivery_address_id = deliveryAddressId,
            customer_name = finalCustomerName,
            customer_phone = finalCustomerPhone,
            delivery_address_text = finalAddressText,
            total_amount = totalAmount,
            payment_method = paymentMethod,
            payment_status = paymentStatus,
            order_status = effectiveStatus,
            assigned_delivery_boy_id = assignedDbId,
            assigned_delivery_boy_name = assignedDbName,
            assigned_delivery_boy_phone = assignedDbPhone.ifBlank { null },
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
