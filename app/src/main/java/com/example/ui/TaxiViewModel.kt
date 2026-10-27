package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TaxiRepository
import com.example.data.model.DriverBid
import com.example.data.model.DriverFeedRequest
import com.example.data.model.DriverMessage
import com.example.data.model.PaymentMethod
import com.example.data.model.Receipt
import com.example.data.model.Ride
import com.example.data.model.RideTierOption
import com.example.data.model.SavedPlace
import com.example.data.model.UserProfile
import com.example.util.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaxiViewModel(
    private val repository: TaxiRepository,
    val currentUserId: String
) : ViewModel() {

    // Preset Ride Tiers
    val rideTiers = listOf(
        RideTierOption(
            id = "economy",
            name = "TaxiGo Economy",
            description = "Affordable, quick rides • Everyday sedans",
            basePrice = 14.00,
            multiplier = 1.0,
            capacity = 4,
            etaMins = 2,
            vehicleBadge = "Toyota Prius / Camry"
        ),
        RideTierOption(
            id = "comfort",
            name = "Comfort Plus",
            description = "Extra legroom • Top rated drivers",
            basePrice = 18.00,
            multiplier = 1.35,
            capacity = 4,
            etaMins = 3,
            vehicleBadge = "Honda Accord / Camry"
        ),
        RideTierOption(
            id = "green",
            name = "Green Electric",
            description = "100% Zero Emissions • Quiet EV ride",
            basePrice = 17.00,
            multiplier = 1.25,
            capacity = 4,
            etaMins = 4,
            vehicleBadge = "Tesla Model 3 / Y"
        ),
        RideTierOption(
            id = "xl",
            name = "TaxiGo XL",
            description = "Spacious 6-passenger SUV / Minivan",
            basePrice = 25.00,
            multiplier = 1.85,
            capacity = 6,
            etaMins = 5,
            vehicleBadge = "Honda Odyssey / Highlander"
        ),
        RideTierOption(
            id = "executive",
            name = "Executive Black",
            description = "Premium luxury fleet • Professional chauffeurs",
            basePrice = 35.00,
            multiplier = 2.50,
            capacity = 4,
            etaMins = 6,
            vehicleBadge = "BMW 5-Series / Mercedes E"
        )
    )

    // User Profile Stream
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Ride Stream
    val activeRide: StateFlow<Ride?> = repository.observeActiveRide(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Recent Rides History
    val recentRides: StateFlow<List<Ride>> = repository.observeRecentRides(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payment Methods Stream
    val paymentMethods: StateFlow<List<PaymentMethod>> = repository.observePaymentMethods(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Receipts Stream
    val receipts: StateFlow<List<Receipt>> = repository.observeReceipts(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Places Stream
    val savedPlaces: StateFlow<List<SavedPlace>> = repository.observeSavedPlaces(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // inDrive Role: "passenger" or "driver"
    private val _currentRole = MutableStateFlow("passenger")
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _showRoleDialog = MutableStateFlow(true)
    val showRoleDialog: StateFlow<Boolean> = _showRoleDialog.asStateFlow()

    // Real Location (GPS) State
    private val _userLat = MutableStateFlow(LocationService.DEFAULT_LAT)
    val userLat: StateFlow<Double> = _userLat.asStateFlow()

    private val _userLng = MutableStateFlow(LocationService.DEFAULT_LNG)
    val userLng: StateFlow<Double> = _userLng.asStateFlow()

    // UI Interactive State
    private val _selectedTier = MutableStateFlow(rideTiers[0])
    val selectedTier: StateFlow<RideTierOption> = _selectedTier.asStateFlow()

    private val _pickupAddress = MutableStateFlow(LocationService.DEFAULT_ADDRESS)
    val pickupAddress: StateFlow<String> = _pickupAddress.asStateFlow()

    private val _destinationAddress = MutableStateFlow("SFO International Airport, Terminal 2")
    val destinationAddress: StateFlow<String> = _destinationAddress.asStateFlow()

    // inDrive Fare Bidding State
    private val _offeredFare = MutableStateFlow(16.0)
    val offeredFare: StateFlow<Double> = _offeredFare.asStateFlow()

    private val _driverBids = MutableStateFlow<List<DriverBid>>(emptyList())
    val driverBids: StateFlow<List<DriverBid>> = _driverBids.asStateFlow()

    private val _showBidsSheet = MutableStateFlow(false)
    val showBidsSheet: StateFlow<Boolean> = _showBidsSheet.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow("card")
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    private val _selectedTip = MutableStateFlow(3.0)
    val selectedTip: StateFlow<Double> = _selectedTip.asStateFlow()

    // Real-Time Simulation Telemetry State
    private val _driverProgress = MutableStateFlow(0.15f)
    val driverProgress: StateFlow<Float> = _driverProgress.asStateFlow()

    private val _driverSpeedKmh = MutableStateFlow(42.0)
    val driverSpeedKmh: StateFlow<Double> = _driverSpeedKmh.asStateFlow()

    private val _currentRoad = MutableStateFlow("Market Boulevard")
    val currentRoad: StateFlow<String> = _currentRoad.asStateFlow()

    private val _isBooking = MutableStateFlow(false)
    val isBooking: StateFlow<Boolean> = _isBooking.asStateFlow()

    // Driver Mode State
    private val _isDriverOnline = MutableStateFlow(true)
    val isDriverOnline: StateFlow<Boolean> = _isDriverOnline.asStateFlow()

    private val _driverFeedRequests = MutableStateFlow<List<DriverFeedRequest>>(emptyList())
    val driverFeedRequests: StateFlow<List<DriverFeedRequest>> = _driverFeedRequests.asStateFlow()

    // Dialog & Sheet Toggles
    private val _showAddressSearchSheet = MutableStateFlow(false)
    val showAddressSearchSheet: StateFlow<Boolean> = _showAddressSearchSheet.asStateFlow()

    private val _showPaymentSheet = MutableStateFlow(false)
    val showPaymentSheet: StateFlow<Boolean> = _showPaymentSheet.asStateFlow()

    private val _showAddCardDialog = MutableStateFlow(false)
    val showAddCardDialog: StateFlow<Boolean> = _showAddCardDialog.asStateFlow()

    private val _showTopUpDialog = MutableStateFlow(false)
    val showTopUpDialog: StateFlow<Boolean> = _showTopUpDialog.asStateFlow()

    private val _showDriverChat = MutableStateFlow(false)
    val showDriverChat: StateFlow<Boolean> = _showDriverChat.asStateFlow()

    private val _showCallModal = MutableStateFlow(false)
    val showCallModal: StateFlow<Boolean> = _showCallModal.asStateFlow()

    private val _selectedReceiptForModal = MutableStateFlow<Receipt?>(null)
    val selectedReceiptForModal: StateFlow<Receipt?> = _selectedReceiptForModal.asStateFlow()

    private val _driverMessages = MutableStateFlow<List<DriverMessage>>(emptyList())
    val driverMessages: StateFlow<List<DriverMessage>> = _driverMessages.asStateFlow()

    private var simulationJob: Job? = null
    private var bidsJob: Job? = null

    init {
        initializeDefaultsIfNeeded()
    }

    private fun initializeDefaultsIfNeeded() {
        viewModelScope.launch {
            try {
                // Ensure profile exists in Firestore
                val profile = repository.getOrCreateUserProfile(currentUserId, "Alex Morgan", "alex.morgan@taxigo.app")
                _currentRole.value = profile.role
                _isDriverOnline.value = profile.isOnline

                // Seed driver incoming requests feed
                populateDriverFeed()

                delay(800)
                if (paymentMethods.value.isEmpty()) {
                    repository.addPaymentMethod(
                        PaymentMethod(
                            userId = currentUserId,
                            type = "card",
                            cardBrand = "visa",
                            lastFour = "4242",
                            expiryMonth = "09",
                            expiryYear = "28",
                            cardholderName = "Alex Morgan",
                            isDefault = true
                        )
                    )
                }
                if (savedPlaces.value.isEmpty()) {
                    repository.savePlace(
                        SavedPlace(
                            userId = currentUserId,
                            title = "Home",
                            address = "742 Evergreen Terrace",
                            lat = 37.7749,
                            lng = -122.4194,
                            iconType = "home"
                        )
                    )
                    repository.savePlace(
                        SavedPlace(
                            userId = currentUserId,
                            title = "Work",
                            address = "100 Montgomery St, Financial District",
                            lat = 37.7891,
                            lng = -122.4014,
                            iconType = "work"
                        )
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun populateDriverFeed() {
        _driverFeedRequests.value = listOf(
            DriverFeedRequest(
                id = "req_1",
                passengerName = "Sophia Chen",
                passengerRating = 4.96,
                pickupAddress = "550 Market St, Downtown",
                destinationAddress = "SFO International Airport, Terminal 2",
                distanceKm = 14.8,
                offeredFare = 18.00,
                timeAgo = "1 min ago"
            ),
            DriverFeedRequest(
                id = "req_2",
                passengerName = "James Wilson",
                passengerRating = 4.88,
                pickupAddress = "Fisherman's Wharf Pier 39",
                destinationAddress = "Palace of Fine Arts, Marina",
                distanceKm = 4.2,
                offeredFare = 12.00,
                timeAgo = "2 mins ago"
            ),
            DriverFeedRequest(
                id = "req_3",
                passengerName = "Emma Watson",
                passengerRating = 4.99,
                pickupAddress = "Ferry Building Marketplace",
                destinationAddress = "Mission Dolores Park",
                distanceKm = 6.5,
                offeredFare = 15.00,
                timeAgo = "Just now"
            )
        )
    }

    // --- REAL GPS INTEGRATION ---
    fun fetchRealLocation(context: Context) {
        viewModelScope.launch {
            val loc = LocationService.getCurrentLocation(context)
            _userLat.value = loc.lat
            _userLng.value = loc.lng
            _pickupAddress.value = loc.address
        }
    }

    // --- inDrive ROLE SWITCHING ---
    fun setRole(role: String) {
        _currentRole.value = role
        viewModelScope.launch {
            try {
                repository.updateUserRole(role)
            } catch (_: Exception) { }
        }
    }

    fun toggleRoleDialog(show: Boolean) {
        _showRoleDialog.value = show
    }

    // --- PASSENGER FARE OFFERING (inDrive Style) ---
    fun setOfferedFare(amount: Double) {
        _offeredFare.value = amount.coerceAtLeast(5.0)
    }

    fun boostOfferedFare(delta: Double) {
        _offeredFare.value = (_offeredFare.value + delta).coerceAtLeast(5.0)
    }

    fun requestRideWithOffer() {
        if (_isBooking.value) return
        _isBooking.value = true
        _showBidsSheet.value = true
        _driverBids.value = emptyList()

        viewModelScope.launch {
            try {
                val tier = _selectedTier.value
                val fare = _offeredFare.value
                val rideId = "ride_${System.currentTimeMillis()}"

                val newRide = Ride(
                    id = rideId,
                    userId = currentUserId,
                    pickupAddress = _pickupAddress.value,
                    pickupLat = _userLat.value,
                    pickupLng = _userLng.value,
                    destinationAddress = _destinationAddress.value,
                    destinationLat = 37.6213,
                    destinationLng = -122.3790,
                    rideTier = tier.id,
                    fare = fare,
                    offeredFare = fare,
                    tip = _selectedTip.value,
                    paymentMethod = _selectedPaymentMethod.value,
                    status = "SEARCHING"
                )

                repository.bookRide(newRide)
                _isBooking.value = false

                // Start simulated incoming driver bids
                listenForDriverBids(rideId, fare)
            } catch (_: Exception) {
                _isBooking.value = false
            }
        }
    }

    private fun listenForDriverBids(rideId: String, currentOffer: Double) {
        bidsJob?.cancel()
        bidsJob = viewModelScope.launch {
            // First bid arrives in 1.8s: Driver matches user fare!
            delay(1800)
            val bid1 = DriverBid(
                driverId = "drv_marcus",
                driverName = "Marcus Vance",
                driverRating = 4.98,
                vehicle = "Tesla Model Y • Midnight Silver",
                etaMinutes = 2,
                offeredPrice = currentOffer,
                isCounterOffer = false
            )
            _driverBids.value = listOf(bid1)

            // Second bid arrives in 2.5s: Counter offer +$2 with high rating
            delay(2200)
            val bid2 = DriverBid(
                driverId = "drv_david",
                driverName = "David Miller",
                driverRating = 4.95,
                vehicle = "Honda Accord • Crystal Black",
                etaMinutes = 3,
                offeredPrice = currentOffer + 2.0,
                isCounterOffer = true
            )
            _driverBids.value = _driverBids.value + bid2

            // Third bid arrives in 2.0s: Counter offer +$1 with 1 min ETA
            delay(2000)
            val bid3 = DriverBid(
                driverId = "drv_elena",
                driverName = "Elena Rostova",
                driverRating = 4.92,
                vehicle = "Toyota Camry • Pearl White",
                etaMinutes = 1,
                offeredPrice = currentOffer + 1.0,
                isCounterOffer = true
            )
            _driverBids.value = _driverBids.value + bid3
        }
    }

    fun acceptDriverBid(bid: DriverBid) {
        bidsJob?.cancel()
        _showBidsSheet.value = false

        val ride = activeRide.value ?: return
        viewModelScope.launch {
            repository.updateRide(
                ride.id,
                mapOf(
                    "status" to "DRIVER_ASSIGNED",
                    "fare" to bid.offeredPrice,
                    "driverId" to bid.driverId,
                    "driverName" to bid.driverName,
                    "driverRating" to bid.driverRating,
                    "carModel" to bid.vehicle,
                    "carColor" to "Metallic",
                    "licensePlate" to "TX-${(100..999).random()}-GO",
                    "etaMinutes" to bid.etaMinutes.toLong(),
                    "speedKmh" to 44.0
                )
            )

            // Start driver telemetry progression to pickup
            startDriverSimulation(ride.id, bid.offeredPrice, bid.driverName, bid.vehicle)
        }
    }

    private fun startDriverSimulation(rideId: String, agreedFare: Double, driverName: String, carModel: String) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _driverProgress.value = 0.05f
            _driverSpeedKmh.value = 45.0
            _currentRoad.value = "Driver Matched • En Route to Pickup"
            _driverMessages.value = listOf(
                DriverMessage(
                    id = "msg_1",
                    sender = "driver",
                    text = "Hello! I accepted your fare of $${agreedFare.toInt()}. On my way!",
                    timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                )
            )

            // Step 2: EN_ROUTE_PICKUP
            delay(2000)
            repository.updateRide(rideId, mapOf("status" to "EN_ROUTE_PICKUP", "etaMinutes" to 2L))

            for (p in 10..34 step 3) {
                _driverProgress.value = p / 100f
                _driverSpeedKmh.value = (40..52).random().toDouble()
                _currentRoad.value = "Market St approaching Pickup"
                delay(600)
            }

            // Step 3: ARRIVED
            _driverProgress.value = 0.35f
            _driverSpeedKmh.value = 0.0
            _currentRoad.value = "Arrived at Pickup • Waiting outside"
            repository.updateRide(rideId, mapOf("status" to "ARRIVED", "etaMinutes" to 0L, "speedKmh" to 0.0))
            _driverMessages.value = _driverMessages.value + DriverMessage(
                id = "msg_2",
                sender = "driver",
                text = "I have arrived at the pickup location. Hazards are on!",
                timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            )
            delay(4000)

            // Step 4: IN_TRIP
            repository.updateRide(rideId, mapOf("status" to "IN_TRIP", "etaMinutes" to 14L, "speedKmh" to 58.0))

            val roads = listOf("Embarcadero Freeway", "Highway 101 South", "Airport Expressway", "Terminal Concourse")
            for (p in 36..100 step 4) {
                _driverProgress.value = p / 100f
                _driverSpeedKmh.value = (52..65).random().toDouble()
                val roadIdx = ((p - 35) / 16).coerceIn(0, roads.lastIndex)
                _currentRoad.value = roads[roadIdx]
                val remainingMins = ((100 - p) / 6).coerceAtLeast(1)
                if (p % 20 == 0) {
                    repository.updateRide(rideId, mapOf("etaMinutes" to remainingMins.toLong(), "speedKmh" to _driverSpeedKmh.value))
                }
                delay(700)
            }

            // Step 5: COMPLETED
            _driverProgress.value = 1.0f
            _driverSpeedKmh.value = 0.0
            _currentRoad.value = "Trip Completed • Welcome to your destination!"
            repository.updateRide(rideId, mapOf("status" to "COMPLETED", "etaMinutes" to 0L, "speedKmh" to 0.0))

            // Generate receipt
            val finalTip = _selectedTip.value
            val totalPaid = agreedFare + finalTip
            val receipt = Receipt(
                userId = currentUserId,
                rideId = rideId,
                pickupAddress = _pickupAddress.value,
                destinationAddress = _destinationAddress.value,
                rideTier = "inDrive Negotiated",
                baseFare = 3.50,
                distanceFare = (agreedFare - 5.00).coerceAtLeast(3.0),
                serviceFee = 1.50,
                tip = finalTip,
                discount = 0.0,
                totalPaid = totalPaid,
                paymentMethodType = _selectedPaymentMethod.value,
                paymentLastFour = "4242",
                transactionId = "TXN-${UUID.randomUUID().toString().take(12).uppercase()}",
                driverName = driverName,
                carModel = carModel,
                distanceKm = 14.8,
                durationMinutes = 22L
            )
            repository.createReceipt(receipt)

            if (_selectedPaymentMethod.value == "wallet") {
                repository.updateWalletBalance(-totalPaid)
            }
        }
    }

    // --- DRIVER MODE ACTIONS ---
    fun toggleDriverOnline() {
        val newState = !_isDriverOnline.value
        _isDriverOnline.value = newState
        viewModelScope.launch {
            try {
                repository.updateDriverOnlineState(newState)
            } catch (_: Exception) { }
        }
    }

    fun driverAcceptRequest(request: DriverFeedRequest, fare: Double) {
        viewModelScope.launch {
            val rideId = "ride_drv_${System.currentTimeMillis()}"
            val newRide = Ride(
                id = rideId,
                userId = currentUserId,
                pickupAddress = request.pickupAddress,
                destinationAddress = request.destinationAddress,
                fare = fare,
                offeredFare = request.offeredFare,
                status = "DRIVER_ASSIGNED",
                driverId = currentUserId,
                driverName = userProfile.value?.displayName ?: "Marcus Vance",
                driverRating = 4.98,
                carModel = userProfile.value?.driverVehicle ?: "Tesla Model Y",
                passengerName = request.passengerName,
                passengerRating = request.passengerRating,
                etaMinutes = 3L,
                speedKmh = 42.0
            )
            repository.bookRide(newRide)

            // Remove from incoming feed
            _driverFeedRequests.value = _driverFeedRequests.value.filter { it.id != request.id }
        }
    }

    fun driverDeclineRequest(requestId: String) {
        _driverFeedRequests.value = _driverFeedRequests.value.filter { it.id != requestId }
    }

    fun driverArrivedAtPickup() {
        val ride = activeRide.value ?: return
        viewModelScope.launch {
            repository.updateRide(ride.id, mapOf("status" to "ARRIVED", "etaMinutes" to 0L, "speedKmh" to 0.0))
        }
    }

    fun driverStartTrip() {
        val ride = activeRide.value ?: return
        viewModelScope.launch {
            repository.updateRide(ride.id, mapOf("status" to "IN_TRIP", "etaMinutes" to 15L, "speedKmh" to 55.0))
        }
    }

    fun driverCompleteTrip() {
        val ride = activeRide.value ?: return
        viewModelScope.launch {
            repository.updateRide(ride.id, mapOf("status" to "COMPLETED", "etaMinutes" to 0L, "speedKmh" to 0.0))
            repository.updateDriverEarnings(ride.fare)
            populateDriverFeed()
        }
    }

    // --- PASSENGER COMMON ACTIONS ---
    fun cancelActiveRide() {
        val ride = activeRide.value ?: return
        simulationJob?.cancel()
        bidsJob?.cancel()
        _showBidsSheet.value = false
        viewModelScope.launch {
            repository.cancelRide(ride.id)
            _driverProgress.value = 0.15f
            _driverSpeedKmh.value = 0.0
            _currentRoad.value = "Ride Cancelled"
        }
    }

    fun skipToArrived() {
        val ride = activeRide.value ?: return
        simulationJob?.cancel()
        viewModelScope.launch {
            _driverProgress.value = 0.35f
            _driverSpeedKmh.value = 0.0
            _currentRoad.value = "Arrived at Pickup"
            repository.updateRide(ride.id, mapOf("status" to "ARRIVED", "etaMinutes" to 0L, "speedKmh" to 0.0))
        }
    }

    fun skipToCompleted() {
        val ride = activeRide.value ?: return
        simulationJob?.cancel()
        viewModelScope.launch {
            _driverProgress.value = 1.0f
            _driverSpeedKmh.value = 0.0
            _currentRoad.value = "Trip Completed"
            repository.updateRide(ride.id, mapOf("status" to "COMPLETED", "etaMinutes" to 0L, "speedKmh" to 0.0))

            val receipt = Receipt(
                userId = currentUserId,
                rideId = ride.id,
                pickupAddress = ride.pickupAddress,
                destinationAddress = ride.destinationAddress,
                rideTier = "inDrive Negotiated",
                baseFare = 3.50,
                distanceFare = (ride.fare - 5.00).coerceAtLeast(3.0),
                serviceFee = 1.50,
                tip = _selectedTip.value,
                discount = 0.0,
                totalPaid = ride.fare + _selectedTip.value,
                paymentMethodType = ride.paymentMethod,
                paymentLastFour = "4242",
                transactionId = "TXN-${UUID.randomUUID().toString().take(12).uppercase()}",
                driverName = ride.driverName.ifBlank { "Marcus Vance" },
                carModel = ride.carModel.ifBlank { "Tesla Model Y" },
                distanceKm = 14.8,
                durationMinutes = 22L
            )
            repository.createReceipt(receipt)
        }
    }

    fun submitRating(stars: Double) {
        val ride = activeRide.value ?: return
        viewModelScope.launch {
            repository.updateRide(ride.id, mapOf("ratingGiven" to stars))
        }
    }

    fun sendDriverMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = DriverMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = if (_currentRole.value == "passenger") "user" else "driver",
            text = text,
            timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        )
        _driverMessages.value = _driverMessages.value + userMsg

        viewModelScope.launch {
            delay(1400)
            val replyText = when {
                text.contains("where", ignoreCase = true) -> "Just turning the corner onto your street!"
                text.contains("outside", ignoreCase = true) || text.contains("here", ignoreCase = true) -> "Great, pulling up right in front of you!"
                else -> "Received, thank you!"
            }
            val replyMsg = DriverMessage(
                id = "msg_${System.currentTimeMillis()}_rep",
                sender = if (_currentRole.value == "passenger") "driver" else "user",
                text = replyText,
                timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            )
            _driverMessages.value = _driverMessages.value + replyMsg
        }
    }

    // --- PAYMENT PROCESSING ---
    fun addNewCard(cardNumber: String, holderName: String, expiry: String, brand: String) {
        viewModelScope.launch {
            val cleanNumber = cardNumber.filter { it.isDigit() }
            val lastFour = if (cleanNumber.length >= 4) cleanNumber.takeLast(4) else "4242"
            val parts = expiry.split("/")
            val month = parts.getOrNull(0)?.trim() ?: "12"
            val year = parts.getOrNull(1)?.trim() ?: "28"

            val newPm = PaymentMethod(
                userId = currentUserId,
                type = "card",
                cardBrand = brand.lowercase(),
                lastFour = lastFour,
                expiryMonth = month,
                expiryYear = year,
                cardholderName = holderName.ifBlank { "Cardholder" },
                isDefault = true
            )
            repository.addPaymentMethod(newPm)
            _selectedPaymentMethod.value = "card"
            _showAddCardDialog.value = false
        }
    }

    fun topUpWallet(amount: Double) {
        viewModelScope.launch {
            repository.updateWalletBalance(amount)
            _showTopUpDialog.value = false
        }
    }

    fun deletePaymentMethod(pmId: String) {
        viewModelScope.launch {
            repository.deletePaymentMethod(pmId)
        }
    }

    // --- UI MODAL TOGGLES ---
    fun selectTier(tier: RideTierOption) {
        _selectedTier.value = tier
        _offeredFare.value = tier.basePrice
    }

    fun setPickup(address: String) { _pickupAddress.value = address }
    fun setDestination(address: String) { _destinationAddress.value = address }
    fun setPaymentMethod(method: String) { _selectedPaymentMethod.value = method }
    fun setTip(amount: Double) { _selectedTip.value = amount }

    fun toggleAddressSearchSheet(show: Boolean) { _showAddressSearchSheet.value = show }
    fun togglePaymentSheet(show: Boolean) { _showPaymentSheet.value = show }
    fun toggleAddCardDialog(show: Boolean) { _showAddCardDialog.value = show }
    fun toggleTopUpDialog(show: Boolean) { _showTopUpDialog.value = show }
    fun toggleDriverChat(show: Boolean) { _showDriverChat.value = show }
    fun toggleCallModal(show: Boolean) { _showCallModal.value = show }
    fun showReceiptDetails(receipt: Receipt?) { _selectedReceiptForModal.value = receipt }
}
