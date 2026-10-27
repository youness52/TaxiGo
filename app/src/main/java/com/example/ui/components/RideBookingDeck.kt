package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ride
import com.example.data.model.RideTierOption
import java.util.Locale

@Composable
fun RideBookingDeck(
    modifier: Modifier = Modifier,
    activeRide: Ride?,
    isBooking: Boolean,
    rideTiers: List<RideTierOption>,
    selectedTier: RideTierOption,
    onSelectTier: (RideTierOption) -> Unit,
    pickupAddress: String,
    destinationAddress: String,
    onAddressClick: () -> Unit,
    offeredFare: Double = 16.0,
    onBoostFare: (Double) -> Unit = {},
    selectedPaymentMethod: String,
    onPaymentMethodClick: () -> Unit,
    selectedTip: Double,
    onSelectTip: (Double) -> Unit,
    onRequestRide: () -> Unit,
    onCancelRide: () -> Unit,
    onCallDriver: () -> Unit,
    onMessageDriver: () -> Unit,
    onSkipToArrived: () -> Unit,
    onSkipToCompleted: () -> Unit,
    onRateDriver: (Double) -> Unit,
    onViewReceipt: () -> Unit,
    onNewRideClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val status = activeRide?.status ?: "IDLE"

            when (status) {
                "IDLE", "CANCELLED" -> {
                    IdleBookingContent(
                        pickupAddress = pickupAddress,
                        destinationAddress = destinationAddress,
                        onAddressClick = onAddressClick,
                        offeredFare = offeredFare,
                        onBoostFare = onBoostFare,
                        rideTiers = rideTiers,
                        selectedTier = selectedTier,
                        onSelectTier = onSelectTier,
                        selectedPaymentMethod = selectedPaymentMethod,
                        onPaymentMethodClick = onPaymentMethodClick,
                        selectedTip = selectedTip,
                        onSelectTip = onSelectTip,
                        isBooking = isBooking,
                        onRequestRide = onRequestRide
                    )
                }

                "SEARCHING" -> {
                    SearchingDriverContent(
                        selectedTier = selectedTier,
                        onCancelRide = onCancelRide
                    )
                }

                "DRIVER_ASSIGNED", "EN_ROUTE_PICKUP", "ARRIVED", "IN_TRIP" -> {
                    ActiveRideTrackingContent(
                        ride = activeRide!!,
                        onCallDriver = onCallDriver,
                        onMessageDriver = onMessageDriver,
                        onCancelRide = onCancelRide,
                        onSkipToArrived = onSkipToArrived,
                        onSkipToCompleted = onSkipToCompleted
                    )
                }

                "COMPLETED" -> {
                    TripCompletedContent(
                        ride = activeRide!!,
                        onRateDriver = onRateDriver,
                        onViewReceipt = onViewReceipt,
                        onNewRideClick = onNewRideClick
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleBookingContent(
    pickupAddress: String,
    destinationAddress: String,
    onAddressClick: () -> Unit,
    offeredFare: Double,
    onBoostFare: (Double) -> Unit,
    rideTiers: List<RideTierOption>,
    selectedTier: RideTierOption,
    onSelectTier: (RideTierOption) -> Unit,
    selectedPaymentMethod: String,
    onPaymentMethodClick: () -> Unit,
    selectedTip: Double,
    onSelectTip: (Double) -> Unit,
    isBooking: Boolean,
    onRequestRide: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Location address preview card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onAddressClick() },
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                    Box(modifier = Modifier.width(2.dp).height(20.dp).background(MaterialTheme.colorScheme.outline))
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pickupAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = destinationAddress,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        // inDrive "Offer Your Fare" Stepper & Chips Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Offer Your Fare",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Drivers can accept or make counter-offers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper: - $1 / + $1
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp
                        ) {
                            IconButton(
                                onClick = { onBoostFare(-1.0) },
                                modifier = Modifier.size(38.dp).testTag("decrease_fare_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Text(
                            text = "$${String.format(Locale.US, "%.0f", offeredFare)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp
                        ) {
                            IconButton(
                                onClick = { onBoostFare(1.0) },
                                modifier = Modifier.size(38.dp).testTag("increase_fare_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Quick Fare Suggestion Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.0, 2.0, 5.0).forEach { delta ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onBoostFare(delta) },
                            color = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = "+$${delta.toInt()}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBoostFare(selectedTier.basePrice - offeredFare) },
                        color = MaterialTheme.colorScheme.surface,
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Text(
                            text = "Rec: $${selectedTier.basePrice.toInt()}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = "Select Vehicle Class",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        // Tier cards carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rideTiers) { tier ->
                val isSelected = selectedTier.id == tier.id
                val tierIcon: ImageVector = when (tier.id) {
                    "green" -> Icons.Default.ElectricCar
                    "xl" -> Icons.Default.DirectionsCar
                    else -> Icons.Default.LocalTaxi
                }

                Surface(
                    modifier = Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectTier(tier) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tierIcon,
                                contentDescription = tier.name,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "${tier.etaMins} min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = tier.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Est. $${String.format(Locale.US, "%.2f", tier.basePrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Tip selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Tip:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.0, 2.0, 3.0, 5.0).forEach { tip ->
                    val isTipSelected = selectedTip == tip
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTip(tip) },
                        color = if (isTipSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Text(
                            text = if (tip == 0.0) "None" else "+$${tip.toInt()}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isTipSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Payment Method Selector Pill & Confirm CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPaymentMethodClick() }
                    .weight(0.42f)
                    .height(54.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pIcon = when (selectedPaymentMethod) {
                        "wallet" -> Icons.Default.AccountBalanceWallet
                        "cash" -> Icons.Default.LocalAtm
                        else -> Icons.Default.CreditCard
                    }
                    Icon(
                        imageVector = pIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (selectedPaymentMethod) {
                            "wallet" -> "Wallet"
                            "cash" -> "Cash"
                            "google_pay" -> "G-Pay"
                            else -> "•••• 4242"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            val totalFare = offeredFare + selectedTip
            Button(
                onClick = onRequestRide,
                enabled = !isBooking,
                modifier = Modifier
                    .weight(0.58f)
                    .height(54.dp)
                    .testTag("confirm_booking_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isBooking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Find Driver • $${String.format(Locale.US, "%.0f", totalFare)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchingDriverContent(
    selectedTier: RideTierOption,
    onCancelRide: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulseScale)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Searching",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Connecting with Nearby Drivers...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${selectedTier.name} • Arriving in ~${selectedTier.etaMins} mins",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onCancelRide,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("cancel_search_button")
        ) {
            Text("Cancel Request", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ActiveRideTrackingContent(
    ride: Ride,
    onCallDriver: () -> Unit,
    onMessageDriver: () -> Unit,
    onCancelRide: () -> Unit,
    onSkipToArrived: () -> Unit,
    onSkipToCompleted: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Status Badge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (statusText, statusColor) = when (ride.status) {
                "DRIVER_ASSIGNED" -> Pair("Driver Matched", MaterialTheme.colorScheme.primary)
                "EN_ROUTE_PICKUP" -> Pair("Driver En Route (${ride.etaMinutes} min)", MaterialTheme.colorScheme.primary)
                "ARRIVED" -> Pair("Driver Arrived at Pickup!", MaterialTheme.colorScheme.tertiary)
                "IN_TRIP" -> Pair("En Route to Destination", MaterialTheme.colorScheme.secondary)
                else -> Pair(ride.status, MaterialTheme.colorScheme.primary)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Text(
                text = "$${String.format(Locale.US, "%.2f", ride.fare + ride.tip)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Driver details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Driver",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ride.driverName.ifBlank { "Marcus Vance" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Text(
                                text = "${ride.driverRating}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${ride.carModel} • ${ride.carColor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Plate: ${ride.licensePlate}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Driver Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCallDriver,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("call_driver_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onMessageDriver,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("message_driver_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Developer / Demo Telemetry Fast-Forward controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Simulation Mode:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ride.status != "ARRIVED" && ride.status != "IN_TRIP") {
                    TextButton(onClick = onSkipToArrived) {
                        Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Skip to Arrived", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onSkipToCompleted) {
                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip to End", fontSize = 12.sp)
                }
            }
        }

        if (ride.status != "IN_TRIP") {
            OutlinedButton(
                onClick = onCancelRide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("cancel_ride_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel Ride", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TripCompletedContent(
    ride: Ride,
    onRateDriver: (Double) -> Unit,
    onViewReceipt: () -> Unit,
    onNewRideClick: () -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "You've Arrived!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Trip to ${ride.destinationAddress} is complete. Total paid: $${String.format(Locale.US, "%.2f", ride.fare + ride.tip)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Star rating bar
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { star ->
                val isSelected = star <= rating
                IconButton(onClick = {
                    rating = star
                    onRateDriver(star.toDouble())
                }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $star",
                        tint = if (isSelected) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewReceipt,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("view_receipt_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Receipt", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNewRideClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("book_another_ride_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Book Another", fontWeight = FontWeight.Bold)
            }
        }
    }
}
