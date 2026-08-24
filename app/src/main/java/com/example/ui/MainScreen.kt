package com.example.ui

import android.graphics.Bitmap
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Orders : Screen("orders", "Orders", Icons.Outlined.ListAlt, Icons.Filled.ListAlt)
    object Profile : Screen("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun MainScreen(supabaseService: SupabaseService) {
    val coroutineScope = rememberCoroutineScope()
    val isAuthenticated by supabaseService.isAuthenticated.collectAsStateWithLifecycle()
    val deliveryBoy by supabaseService.currentDeliveryBoy.collectAsStateWithLifecycle()
    val orders by supabaseService.orders.collectAsStateWithLifecycle()
    val notifications by supabaseService.notifications.collectAsStateWithLifecycle()
    val supportTickets by supabaseService.supportTickets.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedOrderForDetails by remember { mutableStateOf<Order?>(null) }
    var selectedOrderForNav by remember { mutableStateOf<Order?>(null) }
    var selectedOrderForCompletion by remember { mutableStateOf<Order?>(null) }
    var completedOrderSummary by remember { mutableStateOf<Pair<Order, Double>?>(null) }
    var showNotificationsScreen by remember { mutableStateOf(false) }

    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            while (isActive) {
                supabaseService.fetchAssignedOrders()
                delay(4000)
            }
        }
    }

    if (!isAuthenticated) {
        LoginScreen(
            onLoginSuccess = {},
            onLoginAttempt = { user, pass ->
                loginLoading = true
                loginError = null
                coroutineScope.launch {
                    val result = supabaseService.login(user, pass)
                    loginLoading = false
                    if (result.isFailure) {
                        loginError = "Invalid credentials. Please try again."
                    }
                }
            },
            isLoading = loginLoading,
            errorMessage = loginError
        )
        return
    }

    // Modal / Stack Screen Routing
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

    if (selectedOrderForCompletion != null) {
        val ord = selectedOrderForCompletion!!
        DeliveryCompletionDialogScreen(
            order = ord,
            onClose = { selectedOrderForCompletion = null },
            onSubmitComplete = { orderId, amt, sig, proof ->
                coroutineScope.launch {
                    val success = supabaseService.completeDelivery(orderId, amt, null, proof)
                    if (success) {
                        completedOrderSummary = ord to amt
                        selectedOrderForCompletion = null
                        selectedOrderForNav = null
                        selectedOrderForDetails = null
                    }
                }
            }
        )
        return
    }

    if (selectedOrderForNav != null) {
        val activeOrd = orders.find { it.id == selectedOrderForNav!!.id } ?: selectedOrderForNav!!
        NavigationScreen(
            order = activeOrd,
            onBack = { selectedOrderForNav = null },
            onReachedCustomer = { orderId ->
                coroutineScope.launch {
                    supabaseService.reachCustomer(orderId)
                }
            },
            onMarkDelivered = { ordToComplete ->
                selectedOrderForCompletion = ordToComplete
            }
        )
        return
    }

    if (selectedOrderForDetails != null) {
        val currentOrd = orders.find { it.id == selectedOrderForDetails!!.id } ?: selectedOrderForDetails!!
        OrderDetailsScreen(
            order = currentOrd,
            onBack = { selectedOrderForDetails = null },
            onStartDelivery = { orderId ->
                coroutineScope.launch {
                    supabaseService.startDelivery(orderId)
                    selectedOrderForNav = currentOrd
                }
            },
            onViewOnMap = { ord ->
                selectedOrderForNav = ord
            }
        )
        return
    }

    // Main Bottom Nav Scaffold Layout
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
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
                            selectedIconColor = HaribanshoPrimary,
                            selectedTextColor = HaribanshoPrimary,
                            indicatorColor = HaribanshoGreenSurface,
                            unselectedIconColor = HaribanshoTextSecondary,
                            unselectedTextColor = HaribanshoTextSecondary
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
        ) {
            when (currentTab) {
                Screen.Home -> HomeScreen(
                    deliveryBoy = deliveryBoy,
                    orders = orders,
                    unreadNotificationCount = notifications.count { !it.is_read },
                    onToggleOnline = { isOnline ->
                        coroutineScope.launch { supabaseService.toggleOnlineStatus(isOnline) }
                    },
                    onOrderClick = { order -> selectedOrderForDetails = order },
                    onNotificationClick = { showNotificationsScreen = true },
                    onNavigateToOrdersTab = { currentTab = Screen.Orders }
                )

                Screen.Orders -> OrdersScreen(
                    orders = orders,
                    onAcceptOrder = { orderId ->
                        coroutineScope.launch { supabaseService.acceptOrder(orderId) }
                    },
                    onRejectOrder = { orderId, reason ->
                        coroutineScope.launch { supabaseService.rejectOrder(orderId, reason) }
                    },
                    onOrderClick = { order -> selectedOrderForDetails = order }
                )

                Screen.Profile -> ProfileScreen(
                    deliveryBoy = deliveryBoy,
                    supportTickets = supportTickets,
                    onCreateSupportTicket = { subj, desc, prio ->
                        coroutineScope.launch { supabaseService.createSupportTicket(subj, desc, prio) }
                    },
                    onLogout = {
                        coroutineScope.launch { supabaseService.logout() }
                    }
                )
            }
        }
    }
}
