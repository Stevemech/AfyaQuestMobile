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
    // Default map camera (Chimaltenango, Guatemala). Used when no location available.
    // -------------------------------------------------------------------------
    val defaultLatitude = 14.6392
    val defaultLongitude = -90.8208
    val defaultZoom = 13f

    // Default "You are here" in Guatemala (used when [USE_REAL_DEVICE_LOCATION] is false or before first GPS fix).
    private val defaultLiveLatitude = 14.6350
    private val defaultLiveLongitude = -90.8185

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
     * Uses Chimaltenango, Guatemala placeholder data. In production, fetch from API.
     */
    private fun loadMapData() {
        val s = { resId: Int -> context.getString(resId) }

        // —— Health facilities (Chimaltenango area, Guatemala) ——
        _healthFacilities.value = listOf(
            HealthFacility(
                id = "hf1",
                name = s(R.string.facility_hospital_nacional_chimaltenango_name),
                type = FacilityType.HOSPITAL,
                latitude = 14.6614,
                longitude = -90.8194,
                servicesAvailable = listOf(s(R.string.service_emergency), s(R.string.service_maternity), s(R.string.service_pediatrics), s(R.string.service_surgery), s(R.string.service_outpatient), s(R.string.service_laboratory)),
                distance = 0.8
            ),
            HealthFacility(
                id = "hf2",
                name = s(R.string.facility_centro_salud_chimaltenango_name),
                type = FacilityType.CLINIC,
                latitude = 14.6380,
                longitude = -90.8215,
                servicesAvailable = listOf(s(R.string.service_primary_care), s(R.string.service_vaccination), s(R.string.service_family_planning)),
                distance = 0.3
            ),
            HealthFacility(
                id = "hf3",
                name = s(R.string.facility_hospital_san_juan_comalapa_name),
                type = FacilityType.HOSPITAL,
                latitude = 14.7414,
                longitude = -90.8886,
                servicesAvailable = listOf(s(R.string.service_emergency), s(R.string.service_maternity), s(R.string.service_outpatient), s(R.string.service_xray)),
                distance = 12.5
            ),
            HealthFacility(
                id = "hf4",
                name = s(R.string.facility_puesto_salud_parramos_name),
                type = FacilityType.CLINIC,
                latitude = 14.6128,
                longitude = -90.8042,
                servicesAvailable = listOf(s(R.string.service_primary_care), s(R.string.service_vaccination), s(R.string.service_pharmacy)),
                distance = 3.2
            )
        )

        // —— Client houses (Chimaltenango area, Guatemala) ——
        _clientHouses.value = listOf(
            ClientHouse(
                id = "1",
                address = s(R.string.address_calle_real_chimaltenango),
                clientName = s(R.string.client_familia_hernandez_name),
                latitude = 14.6425,
                longitude = -90.8170,
                status = VisitStatus.TO_VISIT,
                distance = 0.5,
                description = s(R.string.desc_maternal_health_checkup_needed)
            ),
            ClientHouse(
                id = "2",
                address = s(R.string.address_canton_san_jacinto),
                clientName = s(R.string.client_casa_lopez_name),
                latitude = 14.6310,
                longitude = -90.8290,
                status = VisitStatus.TO_VISIT,
                distance = 1.2,
                description = s(R.string.desc_child_vaccination_due)
            ),
            ClientHouse(
                id = "3",
                address = s(R.string.address_aldea_san_andres_itzapa),
                clientName = s(R.string.client_familia_garcia_name),
                latitude = 14.6190,
                longitude = -90.8410,
                status = VisitStatus.VISITED,
                lastVisit = s(R.string.time_2_days_ago),
                distance = 3.1,
                description = s(R.string.desc_followup_completed)
            ),
            ClientHouse(
                id = "4",
                address = s(R.string.address_colonia_las_victorias),
                clientName = s(R.string.client_casa_martinez_name),
                latitude = 14.6520,
                longitude = -90.8095,
                status = VisitStatus.SCHEDULED,
                nextVisit = s(R.string.time_tomorrow_10_am),
                distance = 1.8,
                description = s(R.string.desc_family_planning_consultation)
            ),
            ClientHouse(
                id = "5",
                address = s(R.string.address_barrio_el_calvario),
                clientName = s(R.string.client_familia_rodriguez_name),
                latitude = 14.6475,
                longitude = -90.8320,
                status = VisitStatus.TO_VISIT,
                distance = 1.6,
                description = s(R.string.desc_hypertension_screening_needed)
            ),
            ClientHouse(
                id = "6",
                address = s(R.string.address_km_54_ruta_interamericana),
                clientName = s(R.string.client_familia_cumes_name),
                latitude = 14.6265,
                longitude = -90.8130,
                status = VisitStatus.SCHEDULED,
                nextVisit = s(R.string.time_friday_2_pm),
                distance = 2.0,
                description = s(R.string.desc_prenatal_care_appointment)
            )
        )

        // Today's itinerary — ordered stops (path for the map)
        _dailyItineraryStops.value = listOf(
            ItineraryStop(order = 1, id = "1", label = s(R.string.client_familia_hernandez_name), address = s(R.string.address_calle_real_chimaltenango), latitude = 14.6425, longitude = -90.8170, description = s(R.string.desc_maternal_health_checkup_needed)),
            ItineraryStop(order = 2, id = "2", label = s(R.string.client_casa_lopez_name), address = s(R.string.address_canton_san_jacinto), latitude = 14.6310, longitude = -90.8290, description = s(R.string.desc_child_vaccination_due)),
            ItineraryStop(order = 3, id = "hf2", label = s(R.string.facility_centro_salud_chimaltenango_name), address = s(R.string.address_centro_chimaltenango), latitude = 14.6380, longitude = -90.8215, description = s(R.string.desc_dropoff_pickup_supplies)),
            ItineraryStop(order = 4, id = "5", label = s(R.string.client_familia_rodriguez_name), address = s(R.string.address_barrio_el_calvario), latitude = 14.6475, longitude = -90.8320, description = s(R.string.desc_hypertension_screening_needed)),
            ItineraryStop(order = 5, id = "hf1", label = s(R.string.facility_hospital_nacional_chimaltenango_name), address = s(R.string.address_salida_antigua_guatemala), latitude = 14.6614, longitude = -90.8194, description = s(R.string.service_outpatient)),
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