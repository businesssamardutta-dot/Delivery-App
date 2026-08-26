package com.example.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeliveryBoy(
    val id: String = "",
    val user_id: String = "",
    val full_name: String = "Delivery Partner",
    val phone: String = "",
    val app_username: String = "",
    val employee_code: String = "DB-8062",
    val vehicle_info: String = "Motorcycle",
    val license_number: String = "",
    val zone_name: String = "Central Hub",
    val availability_status: String = "Available", // 'Available' | 'Busy' | 'Offline'
    val is_online: Boolean = true,
    val rating: Double = 5.0,
    val total_deliveries: Int = 0,
    val current_latitude: Double = 22.5726,
    val current_longitude: Double = 88.3639,
    val active_deliveries_count: Int = 0
) {
    // Helper accessor for backward-compat
    val name: String get() = full_name.ifBlank { "Delivery Partner" }
    val delivery_boy_id: String get() = employee_code.ifBlank { id }
    val vehicle_type: String get() = vehicle_info.ifBlank { "Motorcycle" }
    val vehicle_number: String get() = vehicle_info
    val email: String get() = ""
    val status: String get() = availability_status
    val normalizedPhoneDigits: String
        get() = phone.replace(Regex("\\D"), "").takeLast(10)
}

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: String = "",
    val order_id: String = "",
    val product_name: String = "Item",
    val quantity: Int = 1,
    val unit_price: Double = 0.0,
    val total_amount: Double = 0.0
) {
    val total_price: Double get() = if (total_amount > 0.0) total_amount else unit_price * quantity
}

@JsonClass(generateAdapter = true)
data class Order(
    val id: String = "",
    val order_number: String = "",
    val customer_name: String = "Customer",
    val customer_phone: String = "+91 98765 00000",
    val delivery_address_text: String = "Customer Delivery Address",
    val total_amount: Double = 0.0,
    val payment_method: String = "COD", // 'COD' | 'Prepaid' | 'UPI'
    val payment_status: String = "Pending", // 'Pending' | 'Paid' | 'Failed'
    val order_status: String = "Assigned", // 'Pending' | 'Assigned' | 'Accepted' | 'Out for Delivery' | 'Delivered' | 'Cancelled'
    val assignment_status: String = "Assigned", // 'Unassigned' | 'Assigned' | 'Accepted' | 'Rejected'
    val assigned_delivery_boy_id: String? = null,
    val assigned_delivery_boy_name: String? = null,
    val assigned_delivery_boy_phone: String? = null,
    val created_at: String = "Today",
    val latitude: Double = 22.5726,
    val longitude: Double = 88.3639,
    val distance_km: Double = 2.4,
    val items: List<OrderItem> = emptyList(),
    val rejection_reason: String? = null,
    val notes: String? = null
) {
    val delivery_address: String get() = delivery_address_text.ifBlank { "Delivery Address" }
    val payment_mode: String get() = payment_method
    val payment_type: String get() = payment_method
    val delivery_boy_id: String? get() = assigned_delivery_boy_id
    val assignedDriverPhoneDigits: String
        get() = (assigned_delivery_boy_phone ?: "").replace(Regex("\\D"), "").takeLast(10)

    fun getDisplayCustomerName(driverName: String = ""): String {
        val cleanCust = customer_name.trim()
        val cleanDriver = driverName.trim()
        if (cleanCust.isNotBlank() && cleanDriver.isNotBlank() && cleanCust.equals(cleanDriver, ignoreCase = true)) {
            return "Customer"
        }
        return customer_name
    }
}

@JsonClass(generateAdapter = true)
data class DeliveryAssignment(
    val id: String = "",
    val order_id: String = "",
    val delivery_boy_id: String = "",
    val status: String = "Assigned", // 'Assigned' | 'Accepted' | 'Started' | 'Delivered' | 'Rejected'
    val assigned_at: String? = null,
    val accepted_at: String? = null,
    val delivered_at: String? = null,
    val signature_url: String? = null,
    val photo_proof_url: String? = null,
    val cod_collected_amount: Double = 0.0,
    val driver_notes: String? = null
) {
    val assignment_status: String get() = status
}

@JsonClass(generateAdapter = true)
data class CodSettlement(
    val id: String = "",
    val delivery_boy_id: String = "",
    val order_id: String = "",
    val order_number: String = "",
    val amount: Double = 0.0,
    val status: String = "Collected_By_Rider", // 'Collected_By_Rider' | 'Deposited_To_Hub' | 'Verified'
    val collected_at: String = ""
)

@JsonClass(generateAdapter = true)
data class AppNotification(
    val id: String = "",
    val user_id: String = "",
    val title: String = "",
    val message: String = "",
    val is_read: Boolean = false,
    val order_id: String? = null,
    val created_at: String = ""
)

@JsonClass(generateAdapter = true)
data class SupportTicket(
    val id: String = "",
    val ticket_number: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "OPEN",
    val priority: String = "NORMAL",
    val created_at: String = ""
)
