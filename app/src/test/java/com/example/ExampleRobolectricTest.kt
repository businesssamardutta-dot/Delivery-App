package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.DeliveryBoy
import com.example.data.models.Order
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Haribansho Delivery", appName)
    }

    @Test
    fun `delivery boy default data model properties`() {
        val boy = DeliveryBoy(
            full_name = "Prosun Majhi",
            employee_code = "DB-8062",
            phone = "+91 98765 43210"
        )
        assertEquals("Prosun Majhi", boy.name)
        assertEquals("DB-8062", boy.delivery_boy_id)
        assertEquals("Available", boy.availability_status)
    }

    @Test
    fun `phone normalization extracts last 10 digits`() {
        val boy1 = DeliveryBoy(phone = "+91 89109 61660")
        val boy2 = DeliveryBoy(phone = "08910961660")
        assertEquals("8910961660", boy1.normalizedPhoneDigits)
        assertEquals("8910961660", boy2.normalizedPhoneDigits)
    }

    @Test
    fun `test driver order isolation by id and phone`() {
        val driverA = DeliveryBoy(id = "uuid-driver-a", employee_code = "DB-1001", phone = "+91 8910961660", full_name = "Driver A")
        val driverB = DeliveryBoy(id = "uuid-driver-b", employee_code = "DB-1002", phone = "+91 7278356446", full_name = "Driver B")

        val orderForA1 = Order(id = "ord-1", assigned_delivery_boy_id = "uuid-driver-a")
        val orderForA2 = Order(id = "ord-2", assigned_delivery_boy_phone = "+91 8910961660")
        val orderForB = Order(id = "ord-3", assigned_delivery_boy_id = "uuid-driver-b", assigned_delivery_boy_phone = "7278356446")

        fun isOrderAssigned(order: Order, driver: DeliveryBoy): Boolean {
            val dId = driver.id.trim()
            val dCode = driver.employee_code.trim()
            val dPhone = driver.normalizedPhoneDigits

            val oId = (order.assigned_delivery_boy_id ?: "").trim()
            val oPhone = order.assignedDriverPhoneDigits

            val matchesId = oId.isNotBlank() && (oId.equals(dId, true) || oId.equals(dCode, true))
            val matchesPhone = dPhone.length == 10 && oPhone.length == 10 && dPhone == oPhone

            if (matchesId || matchesPhone) return true
            if (oId.isNotBlank()) return false
            return false
        }

        assertEquals(true, isOrderAssigned(orderForA1, driverA))
        assertEquals(true, isOrderAssigned(orderForA2, driverA))
        assertEquals(false, isOrderAssigned(orderForB, driverA))

        assertEquals(false, isOrderAssigned(orderForA1, driverB))
        assertEquals(false, isOrderAssigned(orderForA2, driverB))
        assertEquals(true, isOrderAssigned(orderForB, driverB))
    }

    @Test
    fun `test customer name rewrite when customer is driver`() {
        val driver = DeliveryBoy(full_name = "Prosun Majhi")
        val testOrderSelf = Order(customer_name = "Prosun Majhi")
        val testOrderOther = Order(customer_name = "Samar Dutta")

        assertEquals("Customer", testOrderSelf.getDisplayCustomerName(driver.full_name))
        assertEquals("Samar Dutta", testOrderOther.getDisplayCustomerName(driver.full_name))
    }
}
