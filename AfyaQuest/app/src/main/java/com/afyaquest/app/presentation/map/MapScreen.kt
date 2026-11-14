package com.afyaquest.app.presentation.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.data.facilities.FacilityDirectory
import com.afyaquest.app.domain.model.HealthFacility
import com.afyaquest.app.domain.model.ItineraryStop
import com.afyaquest.app.presentation.components.DoneBadge
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.presentation.components.ErrorState
import com.afyaquest.app.presentation.components.LoadingState
import com.afyaquest.app.presentation.components.ProgressSummary
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.navigation.navigateSingle
import com.afyaquest.app.ui.theme.AfyaSuccess
import com.afyaquest.app.util.Resource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import com.afyaquest.app.presentation.navigation.popBack

// Colours that match the Google Maps default marker hues used below, so the legend is truthful.
private val PendingMarkerColor = Color(0xFFE53935)   // HUE_RED
private val VisitedMarkerColor = Color(0xFF43A047)   // HUE_GREEN
private val FacilityMarkerColor = Color(0xFF8E24AA)  // HUE_VIOLET

/** Zoom when centred on the device position (a few streets around the CHV). */
private const val LOCATION_ZOOM = 14f

/** Zoom when only the region / today's first stop is known. */
private const val REGION_ZOOM = 13f

/** While following the device position, re-centre once it is this far from the middle of the map. */
private const val RECENTER_DISTANCE_KM = 0.15

