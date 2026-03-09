package com.example.afyaquest.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.local.entity.PendingClientVisitEntity
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.domain.model.ClientHouse
import com.example.afyaquest.domain.model.FacilityType
import com.example.afyaquest.domain.model.HealthFacility
import com.example.afyaquest.domain.model.ItineraryStop
import com.example.afyaquest.domain.model.VisitStatus
import com.example.afyaquest.sync.SyncManager
import com.example.afyaquest.util.ProgressDataStore
import com.example.afyaquest.util.TokenManager
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Map screen.
 * Manages daily itinerary (ordered stops from API) and real-time device location.
 * Persists stop completion locally and syncs to AWS.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val progressDataStore: ProgressDataStore,
    private val syncManager: SyncManager
) : ViewModel() {

    // Default map camera — Chimaltenango, Guatemala (AfyaQuest operational area)
    val defaultLatitude = 14.6392
    val defaultLongitude = -90.8208
    val defaultZoom = 13f

    private val defaultLiveLatitude = 14.6392
    private val defaultLiveLongitude = -90.8208

    private val _liveLocationLatitude = MutableStateFlow(defaultLiveLatitude)
    val liveLocationLatitude: StateFlow<Double> = _liveLocationLatitude.asStateFlow()

    private val _liveLocationLongitude = MutableStateFlow(defaultLiveLongitude)
    val liveLocationLongitude: StateFlow<Double> = _liveLocationLongitude.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

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

    /** Ordered list of stops for today's itinerary from API */
    private val _dailyItineraryStops = MutableStateFlow<List<ItineraryStop>>(emptyList())
    val dailyItineraryStops: StateFlow<List<ItineraryStop>> = _dailyItineraryStops.asStateFlow()

    /** Set of completed stop IDs (persisted locally) */
    private val _completedStopIds = MutableStateFlow<Set<String>>(emptySet())
    val completedStopIds: StateFlow<Set<String>> = _completedStopIds.asStateFlow()

    private val _selectedClient = MutableStateFlow<ClientHouse?>(null)
    val selectedClient: StateFlow<ClientHouse?> = _selectedClient.asStateFlow()

    private val _statusFilter = MutableStateFlow<VisitStatus?>(null)
    val statusFilter: StateFlow<VisitStatus?> = _statusFilter.asStateFlow()

    init {
        loadHealthFacilities()
        fetchItinerariesFromApi()
        loadCompletedStops()
    }

    /**
     * Load real health facilities in the Chimaltenango/Guatemala area.
     */
    private fun loadHealthFacilities() {
        _healthFacilities.value = listOf(
            HealthFacility(
                id = "hf1",
                name = "Hospital Nacional de Chimaltenango",
                type = FacilityType.HOSPITAL,
                latitude = 14.6614,
                longitude = -90.8194,
                servicesAvailable = listOf("Emergency", "Maternity", "Pediatrics", "Surgery", "Outpatient", "Laboratory"),
                distance = 0.8
            ),
            HealthFacility(
                id = "hf2",
                name = "Centro de Salud Chimaltenango",
                type = FacilityType.CLINIC,
                latitude = 14.6380,
                longitude = -90.8215,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Family Planning", "Pharmacy"),
                distance = 0.3
            ),
            HealthFacility(
                id = "hf3",
                name = "Hospital Nacional San Juan Comalapa",
                type = FacilityType.HOSPITAL,
                latitude = 14.7414,
                longitude = -90.8886,
                servicesAvailable = listOf("Emergency", "Maternity", "Outpatient", "X-Ray"),
                distance = 12.5
            ),
            HealthFacility(
                id = "hf4",
                name = "Puesto de Salud Parramos",
                type = FacilityType.CLINIC,
                latitude = 14.6128,
                longitude = -90.8042,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Pharmacy"),
                distance = 3.2
            ),
            HealthFacility(
                id = "hf5",
                name = "Centro de Salud San Andres Itzapa",
                type = FacilityType.HEALTH_CENTER,
                latitude = 14.6195,
                longitude = -90.8439,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Maternal Health", "Nutrition"),
                distance = 4.1
            ),
            HealthFacility(
                id = "hf6",
                name = "Hospital Antiguo de Antigua Guatemala",
                type = FacilityType.HOSPITAL,
                latitude = 14.5586,
                longitude = -90.7345,
                servicesAvailable = listOf("Emergency", "Surgery", "Maternity", "Pediatrics", "Laboratory", "X-Ray"),
                distance = 15.0
            ),
            HealthFacility(
                id = "hf7",
                name = "Puesto de Salud Zaragoza",
                type = FacilityType.CLINIC,
                latitude = 14.6492,
                longitude = -90.8893,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Pharmacy"),
                distance = 6.3
            ),
            HealthFacility(
                id = "hf8",
                name = "Centro de Salud Patzicia",
                type = FacilityType.HEALTH_CENTER,
                latitude = 14.6312,
                longitude = -90.9270,
                servicesAvailable = listOf("Primary Care", "Maternal Health", "Vaccination", "Family Planning"),
                distance = 9.8
            )
        )
    }

    /**
     * Load completed stops from DataStore
     */
    private fun loadCompletedStops() {
        viewModelScope.launch {
            progressDataStore.getCompletedStops().collect { stops ->
                _completedStopIds.value = stops
            }
        }
    }

    /**
     * Fetch today's itinerary from the API.
     */
    private fun fetchItinerariesFromApi() {
        viewModelScope.launch {
            try {
                val idToken = tokenManager.getIdToken() ?: return@launch
                val response = apiService.getItineraries("Bearer $idToken")
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    val itinerary = body.itineraries.firstOrNull() ?: return@launch
                    if (itinerary.stops.isNotEmpty()) {
                        val localCompleted = _completedStopIds.value
                        _dailyItineraryStops.value = itinerary.stops.map { stopDto ->
                            val stopId = stopDto.houseId ?: "stop-${stopDto.order}"
                            ItineraryStop(
                                order = stopDto.order,
                                id = stopId,
                                label = stopDto.label,
                                address = stopDto.address,
                                latitude = stopDto.latitude,
                                longitude = stopDto.longitude,
                                description = stopDto.description,
                                completed = stopDto.completed || localCompleted.contains(stopId)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("MapViewModel", "Failed to fetch itineraries from API: ${e.message}")
            }
        }
    }

    /**
     * Mark an itinerary stop as completed.
     * Persists locally, queues for sync, and updates AWS.
     */
    fun markStopCompleted(stopId: String) {
        if (_completedStopIds.value.contains(stopId)) return
        _completedStopIds.value = _completedStopIds.value + stopId

        // Update the stop in the itinerary list immediately
        _dailyItineraryStops.value = _dailyItineraryStops.value.map { stop ->
            if (stop.id == stopId) stop.copy(completed = true) else stop
        }

        viewModelScope.launch {
            progressDataStore.markStopCompleted(stopId)

            // Queue for sync via PendingClientVisit
            val userId = tokenManager.getUserId() ?: ""
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = dateFormat.format(Date())

            syncManager.queueClientVisit(
                PendingClientVisitEntity(
                    userId = userId,
                    clientId = stopId,
                    status = "completed",
                    visitDate = today,
                    notes = ""
                )
            )

            // Also try direct API sync
            try {
                val token = tokenManager.getIdToken() ?: return@launch
                val body = mapOf<String, Any>(
                    "type" to "itinerary_stop_complete",
                    "itemId" to stopId,
                    "date" to today
                )
                apiService.updateUserProgress("Bearer $token", body)
            } catch (e: Exception) {
                Log.d("MapViewModel", "Stop sync will retry via SyncManager: ${e.message}")
            }
        }
    }

    fun isStopCompleted(stopId: String): Boolean =
        _completedStopIds.value.contains(stopId)

    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).apply {
            setMinUpdateIntervalMillis(5_000L)
            setMaxUpdates(Int.MAX_VALUE)
            setMinUpdateDistanceMeters(10f)
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

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    fun getItineraryPathPoints(): List<Pair<Double, Double>> =
        _dailyItineraryStops.value.map { it.latitude to it.longitude }

    fun getFullRoutePoints(): List<Pair<Double, Double>> {
        val start = _liveLocationLatitude.value to _liveLocationLongitude.value
        val itinerary = getItineraryPathPoints()
        return listOf(start) + itinerary
    }

    fun getFilteredClients(): List<ClientHouse> {
        val filter = _statusFilter.value
        return if (filter == null) {
            _clientHouses.value
        } else {
            _clientHouses.value.filter { it.status == filter }
        }
    }

    fun setStatusFilter(status: VisitStatus?) {
        _statusFilter.value = status
    }

    fun selectClient(client: ClientHouse?) {
        _selectedClient.value = client
    }

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
        markStopCompleted(clientId)
    }
}
