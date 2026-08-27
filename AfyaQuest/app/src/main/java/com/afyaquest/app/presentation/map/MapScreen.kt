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
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

// Colours that match the Google Maps default marker hues used below, so the legend is truthful.
private val PendingMarkerColor = Color(0xFFE53935)   // HUE_RED
private val VisitedMarkerColor = Color(0xFF43A047)   // HUE_GREEN
private val FacilityMarkerColor = Color(0xFF8E24AA)  // HUE_VIOLET

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
    val liveLocationLat by viewModel.liveLocationLatitude.collectAsState()
    val liveLocationLng by viewModel.liveLocationLongitude.collectAsState()
    val fullRoutePoints = remember(liveLocationLat, liveLocationLng, dailyStops) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_itinerary), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                    liveLocationLat = liveLocationLat,
                    liveLocationLng = liveLocationLng,
                    defaultLat = viewModel.defaultLatitude,
                    defaultLng = viewModel.defaultLongitude,
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
                    items(healthFacilities) { facility ->
                        HealthFacilityCard(facility = facility)
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
    liveLocationLat: Double,
    liveLocationLng: Double,
    defaultLat: Double,
    defaultLng: Double,
    hasLocationPermission: Boolean,
    onRequestLocation: () -> Unit,
    onRetry: () -> Unit,
    onMarkStopVisited: (String) -> Unit,
    onFillReport: () -> Unit
) {
    val dailyStops = itineraryState.data ?: emptyList()
    // Use live location as initial camera position if available, otherwise default
    val initialLat = if (liveLocationLat != 0.0) liveLocationLat else defaultLat
    val initialLng = if (liveLocationLng != 0.0) liveLocationLng else defaultLng
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), 14f)
    }
    val routeLatLngs = fullRoutePoints.map { LatLng(it.first, it.second) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Snap to live location when it first arrives
    var hasSnappedToLocation by remember { mutableStateOf(false) }
    LaunchedEffect(liveLocationLat, liveLocationLng) {
        if (!hasSnappedToLocation && liveLocationLat != defaultLat) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(liveLocationLat, liveLocationLng), 14f)
            )
            hasSnappedToLocation = true
        }
    }

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
                                            // Snap map camera to this stop
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
        // Stop markers with color based on completion
        dailyStops.forEach { stop ->
            Marker(
                state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                title = "${stop.order}. ${stop.label}",
                snippet = if (stop.completed) context.getString(R.string.visited) else stop.address,
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (stop.completed) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                )
            )
        }
        // Health facility markers (violet)
        healthFacilities.forEach { facility ->
            Marker(
                state = MarkerState(position = LatLng(facility.latitude, facility.longitude)),
                title = facility.name,
                snippet = facility.servicesAvailable.take(3).joinToString(", "),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
            )
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
                        text = "${distance}km",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.services) + ":",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = facility.servicesAvailable.joinToString(", "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

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
