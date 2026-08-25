package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.net.Uri
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.models.Order
import com.example.ui.theme.*

@Composable
fun OnlineOfflineSwitch(
    isOnline: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOnline) EmeraldSurface else DarkSurfaceElevated,
        label = "switchBg"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isOnline) EmeraldPrimary else TextMuted,
        label = "switchDot"
    )

    Surface(
        onClick = { onToggle(!isOnline) },
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isOnline) EmeraldPrimary else DarkBorder
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("online_offline_switch")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = if (isOnline) "● ONLINE" else "○ OFFLINE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isOnline) EmeraldLight else TextSecondary
                )
            )
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status.lowercase().trim()) {
        "assigned", "pending" -> AmberSurface to AmberAlert
        "accepted" -> EmeraldSurface to EmeraldLight
        "out for delivery", "on the way", "started", "reached" -> Color(0xFF065F46) to EmeraldPrimary
        "delivered", "completed" -> EmeraldSurface to EmeraldLight
        "cancelled", "rejected" -> RedSurface to RedDanger
        else -> DarkSurfaceElevated to TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = textColor,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PaymentBadge(method: String, isPaid: Boolean, modifier: Modifier = Modifier) {
    val isCod = method.equals("COD", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isCod) AmberSurface.copy(alpha = 0.8f) else EmeraldSurface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCod) AmberAlert.copy(alpha = 0.5f) else EmeraldPrimary.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isCod) Icons.Outlined.Payments else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isCod) AmberAlert else EmeraldLight,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isCod) "CASH ON DELIVERY" else "PREPAID ONLINE",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isCod) AmberAlert else EmeraldLight
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color = EmeraldPrimary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun NewOrderAlertBanner(
    order: Order,
    onAccept: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = AmberSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AmberAlert.copy(alpha = alpha)),
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = AmberAlert)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        shape = CircleShape,
                        color = AmberAlert,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Text(
                        text = "NEW ORDER ASSIGNED!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberAlert
                        )
                    )
                }
                Text(
                    text = order.order_number,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Text(
                text = "${order.customer_name} • ${order.delivery_address}",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ₹${String.format("%.2f", order.total_amount)} (${order.payment_method})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberAlert
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onView,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text("View", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF020617))
                    ) {
                        Text("Accept", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onAccept: (() -> Unit)? = null,
    onStartTrip: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
            // Header Row: Order Number & Status
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
                            color = EmeraldLight
                        )
                    )
                    Text(
                        text = "• ${order.created_at}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                StatusBadge(status = order.order_status)
            }

            Divider(color = DarkBorder)

            // Customer & Address Info
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = order.customer_name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "(${order.customer_phone})",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = order.delivery_address,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount & Payment Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Bill Amount",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    Text(
                        text = "₹${String.format("%.2f", order.total_amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }

                PaymentBadge(
                    method = order.payment_method,
                    isPaid = order.payment_status.equals("Paid", ignoreCase = true)
                )
            }

            // Quick Actions based on status
            when (order.order_status.lowercase().trim()) {
                "assigned", "pending" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onReject != null) {
                            OutlinedButton(
                                onClick = onReject,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Reject", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (onAccept != null) {
                            Button(
                                onClick = onAccept,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color(0xFF020617)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Accept Order", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                "accepted" -> {
                    Button(
                        onClick = { onStartTrip?.invoke() ?: onClick() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF020617)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DirectionsBike, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Delivery Trip", fontWeight = FontWeight.ExtraBold)
                    }
                }

                "out for delivery", "on the way" -> {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAlert,
                            contentColor = Color(0xFF020617)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Trip • Open Navigation", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryTimeline(currentStatus: String, modifier: Modifier = Modifier) {
    val steps = listOf("Assigned", "Accepted", "Out for Delivery", "Delivered")
    val currentIdx = when (currentStatus.lowercase().trim()) {
        "assigned", "pending" -> 0
        "accepted" -> 1
        "out for delivery", "on the way", "started", "reached", "reached customer" -> 2
        "delivered", "completed" -> 3
        else -> 0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Delivery State Machine",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    val isDone = index <= currentIdx
                    val isCurrent = index == currentIdx

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDone) EmeraldPrimary else DarkSurfaceElevated,
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, EmeraldLight) else null,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF020617),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Text(
                            text = step,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone) TextPrimary else TextMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .weight(0.5f)
                                .background(if (index < currentIdx) EmeraldPrimary else DarkBorder)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignatureCanvas(
    onSignatureCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val path = remember { AndroidPath() }
    var pathVersion by remember { mutableIntStateOf(0) }
    var hasSigned by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
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
                    text = "Customer Digital Signature (POD)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                if (hasSigned) {
                    TextButton(
                        onClick = {
                            path.reset()
                            hasSigned = false
                            pathVersion++
                        }
                    ) {
                        Text("Clear", color = RedDanger, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, if (hasSigned) EmeraldPrimary else DarkBorder, RoundedCornerShape(12.dp))
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                path.moveTo(motionEvent.x, motionEvent.y)
                                hasSigned = true
                                pathVersion++
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                path.lineTo(motionEvent.x, motionEvent.y)
                                pathVersion++
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                pathVersion++
                                val bmp = Bitmap.createBitmap(400, 140, Bitmap.Config.ARGB_8888)
                                val canvas = AndroidCanvas(bmp)
                                val paint = Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    strokeWidth = 5f
                                    style = Paint.Style.STROKE
                                    isAntiAlias = true
                                }
                                canvas.drawPath(path, paint)
                                onSignatureCaptured(bmp)
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val composePath = path.asComposePath()
                    drawPath(
                        path = composePath,
                        color = EmeraldLight,
                        style = Stroke(width = 4f)
                    )
                }

                if (!hasSigned) {
                    Text(
                        text = "Customer sign here using finger",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMapCard(
    latitude: Double,
    longitude: Double,
    address: String,
    distanceKm: Double,
    customerName: String = "Customer Destination",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isOsmLoaded by remember { mutableStateOf(false) }

    // Coordinates for Rider (Kolkata Hub reference) and Destination
    val riderLat = 22.572645
    val riderLng = 88.363892
    val destLat = if (latitude != 0.0) latitude else 22.5833
    val destLng = if (longitude != 0.0) longitude else 88.4633

    val safeCustName = customerName.replace("'", "\\'").replace("\"", "\\\"")
    val safeAddress = address.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")

    val osmHtml = remember(destLat, destLng, safeCustName, safeAddress) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #020617; overflow: hidden; }
                #map { width: 100%; height: 100%; background: #020617; }
                .leaflet-container { background: #020617; font-family: sans-serif; }
                .leaflet-popup-content-wrapper { background: #0f172a; color: #f8fafc; border: 1px solid #10b981; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.5); }
                .leaflet-popup-tip { background: #0f172a; }
                .leaflet-bar a { background-color: #0f172a; color: #10b981; border-bottom: 1px solid #334155; }
                .leaflet-bar a:hover { background-color: #1e293b; color: #34d399; }
                .leaflet-control-attribution { background: rgba(15, 23, 42, 0.8) !important; color: #94a3b8 !important; font-size: 9px !important; }
                .leaflet-control-attribution a { color: #10b981 !important; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var riderLat = $riderLat;
                var riderLng = $riderLng;
                var destLat = $destLat;
                var destLng = $destLng;

                var map = L.map('map', {
                    zoomControl: true,
                    attributionControl: true
                }).setView([(riderLat + destLat)/2, (riderLng + destLng)/2], 13);

                // OpenStreetMap standard tile layer
                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                }).addTo(map);

                // Rider Marker (Live Moving Rider)
                var riderIcon = L.divIcon({
                    className: 'rider-pin',
                    html: '<div style="background:#10b981; width:30px; height:30px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2px solid #ffffff; box-shadow:0 0 10px rgba(16,185,129,0.9); font-size:15px;">🛵</div>',
                    iconSize: [30, 30],
                    iconAnchor: [15, 15]
                });
                var riderMarker = L.marker([riderLat, riderLng], {icon: riderIcon}).addTo(map)
                    .bindPopup("<b>🛵 Rider Live Location</b><br>Speed: 24 km/h • Haribansho Fleet");

                // Customer Destination Marker
                var destIcon = L.divIcon({
                    className: 'dest-pin',
                    html: '<div style="background:#f59e0b; width:32px; height:32px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2px solid #ffffff; box-shadow:0 0 12px rgba(245,158,11,0.9); font-size:16px;">📍</div>',
                    iconSize: [32, 32],
                    iconAnchor: [16, 16]
                });
                var destMarker = L.marker([destLat, destLng], {icon: destIcon}).addTo(map)
                    .bindPopup("<b>📍 $safeCustName</b><br><small>$safeAddress</small>").openPopup();

                // Connect with route line
                var midLat = (riderLat * 0.4 + destLat * 0.6);
                var midLng = (riderLng * 0.6 + destLng * 0.4);
                var polyline = L.polyline([
                    [riderLat, riderLng],
                    [midLat, midLng],
                    [destLat, destLng]
                ], {
                    color: '#10b981',
                    weight: 5,
                    opacity: 0.85,
                    dashArray: '8, 8'
                }).addTo(map);

                map.fitBounds(polyline.getBounds(), { padding: [35, 35] });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Customer Name, OSM branding & ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customerName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Text(
                            text = "OpenStreetMap • $distanceKm km • ETA ~${(distanceKm * 3.5).toInt() + 4} mins",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldLight, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSurface
                ) {
                    Text(
                        text = "OSM Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Real OpenStreetMap WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isOsmLoaded = true
                                }
                            }
                            loadDataWithBaseURL(
                                "https://tile.openstreetmap.org",
                                osmHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { view ->
                        view.loadDataWithBaseURL(
                            "https://tile.openstreetmap.org",
                            osmHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay status pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Rider: 24 km/h • OpenStreetMap",
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Address display
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Single Full-Width OpenStreetMap Navigation Button (Google Maps removed as requested)
            Button(
                onClick = {
                    val osmUrl = "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=$riderLat,$riderLng;$destLat,$destLng"
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
