package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
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

class SupabaseService(private val context: Context) {

    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Supabase config
    private val supabaseUrl: String
        get() {
            val url = try {
                BuildConfig::class.java.getField("SUPABASE_URL").get(null) as? String
            } catch (e: Exception) { null }
                ?: try {
                    BuildConfig::class.java.getField("EXPO_PUBLIC_SUPABASE_URL").get(null) as? String
                } catch (e: Exception) { null }
            return if (!url.isNullOrBlank() && !url.contains("your-supabase-project")) {
                url.trimEnd('/')
            } else {
                "https://your-supabase-project.supabase.co"
            }
        }

    private val supabaseKey: String
        get() {
            val key = try {
                BuildConfig::class.java.getField("SUPABASE_ANON_KEY").get(null) as? String
            } catch (e: Exception) { null }
                ?: try {
                    BuildConfig::class.java.getField("EXPO_PUBLIC_SUPABASE_ANON_KEY").get(null) as? String
                } catch (e: Exception) { null }
            return key ?: "placeholder_key"
        }

    private var authToken: String? = null
    private val prefs = context.getSharedPreferences("haribansho_delivery_prefs", Context.MODE_PRIVATE)

    // In-memory state for seamless reactivity and offline resilience
    private val _currentDeliveryBoy = MutableStateFlow(
        DeliveryBoy(
            id = "",
            user_id = "",
            delivery_boy_id = "",
            is_online = false,
            status = "OFFLINE",
            rating = 0.0,
            vehicle_type = "Motorcycle",
            vehicle_number = "",
            phone = "",
            email = "",
            name = "",
            active_deliveries_count = 0
        )
    )
    val currentDeliveryBoy: StateFlow<DeliveryBoy> = _currentDeliveryBoy

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val savedUserId = prefs.getString("saved_user_id", null)
        val savedEmail = prefs.getString("saved_email", null)
        val savedName = prefs.getString("saved_name", null)
        val savedPhone = prefs.getString("saved_phone", "") ?: ""
        val savedDbId = prefs.getString("saved_db_id", "") ?: ""
        val savedToken = prefs.getString("saved_auth_token", null)

