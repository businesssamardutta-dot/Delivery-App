package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Order
import com.example.data.remote.SupabaseService
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.utils.SoundAlertManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", "Dashboard", Icons.Outlined.Home, Icons.Filled.Home)
    object Orders : Screen("orders", "Orders", Icons.Outlined.ListAlt, Icons.Filled.ListAlt)
    object Profile : Screen("profile", "Profile & COD", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun MainScreen(supabaseService: SupabaseService) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAuthenticated by supabaseService.isAuthenticated.collectAsStateWithLifecycle()
    val deliveryBoy by supabaseService.currentDeliveryBoy.collectAsStateWithLifecycle()
    val orders by supabaseService.orders.collectAsStateWithLifecycle()
    val codSettlements by supabaseService.codSettlements.collectAsStateWithLifecycle()
    val notifications by supabaseService.notifications.collectAsStateWithLifecycle()
    val isSyncing by supabaseService.isSyncing.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedOrderForDetails by remember { mutableStateOf<Order?>(null) }
    var selectedOrderForNav by remember { mutableStateOf<Order?>(null) }
    var completedOrderSummary by remember { mutableStateOf<Pair<Order, Double>?>(null) }
    var showNotificationsScreen by remember { mutableStateOf(false) }

    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Register Sound Alert Handler for incoming assignments
    LaunchedEffect(Unit) {
        supabaseService.onNewOrderAssigned = {
            SoundAlertManager.playNewOrderChime(context)
        }
    }

    if (isAuthenticated) {
        LocationTracker(supabaseService = supabaseService)
    }

    // Live background polling for new orders every 5s when authenticated
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            supabaseService.fetchAssignedOrders()
            supabaseService.fetchCodSettlements()
            while (isActive) {
                delay(5000)
                supabaseService.fetchAssignedOrders()
            }
        }
    }

    // 1. Unauthenticated Login Screen
    if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = {},
            onLoginAttempt = { identifier, pass ->
                loginLoading = true
                loginError = null
                coroutineScope.launch {
                    val result = supabaseService.login(identifier, pass)
                    loginLoading = false
                    if (result.isFailure) {
                        loginError = result.exceptionOrNull()?.message ?: "Login failed. Please check your credentials."
                    }
                }
            },
            isLoading = loginLoading,
            errorMessage = loginError
        )
        return
    }

    // 2. Notifications Modal Screen
    if (showNotificationsScreen) {
        NotificationsScreen(
            notifications = notifications,
            onBack = { showNotificationsScreen = false },
            onMarkRead = { supabaseService.markNotificationRead(it) },
            onMarkAllRead = { supabaseService.markAllNotificationsRead() },
            onNotificationClick = { orderId ->
                showNotificationsScreen = false
                if (orderId != null) {
                    val found = orders.find { it.id == orderId }
                    if (found != null) selectedOrderForDetails = found
                }
            }
        )
        return
    }

    // 3. Delivery Completed Success Celebration Screen
    if (completedOrderSummary != null) {
        val (ord, amt) = completedOrderSummary!!
        DeliveryCompletedScreen(
            order = ord,
            collectedAmount = amt,
            onBackToHome = {
                completedOrderSummary = null
                currentTab = Screen.Home
            },
            onViewOrders = {
                completedOrderSummary = null
                currentTab = Screen.Orders
            }
        )
        return
    }

    // 4. Live GPS Navigation HUD Screen
    if (selectedOrderForNav != null) {
        val activeOrd = orders.find { it.id == selectedOrderForNav!!.id } ?: selectedOrderForNav!!
        NavigationScreen(
            order = activeOrd,
            onBack = { selectedOrderForNav = null },
            onBroadcastGps = { lat, lng ->
                coroutineScope.launch {
                    supabaseService.updateGPSLocation(activeOrd.id, lat, lng)
                }
            },
            onReachedCustomer = { orderId ->
                coroutineScope.launch {
                    supabaseService.startDelivery(orderId)
                }
            },
            onOpenPod = { ord ->
                selectedOrderForNav = null
                selectedOrderForDetails = ord
            }
        )
        return
    }

    // 5. Order Details & Execution (with POD & Manifest) Screen
    if (selectedOrderForDetails != null) {
        val currentOrd = orders.find { it.id == selectedOrderForDetails!!.id } ?: selectedOrderForDetails!!
        OrderDetailsScreen(
            order = currentOrd,
            driverName = deliveryBoy.full_name,
            onBack = { selectedOrderForDetails = null },
            onAcceptOrder = { orderId ->
                coroutineScope.launch { supabaseService.acceptOrder(orderId) }
            },
            onStartDelivery = { orderId ->
                coroutineScope.launch {
                    supabaseService.startDelivery(orderId)
                    selectedOrderForNav = currentOrd
                }
            },
            onCompleteDelivery = { orderId, amount, notes ->
                coroutineScope.launch {
                    val success = supabaseService.completeDelivery(
                        orderId = orderId,
                        collectedAmount = amount,
                        driverNotes = notes
                    )
                    if (success) {
                        completedOrderSummary = currentOrd to amount
                        selectedOrderForDetails = null
                    }
                }
            },
            onViewOnMap = { ord ->
                selectedOrderForNav = ord
            }
        )
        return
    }

    // 6. Main 3-Tab Bottom Navigation Layout
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(Color.White)
            ) {
                val items = listOf(Screen.Home, Screen.Orders, Screen.Profile)
                items.forEach { screen ->
                    val isSelected = currentTab.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldSurface,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tab_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBg)
        ) {
            when (currentTab) {
                Screen.Home -> HomeScreen(
                    deliveryBoy = deliveryBoy,
                    orders = orders,
                    codSettlements = codSettlements,
                    unreadNotificationCount = notifications.count { !it.is_read },
                    isSyncing = isSyncing,
                    onToggleOnline = { isOnline ->
                        coroutineScope.launch { supabaseService.toggleOnlineStatus(isOnline) }
                    },
                    onOrderClick = { order -> selectedOrderForDetails = order },
                    onAcceptOrder = { orderId ->
                        coroutineScope.launch { supabaseService.acceptOrder(orderId) }
                    },
                    onStartDelivery = { orderId ->
                        coroutineScope.launch {
                            supabaseService.startDelivery(orderId)
                            val ord = orders.find { it.id == orderId }
                            if (ord != null) selectedOrderForNav = ord
                        }
                    },
                    onNotificationClick = { showNotificationsScreen = true },
                    onNavigateToOrdersTab = { currentTab = Screen.Orders },
                    onRefresh = {
                        coroutineScope.launch {
                            supabaseService.fetchAssignedOrders()
                            supabaseService.fetchCodSettlements()
                        }
                    }
                )

                Screen.Orders -> OrdersScreen(
                    orders = orders,
                    driverName = deliveryBoy.full_name,
                    onAcceptOrder = { orderId ->
                        coroutineScope.launch { supabaseService.acceptOrder(orderId) }
                    },
                    onStartTrip = { orderId ->
                        coroutineScope.launch {
                            supabaseService.startDelivery(orderId)
                            val ord = orders.find { it.id == orderId }
                            if (ord != null) selectedOrderForNav = ord
                        }
                    },
                    onRejectOrder = { orderId, reason ->
                        coroutineScope.launch { supabaseService.rejectOrder(orderId, reason) }
                    },
                    onOrderClick = { order -> selectedOrderForDetails = order }
                )

                Screen.Profile -> ProfileScreen(
                    deliveryBoy = deliveryBoy,
                    codSettlements = codSettlements,
                    onToggleOnline = { isOnline ->
                        coroutineScope.launch { supabaseService.toggleOnlineStatus(isOnline) }
                    },
                    onLogout = {
                        coroutineScope.launch { supabaseService.logout() }
                    },
                    onOpenSupportTicket = {
                        coroutineScope.launch {
                            supabaseService.createSupportTicket("Shift Assistance", "Requesting dispatcher contact", "HIGH")
                        }
                    }
                )
            }
        }
    }
}
