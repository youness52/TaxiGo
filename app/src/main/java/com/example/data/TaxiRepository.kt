package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.Receipt
import com.example.data.model.Ride
import com.example.data.model.SavedPlace
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TaxiRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing taxi services.")
    }

    // --- USER PROFILE ---
    suspend fun getOrCreateUserProfile(uid: String, displayName: String, email: String): UserProfile {
        val userDoc = db.collection("users").document(uid)
        val snapshot = try {
            userDoc.get().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userDoc.path)
            throw e
        }

        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, displayName = displayName, email = email)
        } else {
            val newUser = UserProfile(
                id = uid,
                userId = uid,
                displayName = displayName.ifBlank { "Passenger" },
                email = email,
                phoneNumber = "",
                walletBalance = 50.0,
                rating = 5.0,
                totalRides = 0L,
                createdAt = null,
                updatedAt = null
            )
            val data = mapOf(
                "userId" to uid,
                "displayName" to newUser.displayName,
                "email" to newUser.email,
                "phoneNumber" to newUser.phoneNumber,
                "walletBalance" to newUser.walletBalance,
                "rating" to newUser.rating,
                "totalRides" to newUser.totalRides,
                "createdAt" to FieldValue.serverTimestamp()
            )
            try {
                userDoc.set(data).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, userDoc.path)
                throw e
            }
            return newUser
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val path = "users/$uid"
        return db.collection("users").document(uid).snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    suspend fun updateWalletBalance(amountDelta: Double) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snapshot = docRef.get().await()
            val current = (snapshot.getDouble("walletBalance") ?: 0.0)
            val updated = (current + amountDelta).coerceAtLeast(0.0)
            docRef.update(
                mapOf(
                    "walletBalance" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserRole(role: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "role" to role,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDriverOnlineState(isOnline: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateDriverEarnings(amountEarned: Double) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snapshot = docRef.get().await()
            val currentEarnings = snapshot.getDouble("driverEarnings") ?: 0.0
            val currentTrips = snapshot.getLong("driverTrips") ?: 0L
            docRef.update(
                mapOf(
                    "driverEarnings" to (currentEarnings + amountEarned),
                    "driverTrips" to (currentTrips + 1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- RIDES ---
    fun observeActiveRide(uid: String): Flow<Ride?> {
        val path = "users/$uid/rides"
        return db.collection("users").document(uid).collection("rides")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snap ->
                snap.documents.firstOrNull()?.let { doc ->
                    val ride = doc.toObject(Ride::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    // Return ride if not completed or cancelled, or recently finished
                    ride
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeRecentRides(uid: String): Flow<List<Ride>> {
        val path = "users/$uid/rides"
        return db.collection("users").document(uid).collection("rides")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snap ->
                snap.toObjects(Ride::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun bookRide(ride: Ride): String {
        val uid = requireUserId()
        val rideId = if (ride.id.isNotBlank()) ride.id else "ride_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users").document(uid).collection("rides").document(rideId)

        val payload = mapOf(
            "userId" to uid,
            "pickupAddress" to ride.pickupAddress,
            "pickupLat" to ride.pickupLat,
            "pickupLng" to ride.pickupLng,
            "destinationAddress" to ride.destinationAddress,
            "destinationLat" to ride.destinationLat,
            "destinationLng" to ride.destinationLng,
            "rideTier" to ride.rideTier,
            "fare" to ride.fare,
            "tip" to ride.tip,
            "paymentMethod" to ride.paymentMethod,
            "status" to ride.status,
            "driverName" to ride.driverName,
            "driverRating" to ride.driverRating,
            "driverPhone" to ride.driverPhone,
            "carModel" to ride.carModel,
            "carColor" to ride.carColor,
            "licensePlate" to ride.licensePlate,
            "driverLat" to ride.driverLat,
            "driverLng" to ride.driverLng,
            "etaMinutes" to ride.etaMinutes,
            "speedKmh" to ride.speedKmh,
            "ratingGiven" to ride.ratingGiven,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
            return rideId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateRide(rideId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("rides").document(rideId)
        val safeUpdates = updates + mapOf("updatedAt" to FieldValue.serverTimestamp())

        try {
            docRef.update(safeUpdates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun cancelRide(rideId: String) {
        updateRide(rideId, mapOf("status" to "CANCELLED"))
    }

    // --- PAYMENT METHODS ---
    fun observePaymentMethods(uid: String): Flow<List<PaymentMethod>> {
        val path = "users/$uid/paymentMethods"
        return db.collection("users").document(uid).collection("paymentMethods")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(PaymentMethod::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addPaymentMethod(pm: PaymentMethod): String {
        val uid = requireUserId()
        val pmId = if (pm.id.isNotBlank()) pm.id else "pm_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users").document(uid).collection("paymentMethods").document(pmId)

        val payload = mapOf(
            "userId" to uid,
            "type" to pm.type,
            "cardBrand" to pm.cardBrand,
            "lastFour" to pm.lastFour,
            "expiryMonth" to pm.expiryMonth,
            "expiryYear" to pm.expiryYear,
            "cardholderName" to pm.cardholderName,
            "isDefault" to pm.isDefault,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return pmId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePaymentMethod(pmId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("paymentMethods").document(pmId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- RECEIPTS ---
    fun observeReceipts(uid: String): Flow<List<Receipt>> {
        val path = "users/$uid/receipts"
        return db.collection("users").document(uid).collection("receipts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(Receipt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun createReceipt(receipt: Receipt): String {
        val uid = requireUserId()
        val receiptId = if (receipt.id.isNotBlank()) receipt.id else "rcpt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users").document(uid).collection("receipts").document(receiptId)

        val payload = mapOf(
            "userId" to uid,
            "rideId" to receipt.rideId,
            "pickupAddress" to receipt.pickupAddress,
            "destinationAddress" to receipt.destinationAddress,
            "rideTier" to receipt.rideTier,
            "baseFare" to receipt.baseFare,
            "distanceFare" to receipt.distanceFare,
            "serviceFee" to receipt.serviceFee,
            "tip" to receipt.tip,
            "discount" to receipt.discount,
            "totalPaid" to receipt.totalPaid,
            "paymentMethodType" to receipt.paymentMethodType,
            "paymentLastFour" to receipt.paymentLastFour,
            "transactionId" to receipt.transactionId,
            "driverName" to receipt.driverName,
            "carModel" to receipt.carModel,
            "distanceKm" to receipt.distanceKm,
            "durationMinutes" to receipt.durationMinutes,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return receiptId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- SAVED PLACES ---
    fun observeSavedPlaces(uid: String): Flow<List<SavedPlace>> {
        val path = "users/$uid/savedPlaces"
        return db.collection("users").document(uid).collection("savedPlaces")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(SavedPlace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun savePlace(place: SavedPlace): String {
        val uid = requireUserId()
        val placeId = if (place.id.isNotBlank()) place.id else "place_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users").document(uid).collection("savedPlaces").document(placeId)

        val payload = mapOf(
            "userId" to uid,
            "title" to place.title,
            "address" to place.address,
            "lat" to place.lat,
            "lng" to place.lng,
            "iconType" to place.iconType,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return placeId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
