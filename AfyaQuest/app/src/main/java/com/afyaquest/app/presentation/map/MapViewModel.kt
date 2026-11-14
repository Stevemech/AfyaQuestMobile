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
import com.afyaquest.app.data.facilities.FacilityDirectory
import com.afyaquest.app.data.facilities.FacilityRegion
import com.afyaquest.app.data.local.dao.UserDao
import com.afyaquest.app.data.local.entity.PendingClientVisitEntity
import com.afyaquest.app.data.repository.ItineraryRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for Map screen.
 * Manages daily itinerary (ordered stops from API) and real-time device location.
 * Persists stop completion locally (scoped to today) and syncs to AWS.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val progressDataStore: ProgressDataStore,
    private val syncManager: SyncManager,
    private val userDao: UserDao,
    private val itineraryRepository: ItineraryRepository
) : ViewModel() {

    /**
     * Device position; null until the first real GPS fix. It is never a made-up default: a fake
     * starting point used to draw the day's route from another continent.
     */
    private val _liveLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val liveLocation: StateFlow<Pair<Double, Double>?> = _liveLocation.asStateFlow()

    /** The area this user works in (decides which facilities are listed and where the map opens). */
    private val _region = MutableStateFlow<FacilityRegion?>(null)
    private var facilityDirectory: FacilityDirectory? = null
    private var userOrganization: String? = null

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location -> onLocation(location.latitude, location.longitude) }
        }
    }

    /**
     * Facilities of the user's region. Once the device position is known they carry their real
     * distance and are listed nearest first.
     */
    val healthFacilities: StateFlow<List<HealthFacility>> =
        combine(_region, _liveLocation) { region, location ->
            FacilityDirectory.withDistances(region?.facilities ?: emptyList(), location?.first, location?.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    private companion object {
        const val FACILITY_DIRECTORY_ASSET = "facilities/facility_directory.json"

        /** Nairobi: only used for the instant before the directory has been read. */
        val FALLBACK_CENTER = -1.2921 to 36.8219
    }

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
     * Load the bundled facility directory and pick the region for this user's organization
     * (see [FacilityDirectory.regionFor]).
     */
    private fun loadHealthFacilities() {
        viewModelScope.launch {
            try {
                facilityDirectory = withContext(Dispatchers.IO) {
                    val json = context.assets.open(FACILITY_DIRECTORY_ASSET).bufferedReader().use { it.readText() }
                    FacilityDirectory.parse(json)
                }
                userOrganization = tokenManager.getUserId()?.let { userDao.getUserById(it)?.organization }
                updateRegion()
            } catch (e: Exception) {
                Log.e("MapViewModel", "Could not load the facility directory: ${e.message}")
            }
        }
    }

    private fun updateRegion() {
        val location = _liveLocation.value
        _region.value = facilityDirectory?.regionFor(userOrganization, location?.first, location?.second)
    }

    private fun onLocation(latitude: Double, longitude: Double) {
        val first = _liveLocation.value == null
        _liveLocation.value = latitude to longitude
        // A user whose organization has no region yet gets the region they are standing in.
        if (first) updateRegion()
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
     * Load today's itinerary. Safe to call again (Retry / refresh).
     *
     * The route comes from the server when it can be reached and from the copy kept on the phone
     * otherwise (see [ItineraryRepository]); only when there is neither is an error shown.
     */
    fun loadItinerary() {
        viewModelScope.launch {
            // The route saved on the phone first (Home downloads it when the app opens), so the
            // map has its stops at once; the server's copy replaces it when it arrives.
            val saved = itineraryRepository.cachedToday()
            _rawItinerary.value = if (saved != null) Resource.Success(saved) else Resource.Loading()

            val fresh = itineraryRepository.refreshToday()
            when {
                fresh != null -> _rawItinerary.value = Resource.Success(fresh)
                saved == null -> _rawItinerary.value = Resource.Error(loadErrorText())
            }
        }
    }

    private fun loadErrorText(): String = context.getString(R.string.field_map_load_error)

    /**
     * Mark an itinerary stop as visited today.
     * Persists locally (date-scoped) and queues the visit for upload.
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

            // The queue is the only upload path: it sends at once when there is a connection and
            // later when there is not. Sending directly as well uploaded every visit twice, and
            // the supervisor got two "stop visited" notifications for one stop.
            syncManager.queueClientVisit(
                PendingClientVisitEntity(
                    userId = userId,
                    clientId = stopId,
                    status = "completed",
                    visitDate = today,
                    notes = ""
                )
            )
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
            location?.let { onLocation(it.latitude, it.longitude) }
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

    /** The day's route: from where the CHV is (once that is known) through the stops in order. */
    fun getFullRoutePoints(): List<Pair<Double, Double>> =
        listOfNotNull(_liveLocation.value) + getItineraryPathPoints()

    /**
     * Where the map should open before there is a GPS fix: the first stop of today's route, or
     * the centre of the user's region.
     */
    val mapCenter: StateFlow<Pair<Double, Double>> =
        combine(_region, _rawItinerary) { region, itinerary ->
            itinerary.data?.firstOrNull()?.let { it.latitude to it.longitude }
                ?: region?.let { it.centerLatitude to it.centerLongitude }
                ?: FALLBACK_CENTER
        }.stateIn(viewModelScope, SharingStarted.Eagerly, FALLBACK_CENTER)

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
