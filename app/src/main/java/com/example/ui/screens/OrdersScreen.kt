package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.components.OrderCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    orders: List<Order>,
    onAcceptOrder: (String) -> Unit,
    onRejectOrder: (String, String) -> Unit,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Assigned", "In Progress", "Delivered", "All")

    var rejectDialogOrder by remember { mutableStateOf<Order?>(null) }
    var rejectReason by remember { mutableStateOf("Vehicle problem") }

    val filteredOrders = remember(orders, selectedTab) {
        when (selectedTab) {
            0 -> orders.filter { it.order_status.equals("Assigned", ignoreCase = true) }
            1 -> orders.filter {
                it.order_status.equals("Accepted", ignoreCase = true) ||
                        it.order_status.equals("On The Way", ignoreCase = true) ||
                        it.order_status.equals("Reached Customer", ignoreCase = true)
            }
            2 -> orders.filter { it.order_status.equals("Delivered", ignoreCase = true) }
            else -> orders
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "New & Assigned Orders",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HaribanshoPrimary)
        )

        // Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = HaribanshoPrimary,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Orders List
        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = HaribanshoTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No Orders Found",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HaribanshoTextPrimary
                        )
                    )
                    Text(
                        text = "There are no orders matching this status right now.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HaribanshoTextSecondary
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onAccept = { onAcceptOrder(order.id) },
                        onReject = { rejectDialogOrder = order },
                        onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }

    // Reject Reason Modal
    if (rejectDialogOrder != null) {
        val targetOrder = rejectDialogOrder!!
        val rejectionReasons = listOf(
            "Vehicle problem",
            "Unable to reach location",
            "Already handling another delivery",
            "Emergency",
            "Other"
        )

        AlertDialog(
            onDismissRequest = { rejectDialogOrder = null },
            title = {
                Text(
                    text = "Reject Assignment ${targetOrder.order_number}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please select a reason for rejecting this assignment:", fontSize = 13.sp, color = HaribanshoTextSecondary)

                    rejectionReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = rejectReason == reason,
                                onClick = { rejectReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectOrder(targetOrder.id, rejectReason)
                        rejectDialogOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaribanshoDanger)
                ) {
                    Text("Confirm Rejection", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rejectDialogOrder = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
