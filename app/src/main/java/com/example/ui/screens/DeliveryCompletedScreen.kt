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
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large Green Checkmark Circle
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary,
            shadowElevation = 8.dp,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF020617),
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Trip Completed!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        )

        Text(
            text = "Order Delivered & Settled Successfully",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = EmeraldLight,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Order Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
                    Text("Order Number", color = TextSecondary, fontSize = 14.sp)
                    Text(order.order_number, fontWeight = FontWeight.Bold, color = EmeraldLight, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Customer", color = TextSecondary, fontSize = 14.sp)
                    Text(order.customer_name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Order Total", color = TextSecondary, fontSize = 14.sp)
                    Text("₹${String.format("%.2f", order.total_amount)}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payment Mode", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        if (order.payment_method == "COD") "Cash on Delivery" else "Prepaid Online",
                        fontWeight = FontWeight.Bold,
                        color = if (order.payment_method == "COD") AmberAlert else EmeraldLight,
                        fontSize = 14.sp
                    )
                }

                if (order.payment_method == "COD" && collectedAmount > 0) {
                    Divider(color = DarkBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("COD Cash in Hand", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Text("₹${String.format("%.2f", collectedAmount)}", fontWeight = FontWeight.ExtraBold, color = AmberAlert, fontSize = 16.sp)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color(0xFF020617)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("back_to_home_button")
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = onViewOrders,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("View All Orders", fontWeight = FontWeight.Bold)
            }
        }
    }
}
