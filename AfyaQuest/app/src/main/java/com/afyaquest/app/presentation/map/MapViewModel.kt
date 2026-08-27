package com.afyaquest.app.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.R
import com.afyaquest.app.data.local.entity.PendingClientVisitEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.domain.model.ClientHouse
import com.afyaquest.app.domain.model.FacilityType
import com.afyaquest.app.domain.model.HealthFacility
import com.afyaquest.app.domain.model.ItineraryStop
import com.afyaquest.app.domain.model.VisitStatus
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Map screen.
 * Manages daily itinerary (ordered stops from API) and real-time device location.
 * Persists stop completion locally (scoped to today) and syncs to AWS.
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

    /** Raw itinerary as returned by the API (completion flag from the server only). */
    private val _rawItinerary = MutableStateFlow<Resource<List<ItineraryStop>>>(Resource.Loading())

    /** Stop ids marked visited today (persisted locally, date-scoped). */
    private val _completedStopIds = MutableStateFlow<Set<String>>(emptySet())
    val completedStopIds: StateFlow<Set<String>> = _completedStopIds.asStateFlow()

    /**
     * Loading / error / success state of today's itinerary. On success the stops carry
     * completion merged from the server flag and today's local "visited" marks.
     */
    val itineraryState: StateFlow<Resource<List<ItineraryStop>>> =
        combine(_rawItinerary, _completedStopIds) { raw, completed ->
            val merged: Resource<List<ItineraryStop>> = when (raw) {
                is Resource.Success -> Resource.Success(mergeCompletion(raw.data ?: emptyList(), completed))
                is Resource.Error -> Resource.Error(
                    raw.message ?: context.getString(R.string.field_map_load_error),
                    raw.data?.let { mergeCompletion(it, completed) }
                )
                is Resource.Loading -> Resource.Loading()
            }
            merged
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Resource.Loading<List<ItineraryStop>>()
        )

    /** Ordered list of stops for today's itinerary (empty until loaded). */
    val dailyItineraryStops: StateFlow<List<ItineraryStop>> =
        itineraryState.map { it.data ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedClient = MutableStateFlow<ClientHouse?>(null)
    val selectedClient: StateFlow<ClientHouse?> = _selectedClient.asStateFlow()

    private val _statusFilter = MutableStateFlow<VisitStatus?>(null)
    val statusFilter: StateFlow<VisitStatus?> = _statusFilter.asStateFlow()

    init {
        loadHealthFacilities()
        loadCompletedStops()
        loadItinerary()
    }

    private fun mergeCompletion(stops: List<ItineraryStop>, completed: Set<String>): List<ItineraryStop> =
        stops.map { stop ->
            if (!stop.completed && completed.contains(stop.id)) stop.copy(completed = true) else stop
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
     * Observe the stops marked visited today (date-scoped in DataStore).
     */
    private fun loadCompletedStops() {
        viewModelScope.launch {
            progressDataStore.getCompletedStopsToday().collect { stops ->
                _completedStopIds.value = stops
            }
        }
    }

    /**
     * Fetch today's itinerary from the API. Safe to call again (Retry / refresh).
     */
    fun loadItinerary() {
        viewModelScope.launch {
            _rawItinerary.value = Resource.Loading()
            try {
                val idToken = tokenManager.getIdToken()
                if (idToken == null) {
                    _rawItinerary.value = Resource.Error(loadErrorText())
                    return@launch
                }
                val response = apiService.getItineraries("Bearer $idToken")
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _rawItinerary.value = Resource.Error(loadErrorText())
                    return@launch
                }
                val itinerary = body.itineraries.firstOrNull()
                val stops = itinerary?.stops.orEmpty().map { stopDto ->
                    ItineraryStop(
                        order = stopDto.order,
                        id = stopDto.houseId ?: "stop-${stopDto.order}",
                        label = stopDto.label,
                        address = stopDto.address,
                        latitude = stopDto.latitude,
                        longitude = stopDto.longitude,
                        description = stopDto.description,
                        completed = stopDto.completed
                    )
                }
                progressDataStore.setItineraryTotal(stops.size)
                _rawItinerary.value = Resource.Success(stops)
            } catch (e: Exception) {
                Log.d("MapViewModel", "Failed to fetch itineraries from API: ${e.message}")
                _rawItinerary.value = Resource.Error(loadErrorText())
            }
        }
    }

    private fun loadErrorText(): String = context.getString(R.string.field_map_load_error)

    /**
     * Mark an itinerary stop as visited today.
     * Persists locally (date-scoped), queues for sync, and updates AWS.
     */
    fun markStopCompleted(stopId: String) {
        if (_completedStopIds.value.contains(stopId)) return
        // Optimistic update; the DataStore flow will confirm it right after.
        _completedStopIds.value = _completedStopIds.value + stopId

        viewModelScope.launch {
            progressDataStore.markStopCompleted(stopId)

            // Queue for sync via PendingClientVisit
            val userId = tokenManager.getUserId() ?: ""
            val today = DateUtils.todayIso()

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
        (_rawItinerary.value.data ?: emptyList()).map { it.latitude to it.longitude }

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
