package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String = "",
    val email: String? = null,
    val phone: String? = null,
    val name: String? = null,
    val avatar_url: String? = null
)

@JsonClass(generateAdapter = true)
data class DeliveryBoy(
    val id: String = "DB1250",
    val user_id: String = "user_123",
    val delivery_boy_id: String = "DB1250",
    val is_online: Boolean = true,
    val status: String = "ACTIVE",
    val rating: Double = 4.8,
    val vehicle_type: String = "Motor Scooter",
    val vehicle_number: String = "UP-32-AB-1234",
    val phone: String = "+91 98765 43210",
    val email: String = "delivery.ravi@haribansho.com",
    val name: String = "Ravi Kumar",
    val active_deliveries_count: Int = 1
)

@JsonClass(generateAdapter = true)
data class CustomerAddress(
    val id: String = "",
    val customer_id: String = "",
    val full_address: String = "Near City Mall, Hazratganj, Lucknow, UP 226001",
    val landmark: String = "Near City Mall",
    val city: String = "Lucknow",
    val state: String = "UP",
    val pincode: String = "226001",
    val latitude: Double = 26.8467,
    val longitude: Double = 80.9462
)

@JsonClass(generateAdapter = true)
data class Customer(
    val id: String = "",
    val name: String = "Rahul Sharma",
    val phone: String = "+91 98765 12345",
    val email: String = "rahul.sharma@example.com",
    val address: CustomerAddress = CustomerAddress()
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: String = "",
    val order_id: String = "",
    val product_name: String = "Item",
    val quantity: Int = 1,
    val unit_price: Double = 0.0,
    val total_price: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class Order(
    val id: String = "ORD1250",
    val order_number: String = "#ORD1250",
    val customer_id: String = "cust_1",
    val customer_name: String = "Rahul Sharma",
    val customer_phone: String = "+91 98765 12345",
    val delivery_address: String = "Near City Mall, Hazratganj, Lucknow, UP 226001",
    val latitude: Double = 26.8467,
    val longitude: Double = 80.9462,
    val total_amount: Double = 685.00,
    val order_status: String = "Assigned", // Assigned, Accepted, Picked Up, On The Way, Reached, Delivered, Cancelled
    val payment_mode: String = "COD", // COD or PREPAID
    val payment_status: String = "Pending", // Pending or Paid
    val created_at: String = "10:30 AM | 17 May 2025",
    val distance_km: Double = 2.4,
    val items: List<OrderItem> = emptyList(),
    val rejection_reason: String? = null,
    val delivery_boy_id: String? = "DB1250"
)

@JsonClass(generateAdapter = true)
data class DeliveryAssignment(
    val id: String = "",
    val order_id: String = "",
    val delivery_boy_id: String = "",
    val assignment_status: String = "Assigned", // Assigned, Accepted, Rejected, Completed
    val assigned_at: String = "",
    val accepted_at: String? = null,
    val completed_at: String? = null,
    val rejection_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class DeliveryTracking(
    val id: String = "",
    val order_id: String = "",
    val delivery_boy_id: String = "",
    val latitude: Double = 26.8467,
    val longitude: Double = 80.9462,
    val speed: Double = 25.0,
    val accuracy: Double = 5.0,
    val heading: Double = 90.0,
    val recorded_at: String = ""
)

@JsonClass(generateAdapter = true)
data class CodSettlement(
    val id: String = "",
    val order_id: String = "",
    val delivery_boy_id: String = "",
    val amount_required: Double = 0.0,
    val amount_collected: Double = 0.0,
    val status: String = "SETTLED",
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

data class EarningsSummary(
    val today_earnings: Double = 1250.00,
    val deliveries_count: Int = 5,
    val order_earnings: Double = 1000.00,
    val tips: Double = 150.00,
    val incentives: Double = 100.00,
    val adjustments: Double = 0.00,
    val net_total: Double = 1250.00
)

data class RecentTransaction(
    val id: String,
    val order_number: String,
    val amount: Double,
    val status: String,
    val timestamp: String,
    val customer_name: String
)
