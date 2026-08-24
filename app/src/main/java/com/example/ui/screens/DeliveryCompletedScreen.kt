package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.theme.*

@Composable
fun DeliveryCompletedScreen(
    order: Order,
    collectedAmount: Double,
    onBackToHome: () -> Unit,
    onViewOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Green Checkmark Circle
        Surface(
            shape = CircleShape,
            color = HaribanshoSuccess,
            shadowElevation = 6.dp,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Great Job!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = HaribanshoTextPrimary
            )
        )

        Text(
            text = "Order Delivered Successfully",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = HaribanshoSuccess,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Order Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Order ID", color = HaribanshoTextSecondary, fontSize = 14.sp)
                    Text(order.order_number, fontWeight = FontWeight.Bold, color = HaribanshoPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Customer", color = HaribanshoTextSecondary, fontSize = 14.sp)
                    Text(order.customer_name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Order Amount", color = HaribanshoTextSecondary, fontSize = 14.sp)
                    Text("₹${String.format("%.2f", order.total_amount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payment Mode", color = HaribanshoTextSecondary, fontSize = 14.sp)
                    Text(if (order.payment_mode == "COD") "Cash on Delivery" else "Prepaid Online", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (order.payment_mode == "COD") {
                    Divider(color = Color(0xFFF3F4F6))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Collected Amount", fontWeight = FontWeight.Bold, color = HaribanshoTextPrimary, fontSize = 15.sp)
                        Text("₹${String.format("%.2f", collectedAmount)}", fontWeight = FontWeight.ExtraBold, color = HaribanshoSuccess, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBackToHome,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HaribanshoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("back_to_home_button")
            ) {
                Text("Back to Home Dashboard", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onViewOrders,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("View Orders List", fontWeight = FontWeight.Bold, color = HaribanshoPrimary)
            }
        }
    }
}
