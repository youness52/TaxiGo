package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val lat: Double,
    val lng: Double,
    val address: String,
    val city: String
)

object LocationService {

    const val DEFAULT_LAT = 37.7749
    const val DEFAULT_LNG = -122.4194
    const val DEFAULT_ADDRESS = "Market St, San Francisco, CA"

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationResult = withContext(Dispatchers.IO) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            val location: Location? = suspendCancellableCoroutine { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                cont.invokeOnCancellation { cts.cancel() }
            }

            if (location != null) {
                val geocoded = reverseGeocode(context, location.latitude, location.longitude)
                LocationResult(
                    lat = location.latitude,
                    lng = location.longitude,
                    address = geocoded.first,
                    city = geocoded.second
                )
            } else {
                LocationResult(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_ADDRESS, "San Francisco")
            }
        } catch (_: Exception) {
            LocationResult(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_ADDRESS, "San Francisco")
        }
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val fullAddress = addr.getAddressLine(0) ?: "${addr.featureName ?: ""} ${addr.thoroughfare ?: ""}".trim()
                val city = addr.locality ?: addr.subAdminArea ?: "San Francisco"
                Pair(fullAddress.ifBlank { "Current Location" }, city)
            } else {
                Pair("Current Location (${String.format(Locale.US, "%.3f, %.3f", lat, lng)})", "Nearby")
            }
        } catch (_: Exception) {
            Pair("Current Location (${String.format(Locale.US, "%.3f, %.3f", lat, lng)})", "Nearby")
        }
    }

    suspend fun searchAddress(context: Context, query: String): List<LocationResult> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(query, 5)
            addresses?.map { addr ->
                LocationResult(
                    lat = addr.latitude,
                    lng = addr.longitude,
                    address = addr.getAddressLine(0) ?: query,
                    city = addr.locality ?: ""
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