/**
 * Map/Itinerary screen
 * Shows Google Map with daily itinerary path, ordered stops list, and health facilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val healthFacilities by viewModel.healthFacilities.collectAsState()
    val itineraryState by viewModel.itineraryState.collectAsState()
    val dailyStops = itineraryState.data ?: emptyList()
    // Collect real-time device location so route and "You are here" marker update when GPS changes.
    val liveLocation by viewModel.liveLocation.collectAsState()
    val mapCenter by viewModel.mapCenter.collectAsState()
    val fullRoutePoints = remember(liveLocation, dailyStops) {
        viewModel.getFullRoutePoints()
    }

    val context = LocalContext.current
    // Hoisted so the My Location layer turns on the moment the permission is granted.
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.startLocationUpdates()
    }

    var selectedTab by remember { mutableStateOf(0) }

    // Kept above the tabs so the map holds its position and zoom while the CHV looks at the
    // facilities list and comes back. Opens on the device position when it is already known,
    // otherwise on today's first stop / the user's region (mapCenter).
    val cameraPositionState = rememberCameraPositionState {
        val start = liveLocation ?: mapCenter
        position = CameraPosition.fromLatLngZoom(
            LatLng(start.first, start.second),
            if (liveLocation != null) LOCATION_ZOOM else REGION_ZOOM
        )
    }
    // Snap to the device position when the first GPS fix arrives. Until then follow mapCenter,
    // which changes once the region / today's route has loaded.
    var hasSnappedToLocation by remember { mutableStateOf(liveLocation != null) }
    // Keep the map on the device position until the CHV takes control of it (pans, zooms, taps a
    // marker or picks a stop). The first fix a phone reports is often its last known position
    // from hours ago; snapping once and never again left the map on the place the CHV was this
    // morning. The "My location" button hands control back.
    var followLocation by remember { mutableStateOf(true) }
    var zoomedToLocation by remember { mutableStateOf(liveLocation != null) }
    // The "My location" button moves the camera with the same reason code as the zoom buttons
    // and a marker tap, so it is told apart by its own callback.
    var recenterRequested by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) return@LaunchedEffect
        when (cameraPositionState.cameraMoveStartedReason) {
            CameraMoveStartedReason.GESTURE -> followLocation = false
            CameraMoveStartedReason.API_ANIMATION ->
                if (recenterRequested) recenterRequested = false else followLocation = false
            else -> Unit
        }
    }
    LaunchedEffect(liveLocation) {
        val location = liveLocation ?: return@LaunchedEffect
        hasSnappedToLocation = true
        if (!followLocation) return@LaunchedEffect
        val target = LatLng(location.first, location.second)
        if (!zoomedToLocation) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, LOCATION_ZOOM))
            zoomedToLocation = true
        } else {
            val shown = cameraPositionState.position.target
            val movedKm = FacilityDirectory.distanceKm(shown.latitude, shown.longitude, target.latitude, target.longitude)
            // Small GPS jitter is not worth moving the map for.
            if (movedKm > RECENTER_DISTANCE_KM) cameraPositionState.animate(CameraUpdateFactory.newLatLng(target))
        }
    }
    LaunchedEffect(mapCenter) {
        if (!hasSnappedToLocation) {
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(LatLng(mapCenter.first, mapCenter.second), REGION_ZOOM)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_itinerary), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.map)) },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.health_facilities)) },
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> MapAndItineraryTab(
                    itineraryState = itineraryState,
                    healthFacilities = healthFacilities,
                    fullRoutePoints = fullRoutePoints,
                    cameraPositionState = cameraPositionState,
                    onStopSelected = { followLocation = false },
                    onRecenterRequested = {
                        recenterRequested = true
                        followLocation = true
                    },
                    hasLocationPermission = hasLocationPermission,
                    onRequestLocation = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    onRetry = { viewModel.loadItinerary() },
                    onMarkStopVisited = { stopId -> viewModel.markStopCompleted(stopId) },
                    onFillReport = { navController.navigateSingle(Screen.DailyReport.route) }
                )
                1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (healthFacilities.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(
                                title = stringResource(R.string.field_facilities_empty),
                                icon = Icons.Default.LocalHospital
                            )
                        }
                    }
                    items(healthFacilities, key = { it.id }) { facility ->
                        HealthFacilityCard(facility = facility)
                    }
                    if (healthFacilities.isNotEmpty()) {
                        item(key = "source") {
                            Text(
                                text = stringResource(R.string.field_facilities_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapAndItineraryTab(
    itineraryState: Resource<List<ItineraryStop>>,
    healthFacilities: List<HealthFacility>,
    fullRoutePoints: List<Pair<Double, Double>>,
    cameraPositionState: CameraPositionState,
    onStopSelected: () -> Unit,
    onRecenterRequested: () -> Unit,
    hasLocationPermission: Boolean,
    onRequestLocation: () -> Unit,
    onRetry: () -> Unit,
    onMarkStopVisited: (String) -> Unit,
    onFillReport: () -> Unit
) {
    val dailyStops = itineraryState.data ?: emptyList()
    val routeLatLngs = remember(fullRoutePoints) { fullRoutePoints.map { LatLng(it.first, it.second) } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Stop awaiting "Mark visited" confirmation
    var stopToConfirm by remember { mutableStateOf<ItineraryStop?>(null) }
    stopToConfirm?.let { stop ->
        AlertDialog(
            onDismissRequest = { stopToConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AfyaSuccess
                )
            },
            title = { Text(stringResource(R.string.field_map_confirm_visited_title, stop.label)) },
            text = { Text(stringResource(R.string.field_map_confirm_visited_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkStopVisited(stop.id)
                        stopToConfirm = null
                    },
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(stringResource(R.string.field_map_confirm_visited_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { stopToConfirm = null },
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            LocationRationaleCard(onEnable = onRequestLocation)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (hasLocationPermission) 300.dp else 220.dp)
        ) {
            ItineraryMap(
                cameraPositionState = cameraPositionState,
                onRecenterRequested = onRecenterRequested,
                hasLocationPermission = hasLocationPermission,
                routeLatLngs = routeLatLngs,
                dailyStops = dailyStops,
                healthFacilities = healthFacilities
            )
        }

        MapLegendRow()

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.your_stops_for_the_day),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (itineraryState) {
                    is Resource.Loading -> LoadingState()
                    is Resource.Error -> ErrorState(
                        message = stringResource(R.string.field_map_load_error),
                        onRetry = onRetry
                    )
                    is Resource.Success -> {
                        if (dailyStops.isEmpty()) {
                            EmptyState(
                                title = stringResource(R.string.no_stops_for_today),
                                icon = Icons.Outlined.Map
                            )
                        } else {
                            val visitedCount = dailyStops.count { it.completed }
                            val nextStopId = dailyStops.firstOrNull { !it.completed }?.id
                            ProgressSummary(
                                label = stringResource(R.string.field_map_progress_label),
                                done = visitedCount,
                                total = dailyStops.size
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                if (nextStopId == null) {
                                    item {
                                        AllVisitedBanner(onFillReport = onFillReport)
                                    }
                                }
                                items(dailyStops) { stop ->
                                    ItineraryStopRow(
                                        stop = stop,
                                        isNext = stop.id == nextStopId,
                                        onMarkVisited = { stopToConfirm = stop },
                                        onTap = {
                                            // Snap map camera to this stop (and stop following the
                                            // device position, which would move it straight back)
                                            onStopSelected()
                                            scope.launch {
                                                cameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(stop.latitude, stop.longitude),
                                                        16f
                                                    ),
                                                    durationMs = 600
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryMap(
    cameraPositionState: CameraPositionState,
    onRecenterRequested: () -> Unit,
    hasLocationPermission: Boolean,
    routeLatLngs: List<LatLng>,
    dailyStops: List<ItineraryStop>,
    healthFacilities: List<HealthFacility>
) {
    val context = LocalContext.current
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = remember(hasLocationPermission) {
            MapProperties(isMyLocationEnabled = hasLocationPermission)
        },
        uiSettings = remember(hasLocationPermission) {
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = hasLocationPermission
            )
        },
        // false: the map still does its own move to the device position.
        onMyLocationButtonClick = {
            onRecenterRequested()
            false
        }
    ) {
        // Route polyline
        routeLatLngs.takeIf { it.size >= 2 }?.let { points ->
            Polyline(
                points = points,
                color = Color(0xFF254D4D),
                width = 10f,
                geodesic = true
            )
        }
        // Marker states and icons are remembered per stop / facility. Built inline they were new
        // objects on every recomposition (every GPS fix, every few seconds), and the map threw
        // away and rebuilt every marker each time.
        val pendingIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) }
        val visitedIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN) }
        val facilityIcon = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET) }

        // Stop markers with color based on completion
        dailyStops.forEach { stop ->
            key(stop.id) {
                Marker(
                    state = remember(stop.latitude, stop.longitude) {
                        MarkerState(position = LatLng(stop.latitude, stop.longitude))
                    },
                    title = "${stop.order}. ${stop.label}",
                    snippet = if (stop.completed) context.getString(R.string.visited) else stop.address,
                    icon = if (stop.completed) visitedIcon else pendingIcon
                )
            }
        }
        // Health facility markers (violet)
        healthFacilities.forEach { facility ->
            key(facility.id) {
                Marker(
                    state = remember(facility.latitude, facility.longitude) {
                        MarkerState(position = LatLng(facility.latitude, facility.longitude))
                    },
                    title = facility.name,
                    snippet = remember(facility.servicesAvailable, context) {
                        facility.servicesAvailable.take(3).joinToString(", ") { serviceLabel(context, it) }
                    },
                    icon = facilityIcon
                )
            }
        }
    }
}

/** Compact explanation of the marker colours on the map. */
@Composable
private fun MapLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = PendingMarkerColor, label = stringResource(R.string.pending))
        LegendItem(color = VisitedMarkerColor, label = stringResource(R.string.visited))
        LegendItem(color = FacilityMarkerColor, label = stringResource(R.string.field_map_legend_facility))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shown instead of silently requesting the permission on entry. */
