package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.components.DeliveryTimeline
import com.example.ui.components.PaymentBadge
import com.example.ui.components.SignatureCanvas
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: Order,
    onBack: () -> Unit,
    onAcceptOrder: (String) -> Unit,
    onStartDelivery: (String) -> Unit,
    onCompleteDelivery: (orderId: String, amount: Double, notes: String?) -> Unit,
    onViewOnMap: (Order) -> Unit,
    modifier: Modifier = Modifier,
    driverName: String = ""
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val detailTabs = listOf("Delivery Details", "Package Manifest", "Proof of Delivery (POD)")

    // POD State
    var isCodConfirmed by remember { mutableStateOf(false) }
    var capturedSignature by remember { mutableStateOf<Bitmap?>(null) }
    var isPhotoAttached by remember { mutableStateOf(false) }
    var deliveryNotes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isCod = order.payment_method.equals("COD", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Order #${order.order_number}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Status: ${order.order_status}",
                        style = MaterialTheme.typography.bodySmall.copy(color = EmeraldLight)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = { onViewOnMap(order) }) {
                    Icon(imageVector = Icons.Outlined.Navigation, contentDescription = "Map HUD", tint = EmeraldLight)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        // Segmented Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = EmeraldPrimary,
            divider = { Divider(color = DarkBorder) }
        ) {
            detailTabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tabTitle,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Tab Contents
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 1: DELIVERY DETAILS
                    // 4-Step State Machine Stepper
                    DeliveryTimeline(currentStatus = order.order_status)

                    // Customer Contact Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Customer Information",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldSurface,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = EmeraldLight
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = order.getDisplayCustomerName(driverName.ifBlank { order.assigned_delivery_boy_name ?: "" }),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = order.customer_phone,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }
                                }

                                // Quick Action Buttons (Call + WhatsApp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Direct Phone Call
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customer_phone}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceElevated)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Call,
                                            contentDescription = "Call",
                                            tint = EmeraldLight
                                        )
                                    }

                                    // WhatsApp Chat Trigger
                                    IconButton(
                                        onClick = {
                                            val rawPhone = order.customer_phone.replace("+", "").replace(" ", "")
                                            val waUrl = "https://wa.me/$rawPhone?text=Hello%20${order.customer_name},%20I%20am%20your%20Haribansho%20delivery%20partner%20with%20order%20${order.order_number}."
                                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                            context.startActivity(waIntent)
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceElevated)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Chat,
                                            contentDescription = "WhatsApp",
                                            tint = EmeraldPrimary
                                        )
                                    }
                                }
                            }

                            Divider(color = DarkBorder)

                            // Formatted Address & Maps Trigger
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Place,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = order.delivery_address,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val osmUrl = "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=22.572645,88.363892;${order.latitude},${order.longitude}"
                                        val osmIntent = Intent(Intent.ACTION_VIEW, Uri.parse(osmUrl))
                                        context.startActivity(osmIntent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color(0xFF020617)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open in OpenStreetMap Navigation ➔", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // Payment Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Payment Summary",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                PaymentBadge(
                                    method = order.payment_method,
                                    isPaid = order.payment_status.equals("Paid", ignoreCase = true)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isCod) "Cash to Collect from Customer:" else "Bill Amount (Prepaid):",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", order.total_amount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCod) AmberAlert else EmeraldLight
                                    )
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: PACKAGE MANIFEST
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Itemized Order Checklist",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )

                            val manifestItems = order.items

                            if (manifestItems.isEmpty()) {
                                Text(
                                    text = "Order manifest with 1 package (₹${String.format(Locale.getDefault(), "%.2f", order.total_amount)})",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            } else {
                                manifestItems.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldSurface,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldLight
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = item.product_name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            Text(
                                                text = "Qty: ${item.quantity} × ₹${String.format("%.2f", item.unit_price)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "₹${String.format("%.2f", item.total_price)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }
                                if (idx < manifestItems.size - 1) {
                                    HorizontalDivider(color = LightBorder)
                                }
                            }
                        }

                        HorizontalDivider(color = LightBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Items: ${manifestItems.sumOf { it.quantity }}",
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Total: ₹${String.format("%.2f", order.total_amount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldLight
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: PROOF OF DELIVERY (POD)
                    if (validationError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                color = RedDanger,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // 1. COD Verification Checkbox
                    if (isCod) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmberSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberAlert),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isCodConfirmed,
                                    onCheckedChange = {
                                        isCodConfirmed = it
                                        validationError = null
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AmberAlert,
                                        checkmarkColor = Color(0xFF020617)
                                    )
                                )
                                Text(
                                    text = "I have collected ₹${String.format("%.2f", order.total_amount)} in physical cash from customer.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    }

                    // 2. Digital Signature Pad
                    SignatureCanvas(
                        onSignatureCaptured = {
                            capturedSignature = it
                            validationError = null
                        }
                    )

                    // 3. Camera / Doorstep Photo Proof
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Doorstep Photo Proof",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = if (isPhotoAttached) "Doorstep Photo Attached ✓" else "Optional photo confirmation",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isPhotoAttached) EmeraldPrimary else TextSecondary
                                    )
                                )
                            }

                            Button(
                                onClick = { isPhotoAttached = !isPhotoAttached },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPhotoAttached) EmeraldPrimary else Color(0xFFF1F5F9),
                                    contentColor = if (isPhotoAttached) Color.White else TextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPhotoAttached) "Attached" else "Capture")
                            }
                        }
                    }

                    // 4. Delivery Notes
                    OutlinedTextField(
                        value = deliveryNotes,
                        onValueChange = { deliveryNotes = it },
                        label = { Text("Driver Delivery Notes (Optional)") },
                        placeholder = { Text("e.g. Handed to security / Received by customer") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = LightBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom Fixed State-Transition Action Button (Large 52dp Touch Target)
        Surface(
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                when (order.order_status.lowercase().trim()) {
                    "assigned", "pending" -> {
                        Button(
                            onClick = { onAcceptOrder(order.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF020617)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("accept_order_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept Order Assignment", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    "accepted" -> {
                        Button(
                            onClick = { onStartDelivery(order.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF020617)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_trip_btn")
                        ) {
                            Icon(imageVector = Icons.Default.DirectionsBike, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Picked Up • Start Delivery Trip", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    "out for delivery", "on the way", "started", "reached" -> {
                        Button(
                            onClick = {
                                if (isCod && !isCodConfirmed && selectedTab != 2) {
                                    selectedTab = 2 // Switch to POD to confirm COD cash
                                } else if (isCod && !isCodConfirmed) {
                                    validationError = "Please check the box confirming you collected ₹${String.format(Locale.getDefault(), "%.2f", order.total_amount)} in cash."
                                } else {
                                    val amt = if (isCod) order.total_amount else 0.0
                                    onCompleteDelivery(order.id, amt, deliveryNotes.ifBlank { null })
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color(0xFF020617)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("complete_delivery_btn")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "✓ Mark as Delivered",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    "delivered", "completed" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Order Successfully Delivered & Settled", color = EmeraldLight, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
