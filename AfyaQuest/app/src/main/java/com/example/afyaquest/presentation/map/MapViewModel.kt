package com.example.afyaquest.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.afyaquest.domain.model.ClientHouse
import com.example.afyaquest.domain.model.FacilityType
import com.example.afyaquest.domain.model.HealthFacility
import com.example.afyaquest.domain.model.ItineraryStop
import com.example.afyaquest.domain.model.VisitStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Map screen.
 * Manages health facilities, client houses, daily itinerary (ordered stops),
 * and real-time device location via the Fused Location Provider.
 *
 * Live location is used to show "You are here" on the map and as the start point
 * of the route (device → first stop → … → last stop).
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        /**
         * Set to true to use real device GPS for "You are here".
         * Set to false to keep live marker at Guatemala default.
         * To reactivate actual live location: uncomment the line below and comment the line with false.
         */
        // private const val USE_REAL_DEVICE_LOCATION = true   // Uncomment this line to use real GPS
        private const val USE_REAL_DEVICE_LOCATION = false //comment this line to use real GPS
    }

    // -------------------------------------------------------------------------
    // Default map camera (Guatemala City area). Used when no location available.
    // -------------------------------------------------------------------------
    val defaultLatitude = 14.6349
    val defaultLongitude = -90.5069
    val defaultZoom = 12f

    // Default "You are here" in Guatemala (used when [USE_REAL_DEVICE_LOCATION] is false or before first GPS fix).
    private val defaultLiveLatitude = 14.6520
    private val defaultLiveLongitude = -90.5350

    // -------------------------------------------------------------------------
    // Live location (real GPS when [USE_REAL_DEVICE_LOCATION] is true, else Guatemala default).
    // -------------------------------------------------------------------------
    private val _liveLocationLatitude = MutableStateFlow(defaultLiveLatitude)
    val liveLocationLatitude: StateFlow<Double> = _liveLocationLatitude.asStateFlow()

    private val _liveLocationLongitude = MutableStateFlow(defaultLiveLongitude)
    val liveLocationLongitude: StateFlow<Double> = _liveLocationLongitude.asStateFlow()

    /**
     * Fused Location Provider client. Uses sensors, GPS, and network to get
     * accurate location with low power. Created from application context.
     */
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Callback for continuous location updates. Updates our StateFlows so the
     * map marker and route recompose with new coordinates.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _liveLocationLatitude.value = location.latitude
                _liveLocationLongitude.value = location.longitude
            }
        }
    }

    private val _healthFacilities = MutableStateFlow<List<HealthFacility>>(emptyList())
    val healthFacilities: StateFlow<List<HealthFacility>> = _healthFacilities.asStateFlow()

    private val _clientHouses = MutableStateFlow<List<ClientHouse>>(emptyList())
    val clientHouses: StateFlow<List<ClientHouse>> = _clientHouses.asStateFlow()

    /** Ordered list of stops for today's itinerary — "do them in this order" */
    private val _dailyItineraryStops = MutableStateFlow<List<ItineraryStop>>(emptyList())
    val dailyItineraryStops: StateFlow<List<ItineraryStop>> = _dailyItineraryStops.asStateFlow()

    private val _selectedClient = MutableStateFlow<ClientHouse?>(null)
    val selectedClient: StateFlow<ClientHouse?> = _selectedClient.asStateFlow()

    private val _statusFilter = MutableStateFlow<VisitStatus?>(null)
    val statusFilter: StateFlow<VisitStatus?> = _statusFilter.asStateFlow()

    init {
        loadMapData()
    }

    /**
     * Load health facilities, client houses, and today's ordered itinerary.
     * Includes both Kenya and Guatemala placeholder data. In production, fetch from API.
     */
    private fun loadMapData() {
        // —— Kenya: health facilities ——
        val kenyaHealthFacilities = listOf(
            HealthFacility(
                id = "k-hf1",
                name = "Kajiado Referral Hospital",
                type = FacilityType.HOSPITAL,
                latitude = -1.8522,
                longitude = 36.7820,
                servicesAvailable = listOf("Emergency", "Maternity", "Pediatrics", "Surgery", "Outpatient", "Laboratory"),
                distance = 20.3
            ),
            HealthFacility(
                id = "k-hf2",
                name = "AIC Kajiado Hospital",
                type = FacilityType.HOSPITAL,
                latitude = -1.8489,
                longitude = 36.7845,
                servicesAvailable = listOf("Emergency", "Maternity", "Pediatrics", "Outpatient", "X-Ray"),
                distance = 19.8
            ),
            HealthFacility(
                id = "k-hf3",
                name = "Kitengela Sub-County Hospital",
                type = FacilityType.HOSPITAL,
                latitude = -1.4737,
                longitude = 36.9532,
                servicesAvailable = listOf("Emergency", "Maternity", "Surgery", "Laboratory", "Pharmacy"),
                distance = 42.7
            ),
            HealthFacility(
                id = "k-hf4",
                name = "Kajiado Airport Dispensary",
                type = FacilityType.CLINIC,
                latitude = -1.8595,
                longitude = 36.9801,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Family Planning"),
                distance = 0.3
            )
        )

        // —— Kenya: client houses ——
        val kenyaClientHouses = listOf(
            ClientHouse(
                id = "k1",
                address = "123 Airport Road",
                clientName = "Selina Nkoya Family",
                latitude = -1.8623,
                longitude = 36.9789,
                status = VisitStatus.TO_VISIT,
                distance = 0.5,
                description = "Maternal health check-up needed"
            ),
            ClientHouse(
                id = "k2",
                address = "45 Bissil Road",
                clientName = "Ole Sankale Household",
                latitude = -1.8712,
                longitude = 36.9901,
                status = VisitStatus.TO_VISIT,
                distance = 1.8,
                description = "Child vaccination due"
            ),
            ClientHouse(
                id = "k3",
                address = "78 Mashuuru Village",
                clientName = "Grace Nasieku",
                latitude = -1.8753,
                longitude = 36.8201,
                status = VisitStatus.VISITED,
                lastVisit = "2 days ago",
                distance = 16.7,
                description = "Follow-up completed"
            ),
            ClientHouse(
                id = "k4",
                address = "22 Kajiado Town",
                clientName = "David Lekishon Family",
                latitude = -1.8512,
                longitude = 36.7798,
                status = VisitStatus.SCHEDULED,
                nextVisit = "Tomorrow 10:00 AM",
                distance = 20.1,
                description = "Family planning consultation"
            ),
            ClientHouse(
                id = "k5",
                address = "89 Oloosirkon Area",
                clientName = "Peter Kisemei Household",
                latitude = -1.7901,
                longitude = 36.9267,
                status = VisitStatus.TO_VISIT,
                distance = 9.7,
                description = "Hypertension screening needed"
            ),
            ClientHouse(
                id = "k6",
                address = "34 Magadi Road",
                clientName = "Mary Ole Sankale",
                latitude = -1.9012,
                longitude = 37.0123,
                status = VisitStatus.SCHEDULED,
                nextVisit = "Friday 2:00 PM",
                distance = 6.2,
                description = "Prenatal care appointment"
            )
        )

        // —— Guatemala: health facilities ——
        val guatemalaHealthFacilities = listOf(
            HealthFacility(
                id = "hf1",
                name = "Hospital General San Juan de Dios",
                type = FacilityType.HOSPITAL,
                latitude = 14.6289,
                longitude = -90.5132,
                servicesAvailable = listOf("Emergency", "Maternity", "Pediatrics", "Surgery", "Laboratory"),
                distance = 2.1
            ),
            HealthFacility(
                id = "hf2",
                name = "Centro de Salud Zona 1",
                type = FacilityType.CLINIC,
                latitude = 14.6412,
                longitude = -90.5189,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Family Planning"),
                distance = 1.8
            ),
            HealthFacility(
                id = "hf3",
                name = "Hospital Roosevelt",
                type = FacilityType.HOSPITAL,
                latitude = 14.6123,
                longitude = -90.4892,
                servicesAvailable = listOf("Emergency", "Maternity", "Outpatient", "X-Ray"),
                distance = 3.5
            )
        )

        // —— Guatemala: client houses ——
        val guatemalaClientHouses = listOf(
            ClientHouse(
                id = "1",
                address = "Zona 3, Guatemala City",
                clientName = "Familia Hernández",
                latitude = 14.6389,
                longitude = -90.5089,
                status = VisitStatus.TO_VISIT,
                distance = 0.6,
                description = "Maternal health check-up"
            ),
            ClientHouse(
                id = "2",
                address = "Zona 7, Guatemala City",
                clientName = "Casa López",
                latitude = 14.6222,
                longitude = -90.5212,
                status = VisitStatus.TO_VISIT,
                distance = 1.2,
                description = "Child vaccination due"
            ),
            ClientHouse(
                id = "3",
                address = "Zona 10, Guatemala City",
                clientName = "Familia García",
                latitude = 14.5989,
                longitude = -90.4956,
                status = VisitStatus.VISITED,
                lastVisit = "2 days ago",
                distance = 2.8,
                description = "Follow-up completed"
            ),
            ClientHouse(
                id = "4",
                address = "Zona 5, Mixco",
                clientName = "Casa Martínez",
                latitude = 14.6312,
                longitude = -90.5321,
                status = VisitStatus.SCHEDULED,
                nextVisit = "Tomorrow 10:00 AM",
                distance = 1.5,
                description = "Family planning consultation"
            ),
            ClientHouse(
                id = "5",
                address = "Zona 12, Guatemala City",
                clientName = "Familia Rodríguez",
                latitude = 14.6156,
                longitude = -90.4789,
                status = VisitStatus.TO_VISIT,
                distance = 2.3,
                description = "Hypertension screening"
            )
        )

        // Combined lists: Kenya + Guatemala (nothing removed)
        _healthFacilities.value = kenyaHealthFacilities + guatemalaHealthFacilities
        _clientHouses.value = kenyaClientHouses + guatemalaClientHouses

        // Today's itinerary — ordered stops (path for the map). Guatemala demo for map path.
        _dailyItineraryStops.value = listOf(
            ItineraryStop(order = 1, id = "1", label = "Familia Hernández", address = "Zona 3, Guatemala City", latitude = 14.6389, longitude = -90.5089, description = "Maternal health check-up"),
            ItineraryStop(order = 2, id = "2", label = "Casa López", address = "Zona 7, Guatemala City", latitude = 14.6222, longitude = -90.5212, description = "Child vaccination due"),
            ItineraryStop(order = 3, id = "hf2", label = "Centro de Salud Zona 1", address = "Zona 1", latitude = 14.6412, longitude = -90.5189, description = "Drop-off / pick-up supplies"),
            ItineraryStop(order = 4, id = "4", label = "Casa Martínez", address = "Zona 5, Mixco", latitude = 14.6312, longitude = -90.5321, description = "Family planning consultation"),
            ItineraryStop(order = 5, id = "5", label = "Familia Rodríguez", address = "Zona 12, Guatemala City", latitude = 14.6156, longitude = -90.4789, description = "Hypertension screening")
        )
    }

    // -------------------------------------------------------------------------
    // Location updates lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts receiving real-time location updates from the Fused Location Provider.
     * Call this from the UI after the user has granted ACCESS_FINE_LOCATION.
     *
     * - If [USE_REAL_DEVICE_LOCATION] is false, returns immediately (live marker stays at Guatemala default).
     * - If permission is not granted, this method does nothing (live location stays at default).
     * - Otherwise requests high-accuracy updates and fetches last known location for immediate feedback.
     */
    fun startLocationUpdates() {
        if (!USE_REAL_DEVICE_LOCATION) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // High-accuracy request: GPS preferred, updates every ~10s or when device moves 10m.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).apply {
            setMinUpdateIntervalMillis(5_000L)   // Fastest update interval (5s)
            setMaxUpdates(Int.MAX_VALUE)         // Receive updates until we call removeLocationUpdates
            setMinUpdateDistanceMeters(10f)      // Also update when device moves at least 10 metres
        }.build()
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _liveLocationLatitude.value = it.latitude
                _liveLocationLongitude.value = it.longitude
            }
        }
    }

    /** Stops location updates. Called from [onCleared] to avoid leaks. */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }


    // -------------------------------------------------------------------------
    // Route and itinerary helpers
    // -------------------------------------------------------------------------

    /** LatLng points for the itinerary only (stops in order). */
    fun getItineraryPathPoints(): List<Pair<Double, Double>> =
        _dailyItineraryStops.value.map { it.latitude to it.longitude }

    /**
     * Full route for the map: live location → stop 1 → stop 2 → …
     * Uses current real (or default) device position so the route starts at "you".
     */
    fun getFullRoutePoints(): List<Pair<Double, Double>> {
        val start = _liveLocationLatitude.value to _liveLocationLongitude.value
        val itinerary = getItineraryPathPoints()
        return listOf(start) + itinerary
    }

    /**
     * Get filtered client houses based on status
     */
    fun getFilteredClients(): List<ClientHouse> {
        val filter = _statusFilter.value
        return if (filter == null) {
            _clientHouses.value
        } else {
            _clientHouses.value.filter { it.status == filter }
        }
    }

    /**
     * Set status filter
     */
    fun setStatusFilter(status: VisitStatus?) {
        _statusFilter.value = status
    }

    /**
     * Select a client to show details
     */
    fun selectClient(client: ClientHouse?) {
        _selectedClient.value = client
    }

    /**
     * Mark client as visited
     */
    fun markClientAsVisited(clientId: String) {
        _clientHouses.value = _clientHouses.value.map { client ->
            if (client.id == clientId) {
                client.copy(
                    status = VisitStatus.VISITED,
                    lastVisit = "Just now"
                )
            } else {
                client
            }
        }
    }
}