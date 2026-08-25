package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CodSettlement
import com.example.data.models.DeliveryBoy
import com.example.ui.components.OnlineOfflineSwitch
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    deliveryBoy: DeliveryBoy,
    codSettlements: List<CodSettlement>,
    onToggleOnline: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onOpenSupportTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var depositSuccess by remember { mutableStateOf(false) }

    val pendingCashTotal = codSettlements
        .filter { it.status == "Collected_By_Rider" }
        .sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Rider Profile & Settlements",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        // Hero Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(EmeraldDark, DarkBg)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(3.dp, EmeraldPrimary),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = deliveryBoy.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Employee Code: ${deliveryBoy.employee_code} • ${deliveryBoy.phone}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }

                OnlineOfflineSwitch(
                    isOnline = deliveryBoy.is_online,
                    onToggle = onToggleOnline
                )
            }
        }

        // Main Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Performance Stats Grid
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(18.dp))
                            Text(
                                text = String.format("%.1f", deliveryBoy.rating),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        }
                        Text("Rating", fontSize = 12.sp, color = TextSecondary)
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(DarkBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${deliveryBoy.total_deliveries}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text("Deliveries", fontSize = 12.sp, color = TextSecondary)
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(DarkBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "99.2%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = EmeraldLight
                        )
                        Text("On-Time Rate", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // COD Cash in Hand & Hub Settlement Section
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberAlert.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
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
                            Icon(imageVector = Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = AmberAlert)
                            Text(
                                text = "COD Cash in Hand (Today)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberSurface
                        ) {
                            Text(
                                text = "₹${String.format("%.2f", pendingCashTotal)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberAlert,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Cash collected from cash-on-delivery orders. Settle with Haribansho Hub manager at the end of your shift.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    Button(
                        onClick = { showDepositDialog = true },
                        enabled = pendingCashTotal > 0,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAlert,
                            contentColor = Color(0xFF020617),
                            disabledContainerColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deposit Cash to Hub Supervisor", fontWeight = FontWeight.Bold)
                    }

                    if (depositSuccess) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cash deposit submitted for supervisor verification ✓",
                                color = EmeraldLight,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Vehicle & Assigned Zone Info
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
                        text = "Vehicle & Dispatch Zone",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vehicle Details:", color = TextSecondary, fontSize = 13.sp)
                        Text(deliveryBoy.vehicle_info, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Assigned Zone:", color = TextSecondary, fontSize = 13.sp)
                        Text(deliveryBoy.zone_name, fontWeight = FontWeight.Bold, color = EmeraldLight, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Driving License:", color = TextSecondary, fontSize = 13.sp)
                        Text(deliveryBoy.license_number.ifBlank { "WB-02-2023-LIC-987" }, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Support & Helpdesk
            Card(
                onClick = onOpenSupportTicket,
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.HeadsetMic, contentDescription = null, tint = EmeraldLight)
                        Column {
                            Text("Hub Support & Dispatcher Line", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Contact Hub Dispatcher for escalations", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Shift & Sign Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Hub Cash Deposit Dialog
    if (showDepositDialog) {
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Deposit Cash to Hub Supervisor",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to record the deposit of ₹${String.format("%.2f", pendingCashTotal)} with the Hub Supervisor?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDepositDialog = false
                        depositSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF020617))
                ) {
                    Text("Confirm Deposit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "End Shift & Logout",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "You will be set to Offline status and will stop receiving delivery alerts until your next shift.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("End Shift", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
