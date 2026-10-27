package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val walletBalance: Double = 50.0,
    val rating: Double = 5.0,
    val totalRides: Long = 0L,
    val role: String = "passenger", // "passenger" or "driver"
    val isOnline: Boolean = false, // for driver
    val driverVehicle: String = "Tesla Model Y • Midnight Silver",
    val driverEarnings: Double = 142.50,
    val driverTrips: Long = 7L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Ride(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pickupAddress: String = "",
    val pickupLat: Double = 37.7749,
    val pickupLng: Double = -122.4194,
    val destinationAddress: String = "",
    val destinationLat: Double = 37.7891,
    val destinationLng: Double = -122.4014,
    val rideTier: String = "economy",
    val fare: Double = 0.0,
    val offeredFare: Double = 0.0, // inDrive user proposed fare
    val tip: Double = 0.0,
    val paymentMethod: String = "card",
    val status: String = "SEARCHING", // SEARCHING, BIDS_RECEIVED, DRIVER_ASSIGNED, EN_ROUTE_PICKUP, ARRIVED, IN_TRIP, COMPLETED, CANCELLED
    val driverId: String = "",
    val driverName: String = "",
    val driverRating: Double = 4.95,
    val driverPhone: String = "",
    val carModel: String = "",
    val carColor: String = "",
    val licensePlate: String = "",
    val passengerName: String = "Alex Morgan",
    val passengerRating: Double = 4.96,
    val driverLat: Double = 37.7749,
    val driverLng: Double = -122.4194,
    val etaMinutes: Long = 5L,
    val speedKmh: Double = 0.0,
    val ratingGiven: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PaymentMethod(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "card", // card, google_pay, wallet, cash
    val cardBrand: String = "visa", // visa, mastercard, amex, discover, other
    val lastFour: String = "4242",
    val expiryMonth: String = "12",
    val expiryYear: String = "28",
    val cardholderName: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Receipt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val rideId: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val rideTier: String = "economy",
    val baseFare: Double = 3.50,
    val distanceFare: Double = 12.00,
    val serviceFee: Double = 2.50,
    val tip: Double = 0.0,
    val discount: Double = 0.0,
    val totalPaid: Double = 18.00,
    val paymentMethodType: String = "card",
    val paymentLastFour: String = "4242",
    val transactionId: String = "",
    val driverName: String = "",
    val carModel: String = "",
    val distanceKm: Double = 8.4,
    val durationMinutes: Long = 18L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedPlace(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val iconType: String = "pin", // home, work, star, pin
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RideTierOption(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val multiplier: Double,
    val capacity: Int,
    val etaMins: Int,
    val vehicleBadge: String
)

data class RoutePoint(
    val lat: Double,
    val lng: Double
)

data class DriverMessage(
    val id: String,
    val sender: String, // "driver" or "user"
    val text: String,
    val timeFormatted: String
)

// InDrive Bidding & Driver Feed Models
data class DriverBid(
    val driverId: String,
    val driverName: String,
    val driverRating: Double,
    val vehicle: String,
    val etaMinutes: Int,
    val offeredPrice: Double,
    val isCounterOffer: Boolean
)

data class DriverFeedRequest(
    val id: String,
    val passengerName: String,
    val passengerRating: Double,
    val pickupAddress: String,
    val destinationAddress: String,
    val distanceKm: Double,
    val offeredFare: Double,
    val timeAgo: String
)
