package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverFeedRequest
import com.example.data.model.Ride
import com.example.data.model.UserProfile
import java.util.Locale

@Composable
fun DriverModeView(
    modifier: Modifier = Modifier,
    userProfile: UserProfile?,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    activeRide: Ride?,
    feedRequests: List<DriverFeedRequest>,
    onAcceptRequest: (DriverFeedRequest, Double) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onDriverArrived: () -> Unit,
    onDriverStartTrip: () -> Unit,
    onDriverCompleteTrip: () -> Unit,
    onCallPassenger: () -> Unit,
    onMessagePassenger: () -> Unit
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Driver Header: Online switch & Earnings HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFF64748B), CircleShape)
                    )
                    Column {
                        Text(
                            text = if (isOnline) "Driver Online" else "Driver Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isOnline) "Ready for ride requests" else "Switch online to earn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isOnline,
                    onCheckedChange = { onToggleOnline() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("driver_online_switch")
                )
            }

            // Driver Stats Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Today's Earnings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", userProfile?.driverEarnings ?: 142.50)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Trips", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${userProfile?.driverTrips ?: 7}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Text(
                                text = "4.98",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Active Driver Ride vs Request Feed
            if (activeRide != null && activeRide.status in listOf("DRIVER_ASSIGNED", "EN_ROUTE_PICKUP", "ARRIVED", "IN_TRIP")) {
                DriverActiveRideControls(
                    ride = activeRide,
                    onDriverArrived = onDriverArrived,
                    onDriverStartTrip = onDriverStartTrip,
                    onDriverCompleteTrip = onDriverCompleteTrip,
                    onCallPassenger = onCallPassenger,
                    onMessagePassenger = onMessagePassenger
                )
            } else if (!isOnline) {
                // Offline Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "You are currently offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toggle the switch above to go online and receive ride bids from passengers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Online: Live InDrive Request Feed
                Text(
                    text = "Nearby Passenger Ride Requests (${feedRequests.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(feedRequests) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Passenger row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(req.passengerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                                Text("${req.passengerRating}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                            Text("${req.distanceKm} km • ${req.timeAgo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Passenger offered price tag
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Offered Fare", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", req.offeredFare)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Route
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("From: ${req.pickupAddress}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Text("To: ${req.destinationAddress}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Bidding action buttons (inDrive style)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Accept at passenger's fare
                                    Button(
                                        onClick = { onAcceptRequest(req, req.offeredFare) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.2f).testTag("driver_accept_offer_button")
                                    ) {
                                        Text("Accept $${req.offeredFare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    // Counter Offer chips
                                    OutlinedButton(
                                        onClick = { onAcceptRequest(req, req.offeredFare + 2.0) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Text("+$2 ($${(req.offeredFare + 2.0).toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onAcceptRequest(req, req.offeredFare + 4.0) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Text("+$4 ($${(req.offeredFare + 4.0).toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(onClick = { onDeclineRequest(req.id) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverActiveRideControls(
    ride: Ride,
    onDriverArrived: () -> Unit,
    onDriverStartTrip: () -> Unit,
    onDriverCompleteTrip: () -> Unit,
    onCallPassenger: () -> Unit,
    onMessagePassenger: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Navigation Instructions Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    val navText = when (ride.status) {
                        "DRIVER_ASSIGNED", "EN_ROUTE_PICKUP" -> "Head towards pickup: ${ride.pickupAddress}"
                        "ARRIVED" -> "Waiting for passenger outside"
                        "IN_TRIP" -> "Navigating to: ${ride.destinationAddress}"
                        else -> "Route in progress"
                    }
                    Text(navText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Fare: $${String.format(Locale.US, "%.2f", ride.fare)} • Collect on completion", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Passenger detail row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(ride.passengerName.ifBlank { "Alex Morgan" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        Text("4.96 • Passenger", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onCallPassenger,
                    modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onMessagePassenger,
                    modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Stepper Action CTA
        when (ride.status) {
            "DRIVER_ASSIGNED", "EN_ROUTE_PICKUP" -> {
                Button(
                    onClick = onDriverArrived,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("driver_arrived_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("I Have Arrived at Pickup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            "ARRIVED" -> {
                Button(
                    onClick = onDriverStartTrip,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("driver_start_trip_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Passenger Onboard • Start Trip", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            "IN_TRIP" -> {
                Button(
                    onClick = onDriverCompleteTrip,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("driver_complete_trip_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete Trip & Collect $${String.format(Locale.US, "%.2f", ride.fare)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
