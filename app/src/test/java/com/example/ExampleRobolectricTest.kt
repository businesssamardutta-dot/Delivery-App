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
    fun `order calculations and status validation`() {
        val order = Order(
            id = "test-order-1",
            order_number = "#ORD-1002",
            customer_name = "Amit Roy",
            total_amount = 450.0,
            payment_method = "COD",
            order_status = "Assigned"
        )
        assertEquals(450.0, order.total_amount, 0.001)
        assertEquals("COD", order.payment_mode)
        assertNotNull(order.delivery_address)
    }
}
