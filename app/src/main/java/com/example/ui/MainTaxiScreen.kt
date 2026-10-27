package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.auth.signOut
import com.example.ui.components.AddCardDialog
import com.example.ui.components.AddressSearchSheet
import com.example.ui.components.CallDriverDialog
import com.example.ui.components.DriverChatSheet
import com.example.ui.components.DriverModeView
import com.example.ui.components.InDriveBidsSheet
import com.example.ui.components.PaymentMethodSelectorSheet
import com.example.ui.components.ReceiptDetailDialog
import com.example.ui.components.ReceiptsHistorySheet
import com.example.ui.components.RideBookingDeck
import com.example.ui.components.RoleSelectionDialog
import com.example.ui.components.WalletTopUpDialog
import com.example.ui.map.RealTileMapView
import java.util.Locale

@Composable
fun MainTaxiScreen(
    viewModel: TaxiViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Real GPS Location Permission Handler
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchRealLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        viewModel.fetchRealLocation(context)
    }

    // State Collection
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeRide by viewModel.activeRide.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()

    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val showRoleDialog by viewModel.showRoleDialog.collectAsStateWithLifecycle()

    val userLat by viewModel.userLat.collectAsStateWithLifecycle()
    val userLng by viewModel.userLng.collectAsStateWithLifecycle()

    val selectedTier by viewModel.selectedTier.collectAsStateWithLifecycle()
    val pickupAddress by viewModel.pickupAddress.collectAsStateWithLifecycle()
    val destinationAddress by viewModel.destinationAddress.collectAsStateWithLifecycle()
    val offeredFare by viewModel.offeredFare.collectAsStateWithLifecycle()
    val driverBids by viewModel.driverBids.collectAsStateWithLifecycle()
    val showBidsSheet by viewModel.showBidsSheet.collectAsStateWithLifecycle()

    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val selectedTip by viewModel.selectedTip.collectAsStateWithLifecycle()

    val driverProgress by viewModel.driverProgress.collectAsStateWithLifecycle()
    val driverSpeedKmh by viewModel.driverSpeedKmh.collectAsStateWithLifecycle()
    val currentRoad by viewModel.currentRoad.collectAsStateWithLifecycle()
    val isBooking by viewModel.isBooking.collectAsStateWithLifecycle()

    val isDriverOnline by viewModel.isDriverOnline.collectAsStateWithLifecycle()
    val driverFeedRequests by viewModel.driverFeedRequests.collectAsStateWithLifecycle()

    // Dialog & Sheet states
    val showAddressSearchSheet by viewModel.showAddressSearchSheet.collectAsStateWithLifecycle()
    val showPaymentSheet by viewModel.showPaymentSheet.collectAsStateWithLifecycle()
    val showAddCardDialog by viewModel.showAddCardDialog.collectAsStateWithLifecycle()
    val showTopUpDialog by viewModel.showTopUpDialog.collectAsStateWithLifecycle()
    val showDriverChat by viewModel.showDriverChat.collectAsStateWithLifecycle()
    val showCallModal by viewModel.showCallModal.collectAsStateWithLifecycle()
    val selectedReceiptForModal by viewModel.selectedReceiptForModal.collectAsStateWithLifecycle()
    val driverMessages by viewModel.driverMessages.collectAsStateWithLifecycle()

    var showHistorySheet by remember { mutableStateOf(false) }

    val isTripActive = activeRide != null && activeRide?.status !in listOf("IDLE", "COMPLETED", "CANCELLED")

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full Screen Real-World Tile Map (CartoDB / OpenStreetMap)
        RealTileMapView(
            centerLat = userLat,
            centerLng = userLng,
            ride = activeRide,
            driverProgress = driverProgress,
            speedKmh = driverSpeedKmh,
            currentRoadName = currentRoad,
            isTripActive = isTripActive,
            onCenterOnLocation = {
                viewModel.fetchRealLocation(context)
            }
        )

        // 2. Top Header Overlay (Branding, Role Switcher, Wallet Balance, History, Sign Out)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Role Mode Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentRole == "driver") Icons.Default.DirectionsCar else Icons.Default.LocalTaxi,
                            contentDescription = "TaxiGo inDrive",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Role Switcher Pill (Passenger vs Driver)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.toggleRoleDialog(true) }
                            .testTag("role_switcher_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (currentRole == "driver") "🚘 Driver Mode" else "🚕 Passenger",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Role",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Action Chips: Wallet Balance & History & Sign Out
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Wallet Chip Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.togglePaymentSheet(true) }
                            .testTag("wallet_balance_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            val balance = if (currentRole == "driver") {
                                userProfile?.driverEarnings ?: 142.50
                            } else {
                                userProfile?.walletBalance ?: 50.0
                            }
                            Text(
                                text = "$${String.format(Locale.US, "%.0f", balance)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Trip History Icon Button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Trip History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Sign Out Icon Button
                    IconButton(
                        onClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = coroutineScope
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Bottom Sliding Deck: inDrive Role-Specific View
        if (currentRole == "driver") {
            // DRIVER MODE: Online Switch, Earnings HUD, Incoming Passenger Requests, Navigation Stepper
            DriverModeView(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                userProfile = userProfile,
                isOnline = isDriverOnline,
                onToggleOnline = { viewModel.toggleDriverOnline() },
                activeRide = activeRide,
                feedRequests = driverFeedRequests,
                onAcceptRequest = { req, fare -> viewModel.driverAcceptRequest(req, fare) },
                onDeclineRequest = { id -> viewModel.driverDeclineRequest(id) },
                onDriverArrived = { viewModel.driverArrivedAtPickup() },
                onDriverStartTrip = { viewModel.driverStartTrip() },
                onDriverCompleteTrip = { viewModel.driverCompleteTrip() },
                onCallPassenger = { viewModel.toggleCallModal(true) },
                onMessagePassenger = { viewModel.toggleDriverChat(true) }
            )
        } else {
            // PASSENGER MODE: Either InDrive Bids Sheet or Fare Offer Booking Deck
            if (showBidsSheet && activeRide?.status == "SEARCHING") {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    InDriveBidsSheet(
                        offeredFare = offeredFare,
                        driverBids = driverBids,
                        onAcceptBid = { viewModel.acceptDriverBid(it) },
                        onIncreaseFare = { viewModel.boostOfferedFare(it) },
                        onCancelRequest = { viewModel.cancelActiveRide() }
                    )
                }
            } else {
                RideBookingDeck(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    activeRide = activeRide,
                    isBooking = isBooking,
                    rideTiers = viewModel.rideTiers,
                    selectedTier = selectedTier,
                    onSelectTier = { viewModel.selectTier(it) },
                    pickupAddress = pickupAddress,
                    destinationAddress = destinationAddress,
                    onAddressClick = { viewModel.toggleAddressSearchSheet(true) },
                    offeredFare = offeredFare,
                    onBoostFare = { viewModel.boostOfferedFare(it) },
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodClick = { viewModel.togglePaymentSheet(true) },
                    selectedTip = selectedTip,
                    onSelectTip = { viewModel.setTip(it) },
                    onRequestRide = { viewModel.requestRideWithOffer() },
                    onCancelRide = { viewModel.cancelActiveRide() },
                    onCallDriver = { viewModel.toggleCallModal(true) },
                    onMessageDriver = { viewModel.toggleDriverChat(true) },
                    onSkipToArrived = { viewModel.skipToArrived() },
                    onSkipToCompleted = { viewModel.skipToCompleted() },
                    onRateDriver = { viewModel.submitRating(it) },
                    onViewReceipt = {
                        val lastReceipt = receipts.firstOrNull()
                        if (lastReceipt != null) {
                            viewModel.showReceiptDetails(lastReceipt)
                        }
                    },
                    onNewRideClick = {
                        viewModel.cancelActiveRide()
                    }
                )
            }
        }
    }

    // --- MODALS AND SHEETS ---

    // 0. Role Selection Modal (Passenger vs Driver)
    if (showRoleDialog) {
        RoleSelectionDialog(
            currentRole = currentRole,
            onRoleSelected = { role ->
                viewModel.setRole(role)
            },
            onDismiss = { viewModel.toggleRoleDialog(false) }
        )
    }

    // 1. Address Search Modal
    if (showAddressSearchSheet) {
        AddressSearchSheet(
            initialPickup = pickupAddress,
            initialDestination = destinationAddress,
            savedPlaces = savedPlaces,
            onSelectAddresses = { pickup, dest ->
                viewModel.setPickup(pickup)
                viewModel.setDestination(dest)
            },
            onDismiss = { viewModel.toggleAddressSearchSheet(false) }
        )
    }

    // 2. Payment Selector Sheet
    if (showPaymentSheet) {
        PaymentMethodSelectorSheet(
            onDismiss = { viewModel.togglePaymentSheet(false) },
            paymentMethods = paymentMethods,
            userProfile = userProfile,
            selectedMethod = selectedPaymentMethod,
            onSelectMethod = { method ->
                viewModel.setPaymentMethod(method)
                viewModel.togglePaymentSheet(false)
            },
            onAddCardClick = {
                viewModel.togglePaymentSheet(false)
                viewModel.toggleAddCardDialog(true)
            },
            onTopUpClick = {
                viewModel.togglePaymentSheet(false)
                viewModel.toggleTopUpDialog(true)
            },
            onDeletePaymentMethod = { pmId ->
                viewModel.deletePaymentMethod(pmId)
            }
        )
    }

    // 3. Add Card Dialog
    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { viewModel.toggleAddCardDialog(false) },
            onSaveCard = { number, name, expiry, brand ->
                viewModel.addNewCard(number, name, expiry, brand)
            }
        )
    }

    // 4. Wallet Top-Up Dialog
    if (showTopUpDialog) {
        WalletTopUpDialog(
            currentBalance = userProfile?.walletBalance ?: 50.0,
            onDismiss = { viewModel.toggleTopUpDialog(false) },
            onTopUp = { amt ->
                viewModel.topUpWallet(amt)
            }
        )
    }

    // 5. Digital Receipt Modal
    selectedReceiptForModal?.let { receipt ->
        ReceiptDetailDialog(
            receipt = receipt,
            onDismiss = { viewModel.showReceiptDetails(null) }
        )
    }

    // 6. Driver Chat Sheet
    if (showDriverChat) {
        DriverChatSheet(
            driverName = activeRide?.driverName?.ifBlank { "Marcus Vance" } ?: "Marcus Vance",
            carModel = activeRide?.carModel?.ifBlank { "Tesla Model Y" } ?: "Tesla Model Y",
            messages = driverMessages,
            onSendMessage = { viewModel.sendDriverMessage(it) },
            onDismiss = { viewModel.toggleDriverChat(false) }
        )
    }

    // 7. Call Driver Dialog
    if (showCallModal) {
        CallDriverDialog(
            driverName = activeRide?.driverName?.ifBlank { "Marcus Vance" } ?: "Marcus Vance",
            driverPhone = activeRide?.driverPhone?.ifBlank { "+1 (415) 890-4421" } ?: "+1 (415) 890-4421",
            carModel = activeRide?.carModel?.ifBlank { "Tesla Model Y" } ?: "Tesla Model Y",
            onDismiss = { viewModel.toggleCallModal(false) }
        )
    }

    // 8. Trip & Payment History Sheet
    if (showHistorySheet) {
        ReceiptsHistorySheet(
            receipts = receipts,
            onSelectReceipt = { receipt ->
                showHistorySheet = false
                viewModel.showReceiptDetails(receipt)
            },
            onDismiss = { showHistorySheet = false }
        )
    }
}
