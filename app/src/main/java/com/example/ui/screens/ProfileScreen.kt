package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DeliveryBoy
import com.example.data.models.SupportTicket
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    deliveryBoy: DeliveryBoy,
    supportTickets: List<SupportTicket> = emptyList(),
    onCreateSupportTicket: (String, String, String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HaribanshoBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        TopAppBar(
            title = {
                Text(
                    text = "Delivery Boy Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HaribanshoPrimary)
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Info Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = HaribanshoGreenSurface,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = HaribanshoPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (deliveryBoy.name.isNotBlank()) deliveryBoy.name else "Delivery Partner",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = HaribanshoTextPrimary)
                        )
                        Text(
                            text = if (deliveryBoy.delivery_boy_id.isNotBlank()) "Delivery Boy ID: ${deliveryBoy.delivery_boy_id}" else "Delivery Partner",
                            style = MaterialTheme.typography.bodyMedium.copy(color = HaribanshoTextSecondary)
                        )
                    }

                    Divider(color = Color(0xFFF3F4F6))

                    // Contact & Vehicle Info
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileInfoItem(icon = Icons.Outlined.Phone, label = "Phone", value = if (deliveryBoy.phone.isNotBlank()) deliveryBoy.phone else "Not Provided")
                        ProfileInfoItem(icon = Icons.Outlined.Email, label = "Email", value = if (deliveryBoy.email.isNotBlank()) deliveryBoy.email else "Not Provided")
                        ProfileInfoItem(
                            icon = Icons.Outlined.TwoWheeler,
                            label = "Vehicle",
                            value = if (deliveryBoy.vehicle_number.isNotBlank()) "${deliveryBoy.vehicle_type} (${deliveryBoy.vehicle_number})" else deliveryBoy.vehicle_type.ifBlank { "Motorcycle" }
                        )
                    }
                }
            }

            // Logout Button
            Button(
                onClick = { showLogoutConfirm = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HaribanshoDanger.copy(alpha = 0.1f), contentColor = HaribanshoDanger),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(imageVector = Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout from App", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of the Haribansho Delivery Boy App?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HaribanshoDanger)
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = HaribanshoPrimary, modifier = Modifier.size(18.dp))
        Column {
            Text(label, fontSize = 11.sp, color = HaribanshoTextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HaribanshoTextPrimary)
        }
    }
}
