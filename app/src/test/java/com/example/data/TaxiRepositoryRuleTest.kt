package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Ride
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class TaxiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun getOrCreateUserProfile_authenticatedUser_createsAndReturnsProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TaxiRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile(uid, "Alice Walker", ALICE_EMAIL)
        }

        assertNotNull(profile)
        assertEquals(uid, profile.userId)
        assertEquals("Alice Walker", profile.displayName)
    }

    @Test
    fun bookRide_authenticatedUser_createsRideInSubcollection() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TaxiRepository(firestore)

        val ride = Ride(
            id = "ride_${UUID.randomUUID().toString().take(8)}",
            userId = uid,
            pickupAddress = "500 Howard St, San Francisco",
            destinationAddress = "Palace of Fine Arts, SF",
            rideTier = "electric",
            fare = 24.50,
            paymentMethod = "wallet",
            status = "SEARCHING"
        )

        val rideId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.bookRide(ride)
        }

        assertTrue(rideId.isNotBlank())

        val recentRides = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRecentRides(uid).first { list -> list.any { it.id == rideId } }
        }
        assertTrue(recentRides.any { it.id == rideId })
    }

    @Test
    fun getBobRides_aliceUser_failsWithPermissionDenied() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = TaxiRepository(firestore)
        val bobRide = Ride(
            id = "ride_${UUID.randomUUID().toString().take(8)}",
            userId = bobUid,
            pickupAddress = "100 Pine St",
            destinationAddress = "Fisherman's Wharf",
            rideTier = "comfort",
            fare = 18.00,
            paymentMethod = "card",
            status = "SEARCHING"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.bookRide(bobRide)
        }

        // Switch to Alice and attempt to read Bob's rides
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = TaxiRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                aliceRepo.observeRecentRides(bobUid).first()
            }
            fail("Expected PERMISSION_DENIED when Alice attempts to access Bob's rides")
        } catch (e: Exception) {
            val rootException = e.unwrapFirestoreException()
            assertNotNull("Expected root FirebaseFirestoreException", rootException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, rootException?.code)
        }
    }

    @Test
    fun observeRides_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()

        val repository = TaxiRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeRecentRides(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated listener")
        } catch (e: Exception) {
            val rootException = e.unwrapFirestoreException()
            assertNotNull("Expected root FirebaseFirestoreException", rootException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, rootException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@taxigo.test"
        const val BOB_EMAIL = "bob@taxigo.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