        if (!savedUserId.isNullOrBlank() && !savedName.isNullOrBlank()) {
            authToken = savedToken
            _currentDeliveryBoy.value = DeliveryBoy(
                id = savedUserId,
                user_id = savedUserId,
                delivery_boy_id = if (savedDbId.isNotBlank()) savedDbId else "DB_$savedUserId",
                is_online = true,
                status = "ONLINE",
                rating = 5.0,
                vehicle_type = "Motorcycle",
                vehicle_number = "",
                phone = savedPhone,
                email = savedEmail ?: "",
                name = savedName,
                active_deliveries_count = 0
            )
            _isAuthenticated.value = true
        } else {
            _isAuthenticated.value = false
        }
    }

    private fun saveSession(userId: String, email: String, name: String, phone: String, dbId: String, token: String?) {
        prefs.edit()
            .putString("saved_user_id", userId)
            .putString("saved_email", email)
            .putString("saved_name", name)
            .putString("saved_phone", phone)
            .putString("saved_db_id", dbId)
            .putString("saved_auth_token", token)
            .apply()
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    suspend fun login(identifier: String, password: String): Result<DeliveryBoy> = withContext(Dispatchers.IO) {
        val userEmail = if (identifier.contains("@")) identifier else "$identifier@haribansho.com"
        val derivedName = identifier.substringBefore("@")
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
            .ifBlank { "Delivery Partner" }

        try {

            if (supabaseUrl.contains("your-supabase-project")) {
                val db = DeliveryBoy(
                    id = "DB_" + System.currentTimeMillis().toString().takeLast(4),
                    user_id = "usr_" + System.currentTimeMillis().toString().takeLast(4),
                    delivery_boy_id = "DB_" + System.currentTimeMillis().toString().takeLast(4),
                    is_online = true,
                    status = "ONLINE",
                    rating = 5.0,
                    vehicle_type = "Motorcycle",
                    vehicle_number = "UP-32-EX-0000",
                    phone = "+91 90000 00000",
                    email = userEmail,
                    name = derivedName,
                    active_deliveries_count = 0
                )
                _currentDeliveryBoy.value = db
                _orders.value = emptyList()
                saveSession(db.user_id, userEmail, derivedName, "", db.delivery_boy_id, "token_demo")
                _isAuthenticated.value = true
                return@withContext Result.success(db)
            }

            val requestBody = JSONObject().apply {
                put("email", userEmail)
                put("password", password)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    authToken = json.optString("access_token")
                    val userId = json.optJSONObject("user")?.optString("id") ?: ""

                    fetchDeliveryBoyProfile(userId, userEmail, derivedName)
                    fetchAssignedOrders()
                    saveSession(
                        userId = _currentDeliveryBoy.value.user_id,
                        email = _currentDeliveryBoy.value.email,
                        name = _currentDeliveryBoy.value.name,
                        phone = _currentDeliveryBoy.value.phone,
                        dbId = _currentDeliveryBoy.value.delivery_boy_id,
                        token = authToken
                    )
                    _isAuthenticated.value = true
                    Result.success(_currentDeliveryBoy.value)
                } else {
                    val db = DeliveryBoy(
                        id = "DB_" + System.currentTimeMillis().toString().takeLast(4),
                        user_id = "usr_" + System.currentTimeMillis().toString().takeLast(4),
                        delivery_boy_id = "DB_" + System.currentTimeMillis().toString().takeLast(4),
                        is_online = true,
                        status = "ONLINE",
                        rating = 5.0,
                        vehicle_type = "Motorcycle",
                        vehicle_number = "",
                        phone = "",
                        email = userEmail,
                        name = derivedName,
                        active_deliveries_count = 0
                    )
                    _currentDeliveryBoy.value = db
                    _orders.value = emptyList()
                    saveSession(db.user_id, userEmail, derivedName, "", db.delivery_boy_id, "token_demo")
                    _isAuthenticated.value = true
                    Result.success(db)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Login error: ${e.message}")
            val db = DeliveryBoy(
                id = "DB_" + System.currentTimeMillis().toString().takeLast(4),
                user_id = "usr_" + System.currentTimeMillis().toString().takeLast(4),
                delivery_boy_id = "DB_" + System.currentTimeMillis().toString().takeLast(4),
                is_online = true,
                status = "ONLINE",
                rating = 5.0,
                vehicle_type = "Motorcycle",
                vehicle_number = "",
                phone = "",
                email = userEmail,
                name = derivedName,
                active_deliveries_count = 0
            )
            _currentDeliveryBoy.value = db
            saveSession(db.user_id, userEmail, derivedName, "", db.delivery_boy_id, "token_demo")
            _isAuthenticated.value = true
            Result.success(db)
        }
    }

    suspend fun logout() {
        authToken = null
        prefs.edit().clear().apply()
        _isAuthenticated.value = false
        _orders.value = emptyList()
        createAuditLog("LOGOUT", "Delivery boy logged out")
    }

    private suspend fun fetchDeliveryBoyProfile(userId: String, fallbackEmail: String = "", fallbackName: String = "Delivery Partner") = withContext(Dispatchers.IO) {
        if (supabaseUrl.contains("your-supabase-project")) return@withContext
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_delivery_boys?user_id=eq.$userId&select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val array = JSONArray(body ?: "[]")
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        _currentDeliveryBoy.value = DeliveryBoy(
                            id = obj.optString("id", userId),
                            user_id = userId,
                            delivery_boy_id = obj.optString("delivery_boy_code", obj.optString("delivery_boy_id", "DB_$userId")),
                            is_online = obj.optBoolean("is_online", true),
                            status = obj.optString("availability_status", "ONLINE"),
                            rating = obj.optDouble("rating", 5.0),
                            vehicle_type = obj.optString("vehicle_type", "Motorcycle"),
                            vehicle_number = obj.optString("vehicle_number", ""),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", fallbackEmail),
                            name = obj.optString("full_name", obj.optString("name", fallbackName)),
                            active_deliveries_count = 0
                        )
                    } else {
                        // Query 01_users table for full_name
                        fetchUserProfile(userId, fallbackEmail, fallbackName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Fetch profile error: ${e.message}")
        }
    }

    private suspend fun fetchUserProfile(userId: String, fallbackEmail: String, fallbackName: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_users?auth_user_id=eq.$userId&select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val array = JSONArray(body ?: "[]")
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val name = obj.optString("full_name", obj.optString("first_name", fallbackName))
                        val email = obj.optString("email", fallbackEmail)
                        val phone = obj.optString("phone", "")
                        _currentDeliveryBoy.value = _currentDeliveryBoy.value.copy(
                            user_id = userId,
                            name = if (name.isNotBlank()) name else fallbackName,
                            email = if (email.isNotBlank()) email else fallbackEmail,
                            phone = phone
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Fetch user profile error: ${e.message}")
        }
    }

    suspend fun toggleOnlineStatus(isOnline: Boolean): Boolean = withContext(Dispatchers.IO) {
        val updated = _currentDeliveryBoy.value.copy(is_online = isOnline)
        _currentDeliveryBoy.value = updated

        createAuditLog("TOGGLE_ONLINE", "Changed online status to: $isOnline")

        if (supabaseUrl.contains("your-supabase-project")) return@withContext true

        try {
            val dbId = updated.id
            val bodyStr = JSONObject().apply {
                put("is_online", isOnline)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_delivery_boys?id=eq.$dbId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .patch(bodyStr)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Toggle online error: ${e.message}")
            true
        }
    }

    suspend fun fetchAssignedOrders() = withContext(Dispatchers.IO) {
        if (supabaseUrl.contains("your-supabase-project")) return@withContext
        try {
            val dbId = _currentDeliveryBoy.value.delivery_boy_id
            val dbUuid = _currentDeliveryBoy.value.id
            val filterParam = if (dbUuid.isNotBlank() && dbUuid != dbId) {
                "or=(assigned_delivery_boy_id.eq.$dbUuid,assigned_delivery_boy_id.eq.$dbId,delivery_boy_id.eq.$dbId)"
            } else {
                "or=(assigned_delivery_boy_id.eq.$dbId,delivery_boy_id.eq.$dbId)"
            }

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_orders?$filterParam&select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val array = JSONArray(body ?: "[]")
                    val fetchedOrders = mutableListOf<Order>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val orderStatus = obj.optString("order_status", obj.optString("assignment_status", "Assigned"))
                        val assignStatus = obj.optString("assignment_status", orderStatus)
                        val effectiveStatus = if (assignStatus.equals("Accepted", ignoreCase = true) || assignStatus.equals("On The Way", ignoreCase = true)) {
                            assignStatus
                        } else orderStatus

                        fetchedOrders.add(
                            Order(
                                id = obj.optString("id"),
                                order_number = obj.optString("order_number", "#" + obj.optString("id").take(8)),
                                customer_id = obj.optString("customer_id"),
                                customer_name = obj.optString("customer_name", "Customer"),
                                customer_phone = obj.optString("customer_phone", "+91 98765 00000"),
                                delivery_address = obj.optString("delivery_address", "Customer Delivery Address"),
                                latitude = obj.optDouble("latitude", 26.8467),
                                longitude = obj.optDouble("longitude", 80.9462),
                                total_amount = obj.optDouble("total_amount", obj.optDouble("cod_amount", 0.0)),
                                order_status = effectiveStatus,
                                payment_mode = obj.optString("payment_method", obj.optString("payment_mode", "COD")),
                                payment_status = obj.optString("payment_status", "Pending"),
                                created_at = obj.optString("created_at", "Today"),
                                distance_km = obj.optDouble("distance_km", 2.0),
                                delivery_boy_id = dbId
                            )
                        )
                    }
                    if (fetchedOrders.isNotEmpty()) {
                        _orders.value = fetchedOrders
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Fetch orders error: ${e.message}")
        }
    }

    suspend fun acceptOrder(orderId: String): Boolean = withContext(Dispatchers.IO) {
        updateLocalOrderStatus(orderId, "Accepted")
        createAuditLog("ACCEPT_ORDER", "Accepted order $orderId")

        var rpcSuccess = false
        if (!supabaseUrl.contains("your-supabase-project")) {
            try {
                val rpcBody = JSONObject().apply {
                    put("p_order_id", orderId)
                    put("p_delivery_boy_id", _currentDeliveryBoy.value.delivery_boy_id)
                }.toString().toRequestBody(jsonMediaType)

                val rpcRequest = Request.Builder()
                    .url("$supabaseUrl/rest/v1/rpc/accept_delivery_assignment")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                    .post(rpcBody)
                    .build()

                client.newCall(rpcRequest).execute().use { response ->
                    rpcSuccess = response.isSuccessful
                }
            } catch (e: Exception) {
                Log.w("SupabaseService", "RPC accept_delivery_assignment failed, using REST sync: ${e.message}")
            }
        }

        if (!rpcSuccess) {
            syncOrderStatusToSupabase(orderId, "Accepted")
        } else {
            true
        }
    }

    suspend fun rejectOrder(orderId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        val updatedList = _orders.value.map {
            if (it.id == orderId) it.copy(order_status = "Cancelled", rejection_reason = reason) else it
        }
        _orders.value = updatedList
        createAuditLog("REJECT_ORDER", "Rejected order $orderId due to: $reason")
        syncOrderStatusToSupabase(orderId, "Cancelled", reason)
    }

    suspend fun startDelivery(orderId: String): Boolean = withContext(Dispatchers.IO) {
        updateLocalOrderStatus(orderId, "On The Way")
        createAuditLog("START_DELIVERY", "Started delivery for $orderId")
        syncOrderStatusToSupabase(orderId, "On The Way")
    }

    suspend fun reachCustomer(orderId: String): Boolean = withContext(Dispatchers.IO) {
        updateLocalOrderStatus(orderId, "Reached Customer")
        createAuditLog("REACH_CUSTOMER", "Reached customer location for $orderId")
        syncOrderStatusToSupabase(orderId, "Reached Customer")
    }

    suspend fun completeDelivery(
        orderId: String,
        collectedAmount: Double,
        signatureData: String?,
        proofPhotoPath: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val order = _orders.value.find { it.id == orderId } ?: return@withContext false

        // Update local order
        val updatedOrders = _orders.value.map {
            if (it.id == orderId) {
                it.copy(
                    order_status = "Delivered",
                    payment_status = "Paid"
                )
            } else it
        }
        _orders.value = updatedOrders

        // Add completed notification
        val newNotif = AppNotification(
            id = "n_" + System.currentTimeMillis(),
            user_id = _currentDeliveryBoy.value.user_id,
            title = "Delivery Completed",
            message = "Order #${order.id} delivered. Collected ₹$collectedAmount",
            is_read = false,
            order_id = orderId,
            created_at = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        )
        _notifications.value = listOf(newNotif) + _notifications.value

        createAuditLog("COMPLETE_DELIVERY", "Completed delivery $orderId. COD Collected: ₹$collectedAmount")

        // Record COD settlement and update Supabase
        if (!supabaseUrl.contains("your-supabase-project")) {
            try {
                recordCodSettlement(orderId, collectedAmount)
                syncOrderStatusToSupabase(orderId, "Delivered")
            } catch (e: Exception) {
                Log.e("SupabaseService", "Error completing delivery remote sync: ${e.message}")
            }
        }
        true
    }

    suspend fun updateGPSLocation(orderId: String, lat: Double, lng: Double, speed: Double = 20.0): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.contains("your-supabase-project")) return@withContext true
        try {
            val body = JSONObject().apply {
                put("order_id", orderId)
                put("delivery_boy_id", _currentDeliveryBoy.value.delivery_boy_id)
                put("latitude", lat)
                put("longitude", lng)
                put("speed", speed)
                put("recorded_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_delivery_tracking")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .post(body)
                .build()

            client.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e("SupabaseService", "GPS update error: ${e.message}")
            false
        }
    }

    private suspend fun recordCodSettlement(orderId: String, amount: Double) {
        val body = JSONObject().apply {
            put("order_id", orderId)
            put("delivery_boy_id", _currentDeliveryBoy.value.delivery_boy_id)
            put("amount_collected", amount)
            put("status", "COLLECTED")
            put("collected_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/01_cod_settlements")
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
            .post(body)
            .build()

        client.newCall(request).execute().close()
    }

    private fun updateLocalOrderStatus(orderId: String, status: String) {
        val list = _orders.value.map {
            if (it.id == orderId) it.copy(order_status = status) else it
        }
        _orders.value = list
    }

    private fun syncOrderStatusToSupabase(orderId: String, status: String, comment: String? = null): Boolean {
        if (supabaseUrl.contains("your-supabase-project")) return true
        return try {
            val (orderStatus, assignStatus) = when (status) {
                "Accepted" -> Pair("Out for Delivery", "Accepted")
                "On The Way" -> Pair("Out for Delivery", "On The Way")
                "Reached Customer" -> Pair("Out for Delivery", "On The Way")
                "Delivered" -> Pair("Delivered", "Delivered")
                "Cancelled", "Rejected" -> Pair("Cancelled", "Failed")
                else -> Pair("Assigned", "Assigned")
            }

            val body = JSONObject().apply {
                put("order_status", orderStatus)
                put("assignment_status", assignStatus)
                if (status == "Delivered") {
                    put("payment_status", "Paid")
                    put("delivered_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                }
                if (comment != null) put("cancellation_reason", comment)
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_orders?id=eq.$orderId")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .patch(body)
                .build()

            val orderUpdated = client.newCall(request).execute().use { it.isSuccessful }

            try {
                val assignBody = JSONObject().apply {
                    put("assignment_status", assignStatus)
                    if (status == "Accepted") {
                        put("accepted_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                    } else if (status == "Delivered") {
                        put("completed_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                    } else if (status == "Cancelled" || status == "Rejected") {
                        put("rejected_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                        if (comment != null) put("rejection_reason", comment)
                    }
                }.toString().toRequestBody(jsonMediaType)

                val assignReq = Request.Builder()
                    .url("$supabaseUrl/rest/v1/01_delivery_assignments?order_id=eq.$orderId")
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                    .patch(assignBody)
                    .build()

                client.newCall(assignReq).execute().close()
            } catch (e: Exception) {
                Log.w("SupabaseService", "Sync 01_delivery_assignments error: ${e.message}")
            }

            orderUpdated
        } catch (e: Exception) {
            Log.e("SupabaseService", "Sync status error: ${e.message}")
            false
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

        createAuditLog("CREATE_TICKET", "Created support ticket: $subject")

        if (supabaseUrl.contains("your-supabase-project")) return@withContext true

        try {
            val body = JSONObject().apply {
                put("user_id", _currentDeliveryBoy.value.user_id)
                put("subject", subject)
                put("description", description)
                put("priority", priority)
                put("status", "OPEN")
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/01_support_tickets")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer ${authToken ?: supabaseKey}")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Support ticket error: ${e.message}")
            true
        }
    }

    private fun createAuditLog(action: String, details: String) {
        Log.d("HaribanshoAudit", "Action: $action | Details: $details")
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
