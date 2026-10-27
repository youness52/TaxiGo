package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SavedPlace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchSheet(
    initialPickup: String,
    initialDestination: String,
    savedPlaces: List<SavedPlace>,
    onSelectAddresses: (pickup: String, destination: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickupText by remember { mutableStateOf(initialPickup) }
    var destinationText by remember { mutableStateOf(initialDestination) }
    var activeSearchTarget by remember { mutableStateOf("destination") } // "pickup" or "destination"

    val popularLocations = listOf(
        Pair("SFO International Airport, Terminal 2", "San Francisco, CA"),
        Pair("The Embarcadero & Ferry Building", "1 Ferry Building, San Francisco"),
        Pair("Palace of Fine Arts", "3301 Lyon St, San Francisco"),
        Pair("Moscone Center Convention Hall", "747 Howard St, San Francisco"),
        Pair("Golden Gate Park Botanical Garden", "1199 9th Ave, San Francisco"),
        Pair("Fisherman's Wharf & Pier 39", "Beach St & The Embarcadero"),
        Pair("Stanford University Medical Center", "300 Pasteur Dr, Stanford"),
        Pair("Union Square Shopping District", "333 Post St, San Francisco")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Route",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inputs
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        )
                        OutlinedTextField(
                            value = pickupText,
                            onValueChange = {
                                pickupText = it
                                activeSearchTarget = "pickup"
                            },
                            placeholder = { Text("Pickup Location") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                                .testTag("pickup_address_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        OutlinedTextField(
                            value = destinationText,
                            onValueChange = {
                                destinationText = it
                                activeSearchTarget = "destination"
                            },
                            placeholder = { Text("Where to?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                                .testTag("destination_address_input"),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Saved & Suggested Locations",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Saved Places
                items(savedPlaces) { place ->
                    val icon = when (place.iconType) {
                        "home" -> Icons.Default.Home
                        "work" -> Icons.Default.Work
                        else -> Icons.Default.Place
                    }
                    LocationItemRow(
                        title = place.title,
                        subtitle = place.address,
                        icon = icon,
                        onClick = {
                            if (activeSearchTarget == "pickup") {
                                pickupText = place.address
                            } else {
                                destinationText = place.address
                            }
                            onSelectAddresses(pickupText, destinationText)
                            onDismiss()
                        }
                    )
                }

                // Popular Landmarks
                items(popularLocations) { loc ->
                    val icon = if (loc.first.contains("Airport", ignoreCase = true)) {
                        Icons.Default.Flight
                    } else {
                        Icons.Default.LocationOn
                    }
                    LocationItemRow(
                        title = loc.first,
                        subtitle = loc.second,
                        icon = icon,
                        onClick = {
                            if (activeSearchTarget == "pickup") {
                                pickupText = loc.first
                            } else {
                                destinationText = loc.first
                            }
                            onSelectAddresses(pickupText, destinationText)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LocationItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
