package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Order
import com.example.ui.theme.*

@Composable
fun OnlineOfflineSwitch(
    isOnline: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
        label = "switchBg"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isOnline) HaribanshoSuccess else Color(0xFF9CA3AF),
        label = "switchDot"
    )

    Surface(
        onClick = { onToggle(!isOnline) },
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOnline) HaribanshoSuccess.copy(alpha = 0.3f) else Color.Transparent),
        modifier = modifier.testTag("online_offline_switch")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isOnline) HaribanshoDarkGreen else HaribanshoTextSecondary
                )
            )
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "assigned", "new" -> Color(0xFFEFF6FF) to HaribanshoInfo
        "accepted", "in progress", "on the way" -> Color(0xFFE8F5E9) to HaribanshoDarkGreen
        "reached customer", "reached" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        "delivered", "completed" -> Color(0xFFDCFCE7) to HaribanshoSuccess
        "cancelled", "rejected" -> Color(0xFFFEE2E2) to HaribanshoDanger
        else -> Color(0xFFF3F4F6) to HaribanshoTextSecondary
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = textColor
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    onAccept: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: #ORD1250 + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = order.order_number,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = HaribanshoPrimary
                        )
                    )
                    if (order.payment_mode == "COD") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HaribanshoWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "COD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                StatusBadge(status = order.order_status)
            }

            Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            // Customer Name & Address
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Customer",
                        tint = HaribanshoTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = order.customer_name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HaribanshoTextPrimary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Address",
                        tint = HaribanshoPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = order.delivery_address,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HaribanshoTextSecondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer info: Distance & Amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HaribanshoBackground, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Distance",
                        tint = HaribanshoPrimaryVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${order.distance_km} km away",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = HaribanshoTextPrimary
                        )
                    )
                }

                Text(
                    text = "₹${String.format("%.2f", order.total_amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = HaribanshoPrimary
                    )
                )
            }

            // Actions for orders based on database status
            when {
                order.order_status.equals("Assigned", ignoreCase = true) && onAccept != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onReject != null) {
                            OutlinedButton(
                                onClick = onReject,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HaribanshoDanger),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("reject_order_button")
                            ) {
                                Text("Reject", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onAccept,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HaribanshoPrimary),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("accept_order_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accept Order", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                order.order_status.equals("Accepted", ignoreCase = true) -> {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaribanshoPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_delivery_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Delivery", fontWeight = FontWeight.Bold)
                    }
                }
                order.order_status.equals("On The Way", ignoreCase = true) || order.order_status.equals("Reached Customer", ignoreCase = true) -> {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaribanshoSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("active_delivery_button")
                    ) {
                        Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Continue Delivery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignatureCanvas(
    onSignatureCaptured: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, HaribanshoCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customer Signature",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = HaribanshoTextPrimary
                )
            )
            TextButton(
                onClick = {
                    paths.clear()
                    currentPath = null
                    onSignatureCaptured(null)
                }
            ) {
                Text("Clear", color = HaribanshoDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            val p = Path().apply { moveTo(event.x, event.y) }
                            currentPath = p
                            paths.add(p)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            currentPath?.lineTo(event.x, event.y)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            currentPath = null
                            // Pass mock bitmap indicator
                            val bmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
                            onSignatureCaptured(bmp)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color(0xFF1E293B),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            if (paths.isEmpty()) {
                Text(
                    text = "Sign inside this area",
                    color = HaribanshoTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun InteractiveMapCard(
    latitude: Double,
    longitude: Double,
    address: String,
    distanceKm: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null, tint = HaribanshoPrimary)
                    Text(
                        text = "$distanceKm km to reach customer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = HaribanshoPrimary
                        )
                    )
                }
                Surface(shape = RoundedCornerShape(20.dp), color = HaribanshoGreenSurface) {
                    Text(
                        text = "GPS Live",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HaribanshoDarkGreen),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Simulated Map Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                // Background grid / roads drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Roads
                    drawRect(color = Color(0xFFCBD5E1), topLeft = Offset(0f, h * 0.4f), size = androidx.compose.ui.geometry.Size(w, 24.dp.toPx()))
                    drawRect(color = Color(0xFFCBD5E1), topLeft = Offset(w * 0.5f, 0f), size = androidx.compose.ui.geometry.Size(24.dp.toPx(), h))

                    // Route line from DB 🛵 to Customer 📍
                    drawPath(
                        path = Path().apply {
                            moveTo(w * 0.2f, h * 0.7f)
                            lineTo(w * 0.5f, h * 0.42f)
                            lineTo(w * 0.8f, h * 0.25f)
                        },
                        color = HaribanshoPrimary,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // Delivery Boy Marker 🛵
                Surface(
                    shape = CircleShape,
                    color = HaribanshoPrimary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp, bottom = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = "Delivery Boy",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                // Destination Marker 📍
                Surface(
                    shape = CircleShape,
                    color = HaribanshoDanger,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 32.dp, top = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Customer",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }

            // Address & Navigation Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Destination:", style = MaterialTheme.typography.labelMedium, color = HaribanshoTextSecondary)
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = HaribanshoTextPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val gmapsUri = Uri.parse("google.navigation:q=$latitude,$longitude")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude"))
                            context.startActivity(webMapIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaribanshoPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Maps", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun DeliveryTimeline(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Order Accepted" to "10:30 AM",
        "On The Way" to "10:35 AM",
        "Reached Customer" to "10:50 AM",
        "Order Delivered" to "Pending"
    )

    val currentStepIndex = when (currentStatus.lowercase()) {
        "assigned" -> 0
        "accepted" -> 0
        "on the way", "picked up" -> 1
        "reached customer", "reached" -> 2
        "delivered", "completed" -> 3
        else -> 0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Delivery Timeline",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = HaribanshoTextPrimary)
            )

            steps.forEachIndexed { index, (stepTitle, defaultTime) ->
                val isDone = index <= currentStepIndex
                val isCurrent = index == currentStepIndex

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDone) HaribanshoPrimary else Color(0xFFE2E8F0)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(text = "${index + 1}", color = HaribanshoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stepTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isDone) HaribanshoTextPrimary else HaribanshoTextMuted
                            )
                        )
                        Text(
                            text = if (isDone && index < currentStepIndex) defaultTime else if (isCurrent) "Current Status" else "Pending",
                            style = MaterialTheme.typography.bodySmall.copy(color = HaribanshoTextSecondary, fontSize = 12.sp)
                        )
                    }
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 13.dp)
                            .width(2.dp)
                            .height(16.dp)
                            .background(if (index < currentStepIndex) HaribanshoPrimary else Color(0xFFE2E8F0))
                    )
                }
            }
        }
    }
}
