package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
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
    onStartTrip: (String) -> Unit,
    onRejectOrder: (String, String) -> Unit,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Assigned", "Accepted", "In Progress", "Delivered")

    var rejectDialogOrder by remember { mutableStateOf<Order?>(null) }
    var rejectReason by remember { mutableStateOf("Bike breakdown / mechanical problem") }

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { order ->
            // Filter chip matching
            val matchesFilter = when (selectedFilter) {
                "Assigned" -> order.order_status.equals("Assigned", ignoreCase = true)
                "Accepted" -> order.order_status.equals("Accepted", ignoreCase = true)
                "In Progress" -> order.order_status.equals("Out for Delivery", ignoreCase = true) ||
                        order.order_status.equals("On The Way", ignoreCase = true) ||
                        order.order_status.equals("Accepted", ignoreCase = true)
                "Delivered" -> order.order_status.equals("Delivered", ignoreCase = true)
                else -> true
            }

            // Search text matching (Order #, Name, Phone, Address)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = if (q.isBlank()) true else {
                order.order_number.lowercase().contains(q) ||
                        order.customer_name.lowercase().contains(q) ||
                        order.customer_phone.lowercase().contains(q) ||
                        order.delivery_address.lowercase().contains(q)
            }

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Orders & Trip Dispatch",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        // Search Bar Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Order #, Customer, Phone, or Street...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = EmeraldLight)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("orders_search_input")
            )
        }

        // Horizontal Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filterName ->
                val isSelected = selectedFilter == filterName
                val count = when (filterName) {
                    "Assigned" -> orders.count { it.order_status.equals("Assigned", ignoreCase = true) }
                    "Accepted" -> orders.count { it.order_status.equals("Accepted", ignoreCase = true) }
                    "In Progress" -> orders.count {
                        it.order_status.equals("Out for Delivery", ignoreCase = true) ||
                                it.order_status.equals("On The Way", ignoreCase = true) ||
                                it.order_status.equals("Accepted", ignoreCase = true)
                    }
                    "Delivered" -> orders.count { it.order_status.equals("Delivered", ignoreCase = true) }
                    else -> orders.size
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filterName },
                    label = {
                        Text(
                            text = "$filterName ($count)",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color(0xFF020617),
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DarkBorder,
                        selectedBorderColor = EmeraldLight
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(36.dp)
                )
            }
        }

        Divider(color = DarkBorder, modifier = Modifier.padding(top = 8.dp))

        // Orders List or Empty State
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
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No Orders Match Filter",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try changing your search keywords." else "No deliveries found under '$selectedFilter'.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
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
                        onStartTrip = { onStartTrip(order.id) },
                        onReject = { rejectDialogOrder = order },
                        onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }

    // Reject Reason Modal Dialog
    if (rejectDialogOrder != null) {
        val targetOrder = rejectDialogOrder!!
        val rejectionReasons = listOf(
            "Bike breakdown / mechanical problem",
            "Customer address out of reachable radius",
            "Currently handling active delivery",
            "Emergency situation / Shift ending"
        )

        AlertDialog(
            onDismissRequest = { rejectDialogOrder = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Reject Delivery #${targetOrder.order_number}",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select a reason for reassigning this order back to the Hub dispatch queue:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    rejectionReasons.forEach { reasonText ->
                        Surface(
                            onClick = { rejectReason = reasonText },
                            shape = RoundedCornerShape(8.dp),
                            color = if (rejectReason == reasonText) RedSurface else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (rejectReason == reasonText) RedDanger else DarkBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = rejectReason == reasonText,
                                    onClick = { rejectReason = reasonText },
                                    colors = RadioButtonDefaults.colors(selectedColor = RedDanger)
                                )
                                Text(
                                    text = reasonText,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = if (rejectReason == reasonText) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ordId = targetOrder.id
                        rejectDialogOrder = null
                        onRejectOrder(ordId, rejectReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Confirm Rejection", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogOrder = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
