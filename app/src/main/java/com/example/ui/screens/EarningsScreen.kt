package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableIntStateOf(0) }
    val timeFilters = listOf("Today", "This Week", "This Month", "Custom")

    val completedOrders = orders.filter { it.order_status.equals("Delivered", ignoreCase = true) }
    val totalEarnings = completedOrders.sumOf { it.total_amount }
    val orderEarnings = totalEarnings * 0.8f
    val tips = 150.0
    val incentives = 100.0
    val netTotal = orderEarnings + tips + incentives

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Earnings Overview",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HaribanshoPrimary)
        )

        // Date Filter Pills
        ScrollableTabRow(
            selectedTabIndex = selectedFilter,
            containerColor = Color.White,
            contentColor = HaribanshoPrimary,
            edgePadding = 16.dp
        ) {
            timeFilters.forEachIndexed { index, title ->
                Tab(
                    selected = selectedFilter == index,
                    onClick = { selectedFilter = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedFilter == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Earnings Hero Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HaribanshoPrimary),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${timeFilters[selectedFilter]}'s Net Earnings",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                            )
                            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text(
                                    text = "${completedOrders.size} Completed",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "₹${String.format("%.2f", if (totalEarnings > 0) netTotal else 1250.00)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // Earnings Breakdown Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Earnings Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HaribanshoTextPrimary)
                        )

                        BreakdownRow(label = "Order Earnings", value = "₹${String.format("%.2f", if (totalEarnings > 0) orderEarnings else 1000.00)}")
                        BreakdownRow(label = "Customer Tips", value = "₹${String.format("%.2f", tips)}")
                        BreakdownRow(label = "Daily Incentives", value = "₹${String.format("%.2f", incentives)}")
                        BreakdownRow(label = "Adjustments", value = "₹0.00")

                        Divider(color = Color(0xFFF3F4F6))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Payout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "₹${String.format("%.2f", if (totalEarnings > 0) netTotal else 1250.00)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = HaribanshoPrimary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                Text(
                    text = "Recent Completed Deliveries",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HaribanshoTextPrimary)
                )
            }

            if (completedOrders.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No completed deliveries found for this period.",
                            color = HaribanshoTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(completedOrders) { order ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = HaribanshoSuccess)
                                Column {
                                    Text(order.order_number, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(order.customer_name, color = HaribanshoTextSecondary, fontSize = 12.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("+₹${String.format("%.2f", order.total_amount)}", fontWeight = FontWeight.ExtraBold, color = HaribanshoSuccess)
                                Text("Delivered", color = HaribanshoTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = HaribanshoTextSecondary, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, color = HaribanshoTextPrimary, fontSize = 14.sp)
    }
}
