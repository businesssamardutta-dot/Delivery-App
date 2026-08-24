package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.models.DeliveryBoy
import com.example.data.models.Order
import com.example.ui.components.OnlineOfflineSwitch
import com.example.ui.components.OrderCard
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    deliveryBoy: DeliveryBoy,
    orders: List<Order>,
    unreadNotificationCount: Int,
    onToggleOnline: (Boolean) -> Unit,
    onOrderClick: (Order) -> Unit,
    onNotificationClick: () -> Unit,
    onNavigateToOrdersTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assignedCount = orders.count { it.order_status.equals("Assigned", ignoreCase = true) }
    val deliveredCount = orders.count { it.order_status.equals("Delivered", ignoreCase = true) }
    val inProgressCount = orders.count {
        it.order_status.equals("Accepted", ignoreCase = true) ||
                it.order_status.equals("On The Way", ignoreCase = true) ||
                it.order_status.equals("Reached Customer", ignoreCase = true)
    }

    val activeOrder = orders.firstOrNull {
        it.order_status.equals("On The Way", ignoreCase = true) ||
                it.order_status.equals("Reached Customer", ignoreCase = true) ||
                it.order_status.equals("Accepted", ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Green Brand Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HaribanshoPrimary, HaribanshoDarkGreen)
                    )
                )
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Title & Notification Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Haribansho",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "Delivery Boy App",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        )
                    }

                    // Notification Button
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(containerColor = HaribanshoWarning, contentColor = Color.Black) {
                                    Text(text = "$unreadNotificationCount", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNotificationClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Delivery Boy Profile Card with Online/Offline Switch
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            Surface(
                                shape = CircleShape,
                                color = HaribanshoGreenSurface,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile Photo",
                                        tint = HaribanshoPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (deliveryBoy.name.isNotBlank()) deliveryBoy.name else "Delivery Partner",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = HaribanshoTextPrimary
                                    )
                                )
                                Text(
                                    text = if (deliveryBoy.delivery_boy_id.isNotBlank()) "Delivery Boy ID: ${deliveryBoy.delivery_boy_id}" else "Delivery Partner",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = HaribanshoTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        OnlineOfflineSwitch(
                            isOnline = deliveryBoy.is_online,
                            onToggle = onToggleOnline
                        )
                    }
                }
            }
        }

        // Dashboard Main Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Stats 3 Card Row
            Text(
                text = "Today's Delivery Overview",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = HaribanshoTextPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Assigned",
                    value = String.format("%02d", assignedCount),
                    icon = Icons.Outlined.LocalShipping,
                    iconTint = HaribanshoInfo,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "In Progress",
                    value = String.format("%02d", inProgressCount),
                    icon = Icons.Default.Navigation,
                    iconTint = HaribanshoWarning,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Delivered",
                    value = String.format("%02d", deliveredCount),
                    icon = Icons.Default.CheckCircle,
                    iconTint = HaribanshoSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Go Online / Go Offline Primary Toggle Button
            Button(
                onClick = { onToggleOnline(!deliveryBoy.is_online) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (deliveryBoy.is_online) HaribanshoDarkGreen else HaribanshoPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("toggle_online_button")
            ) {
                Icon(
                    imageVector = if (deliveryBoy.is_online) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (deliveryBoy.is_online) "Go Offline" else "Go Online",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            // Active Delivery Banner or Recent New Orders
            if (activeOrder != null) {
                Text(
                    text = "Active Delivery In Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HaribanshoTextPrimary
                    )
                )
                OrderCard(
                    order = activeOrder,
                    onClick = { onOrderClick(activeOrder) }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Assigned Orders",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HaribanshoTextPrimary
                        )
                    )
                    TextButton(onClick = onNavigateToOrdersTab) {
                        Text("View All ($assignedCount)", color = HaribanshoPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                val newOrders = orders.filter { it.order_status.equals("Assigned", ignoreCase = true) }
                if (newOrders.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HaribanshoSuccess,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No Pending Orders",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "You don't have any new delivery assignments right now.",
                                style = MaterialTheme.typography.bodySmall.copy(color = HaribanshoTextSecondary),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    newOrders.take(2).forEach { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = HaribanshoTextPrimary,
                    fontSize = 24.sp
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = HaribanshoTextSecondary,
                    fontSize = 13.sp
                )
            )
        }
    }
}
