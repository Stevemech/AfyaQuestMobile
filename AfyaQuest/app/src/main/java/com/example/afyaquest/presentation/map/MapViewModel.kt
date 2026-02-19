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
import com.example.afyaquest.R
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
    // Default map camera (Kajiado district, Kenya). Used when no location available.
    // -------------------------------------------------------------------------
    val defaultLatitude = -1.93
    val defaultLongitude = 36.7820
    val defaultZoom = 12f

    // Default "You are here" in Kenya (used when [USE_REAL_DEVICE_LOCATION] is false or before first GPS fix).
    private val defaultLiveLatitude = -1.93
    private val defaultLiveLongitude = 36.9801

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
        // —— Kenya: health facilities (localized) ——
        val s = { resId: Int -> context.getString(resId) }
        val kenyaHealthFacilities = listOf(
            HealthFacility(
                id = "k-hf1",
                name = s(R.string.facility_kajiado_referral_hospital_name),
                type = FacilityType.HOSPITAL,
                latitude = -1.8522,
                longitude = 36.7820,
                servicesAvailable = listOf(s(R.string.service_emergency), s(R.string.service_maternity), s(R.string.service_pediatrics), s(R.string.service_surgery), s(R.string.service_outpatient), s(R.string.service_laboratory)),
                distance = 20.3
            ),
            HealthFacility(
                id = "k-hf2",
                name = s(R.string.facility_aic_kajiado_hospital_name),
                type = FacilityType.HOSPITAL,
                latitude = -1.8489,
                longitude = 36.7845,
                servicesAvailable = listOf(s(R.string.service_emergency), s(R.string.service_maternity), s(R.string.service_pediatrics), s(R.string.service_outpatient), s(R.string.service_xray)),
                distance = 19.8
            ),
            HealthFacility(
                id = "k-hf3",
                name = s(R.string.facility_kitengela_subcounty_hospital_name),
                type = FacilityType.HOSPITAL,
                latitude = -1.4737,
                longitude = 36.9532,
                servicesAvailable = listOf(s(R.string.service_emergency), s(R.string.service_maternity), s(R.string.service_surgery), s(R.string.service_laboratory), s(R.string.service_pharmacy)),
                distance = 42.7
            ),
            HealthFacility(
                id = "k-hf4",
                name = s(R.string.facility_kajiado_airport_dispensary_name),
                type = FacilityType.CLINIC,
                latitude = -1.8595,
                longitude = 36.9801,
                servicesAvailable = listOf(s(R.string.service_primary_care), s(R.string.service_vaccination), s(R.string.service_family_planning)),
                distance = 0.3
            )
        )

        // —— Kenya: client houses (localized) ——
        val kenyaClientHouses = listOf(
            ClientHouse(
                id = "k1",
                address = s(R.string.address_123_airport_road),
                clientName = s(R.string.client_selina_nkoya_family_name),
                latitude = -1.8623,
                longitude = 36.9789,
                status = VisitStatus.TO_VISIT,
                distance = 0.5,
                description = s(R.string.desc_maternal_health_checkup_needed)
            ),
            ClientHouse(
                id = "k2",
                address = s(R.string.address_45_bissil_road),
                clientName = s(R.string.client_ole_sankale_household_name),
                latitude = -1.8712,
                longitude = 36.9901,
                status = VisitStatus.TO_VISIT,
                distance = 1.8,
                description = s(R.string.desc_child_vaccination_due)
            ),
            ClientHouse(
                id = "k3",
                address = s(R.string.address_78_mashuuru_village),
                clientName = s(R.string.client_grace_nasieku_name),
                latitude = -1.8753,
                longitude = 36.8201,
                status = VisitStatus.VISITED,
                lastVisit = s(R.string.time_2_days_ago),
                distance = 16.7,
                description = s(R.string.desc_followup_completed)
            ),
            ClientHouse(
                id = "k4",
                address = s(R.string.address_22_kajiado_town),
                clientName = s(R.string.client_david_lekishon_family_name),
                latitude = -1.8512,
                longitude = 36.7798,
                status = VisitStatus.SCHEDULED,
                nextVisit = s(R.string.time_tomorrow_10_am),
                distance = 20.1,
                description = s(R.string.desc_family_planning_consultation)
            ),
            ClientHouse(
                id = "k5",
                address = s(R.string.address_89_oloosirkon_area),
                clientName = s(R.string.client_peter_kisemei_household_name),
                latitude = -1.7901,
                longitude = 36.9267,
                status = VisitStatus.TO_VISIT,
                distance = 9.7,
                description = s(R.string.desc_hypertension_screening_needed)
            ),
            ClientHouse(
                id = "k6",
                address = s(R.string.address_34_magadi_road),
                clientName = s(R.string.client_mary_ole_sankale_name),
                latitude = -1.9012,
                longitude = 37.0123,
                status = VisitStatus.SCHEDULED,
                nextVisit = s(R.string.time_friday_2_pm),
                distance = 6.2,
                description = s(R.string.desc_prenatal_care_appointment)
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

        // Today's itinerary — ordered stops (path for the map). Kenya demo (localized).
        _dailyItineraryStops.value = listOf(
            ItineraryStop(order = 1, id = "k1", label = s(R.string.client_selina_nkoya_family_name), address = s(R.string.address_123_airport_road), latitude = -1.8623, longitude = 36.9789, description = s(R.string.desc_maternal_health_checkup_needed)),
            ItineraryStop(order = 2, id = "k2", label = s(R.string.client_ole_sankale_household_name), address = s(R.string.address_45_bissil_road), latitude = -1.8712, longitude = 36.9901, description = s(R.string.desc_child_vaccination_due)),
            ItineraryStop(order = 3, id = "k-hf4", label = s(R.string.facility_kajiado_airport_dispensary_name), address = s(R.string.address_near_kajiado_airport), latitude = -1.8595, longitude = 36.9801, description = s(R.string.desc_dropoff_pickup_supplies)),
            ItineraryStop(order = 4, id = "k5", label = s(R.string.client_peter_kisemei_household_name), address = s(R.string.address_89_oloosirkon_area), latitude = -1.7901, longitude = 36.9267, description = s(R.string.desc_hypertension_screening_needed)),
            ItineraryStop(order = 5, id = "k-hf1", label = s(R.string.facility_kajiado_referral_hospital_name), address = s(R.string.address_kajiado_town), latitude = -1.8522, longitude = 36.7820, description = s(R.string.service_outpatient)),
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
                    lastVisit = context.getString(R.string.time_just_now)
                )
            } else {
                client
            }
        }
    }
}