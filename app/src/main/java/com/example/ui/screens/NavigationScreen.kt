package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.components.DeliveryTimeline
import com.example.ui.components.InteractiveMapCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    order: Order,
    onBack: () -> Unit,
    onReachedCustomer: (String) -> Unit,
    onMarkDelivered: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Navigation & Delivery Progress",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HaribanshoPrimary)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Map Component
            InteractiveMapCard(
                latitude = order.latitude,
                longitude = order.longitude,
                address = order.delivery_address,
                distanceKm = order.distance_km
            )

            // Delivery Timeline Stepper
            DeliveryTimeline(currentStatus = order.order_status)

            // Customer Contact & Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customer Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HaribanshoTextPrimary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(order.customer_name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(order.customer_phone, style = MaterialTheme.typography.bodySmall.copy(color = HaribanshoTextSecondary))
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customer_phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .background(HaribanshoGreenSurface, RoundedCornerShape(10.dp))
                        ) {
                            Icon(imageVector = Icons.Outlined.Call, contentDescription = "Call Customer", tint = HaribanshoPrimary)
                        }
                    }

                    Text(
                        text = "Address: ${order.delivery_address}",
                        style = MaterialTheme.typography.bodySmall.copy(color = HaribanshoTextSecondary)
                    )
                }
            }

            // Status Actions
            when (order.order_status.lowercase()) {
                "on the way", "accepted", "assigned", "picked up" -> {
                    Button(
                        onClick = { onReachedCustomer(order.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaribanshoWarning),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("reached_customer_button")
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reached Customer Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                "reached customer", "reached" -> {
                    Button(
                        onClick = { onMarkDelivered(order) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaribanshoSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("mark_delivered_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Delivered & Collect Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                "delivered" -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HaribanshoSuccess.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = HaribanshoSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Order Already Delivered Successfully", color = HaribanshoSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