@Composable
private fun LocationRationaleCard(onEnable: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.field_map_location_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.field_map_location_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEnable,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(stringResource(R.string.field_map_location_button))
                }
            }
        }
    }
}

/** Success banner once every stop of the day is visited; points to the daily report. */
@Composable
private fun AllVisitedBanner(onFillReport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.completed),
                    tint = AfyaSuccess,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.all_done_today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.field_map_all_visited_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFillReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.field_map_fill_report))
            }
        }
    }
}

@Composable
private fun ItineraryStopRow(
    stop: ItineraryStop,
    isNext: Boolean,
    onMarkVisited: () -> Unit,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(8.dp)
    val bgColor = when {
        stop.completed -> AfyaSuccess.copy(alpha = 0.08f)
        isNext -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onTap)
            .background(bgColor, shape)
            .then(
                if (isNext) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
            )
            .padding(12.dp)
    ) {
        if (isNext) {
            Text(
                text = stringResource(R.string.next_up),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Order number / done badge
            if (stop.completed) {
                DoneBadge(size = 32.dp, contentDescription = stringResource(R.string.visited))
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        text = "${stop.order}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stop.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                stop.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (stop.completed) {
                Badge(containerColor = AfyaSuccess) {
                    Text(
                        text = stringResource(R.string.visited),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action buttons row
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Directions button — opens Google Maps navigation
            OutlinedButton(
                onClick = { openDirections(context, stop.latitude, stop.longitude) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.directions), fontSize = 12.sp)
            }

            if (!stop.completed) {
                FilledTonalButton(
                    onClick = onMarkVisited,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Text(stringResource(R.string.mark_visited), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun openDirections(context: android.content.Context, latitude: Double, longitude: Double) {
    val uri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback to browser if Google Maps not installed
        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

/** "0.8 km" up close, whole kilometres once the decimal stops meaning anything. */
internal fun formatDistanceKm(distanceKm: Double): String =
    if (distanceKm < 100) String.format(java.util.Locale.US, "%.1f km", distanceKm)
    else String.format(java.util.Locale.US, "%.0f km", distanceKm)

/** Localized name of a service key from the facility directory ("primary_care" -> "Primary Care"). */
internal fun serviceLabel(context: android.content.Context, key: String): String {
    val res = when (key) {
        "emergency" -> R.string.service_emergency
        "maternity" -> R.string.service_maternity
        "pediatrics" -> R.string.service_pediatrics
        "surgery" -> R.string.service_surgery
        "outpatient" -> R.string.service_outpatient
        "laboratory" -> R.string.service_laboratory
        "xray" -> R.string.service_xray
        "primary_care" -> R.string.service_primary_care
        "vaccination" -> R.string.service_vaccination
        "family_planning" -> R.string.service_family_planning
        "pharmacy" -> R.string.service_pharmacy
        "maternal_health" -> R.string.category_maternal_health
        "nutrition" -> R.string.category_nutrition
        "mental_health" -> R.string.service_mental_health
        else -> return key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    return context.getString(res)
}

@Composable
fun HealthFacilityCard(facility: HealthFacility) {
    val context = LocalContext.current
    val facilityTypeLabel = when (facility.type) {
        com.afyaquest.app.domain.model.FacilityType.HOSPITAL -> stringResource(R.string.facility_type_hospital)
        com.afyaquest.app.domain.model.FacilityType.CLINIC -> stringResource(R.string.facility_type_clinic)
        com.afyaquest.app.domain.model.FacilityType.HEALTH_CENTER -> stringResource(R.string.facility_type_health_center)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = facility.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = facilityTypeLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                facility.distance?.let { distance ->
                    Text(
                        text = formatDistanceKm(distance),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Services are only listed where they are known; an empty list shows nothing rather
            // than a guess.
            if (facility.servicesAvailable.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.services) + ":",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = facility.servicesAvailable.map { serviceLabel(context, it) }.joinToString(", "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { openDirections(context, facility.latitude, facility.longitude) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.get_directions), fontSize = 13.sp)
            }
        }
    }
}
