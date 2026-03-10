package com.example.afyaquest.presentation.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import com.example.afyaquest.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.domain.model.HealthFacility
import com.example.afyaquest.domain.model.ItineraryStop
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

/**
 * Map/Itinerary screen
 * Shows Google Map with daily itinerary path, ordered stops list, client houses, and health facilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val healthFacilities by viewModel.healthFacilities.collectAsState()
    val dailyStops by viewModel.dailyItineraryStops.collectAsState()
    // Collect real-time device location so route and "You are here" marker update when GPS changes.
    val liveLocationLat by viewModel.liveLocationLatitude.collectAsState()
    val liveLocationLng by viewModel.liveLocationLongitude.collectAsState()
    val fullRoutePoints = remember(liveLocationLat, liveLocationLng, dailyStops) {
        viewModel.getFullRoutePoints()
    }

    val context = LocalContext.current
    // Permission launcher: when user grants ACCESS_FINE_LOCATION, start receiving GPS updates.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startLocationUpdates()
    }
    LaunchedEffect(Unit) {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> viewModel.startLocationUpdates()
            else -> permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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
                    dailyStops = dailyStops,
                    healthFacilities = healthFacilities,
                    fullRoutePoints = fullRoutePoints,
                    liveLocationLat = liveLocationLat,
                    liveLocationLng = liveLocationLng,
                    defaultLat = viewModel.defaultLatitude,
                    defaultLng = viewModel.defaultLongitude,
                    defaultZoom = viewModel.defaultZoom,
                    onMarkStopVisited = { stopId -> viewModel.markStopCompleted(stopId) }
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
    dailyStops: List<ItineraryStop>,
    healthFacilities: List<HealthFacility>,
    fullRoutePoints: List<Pair<Double, Double>>,
    liveLocationLat: Double,
    liveLocationLng: Double,
    defaultLat: Double,
    defaultLng: Double,
    defaultZoom: Float,
    onMarkStopVisited: (String) -> Unit = {}
) {
    // Use live location as initial camera position if available, otherwise default
    val initialLat = if (liveLocationLat != 0.0) liveLocationLat else defaultLat
    val initialLng = if (liveLocationLng != 0.0) liveLocationLng else defaultLng
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), 14f)
    }
    val routeLatLngs = fullRoutePoints.map { LatLng(it.first, it.second) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = remember(hasLocationPermission) {
                    MapProperties(isMyLocationEnabled = hasLocationPermission)
                },
                uiSettings = remember {
                    MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true
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
                        snippet = if (stop.completed) "Visited" else stop.address,
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            if (stop.completed)
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                            else
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
                // Health facility markers (blue/violet for hospitals)
                healthFacilities.forEach { facility ->
                    Marker(
                        state = MarkerState(position = LatLng(facility.latitude, facility.longitude)),
                        title = facility.name,
                        snippet = facility.servicesAvailable.take(3).joinToString(", "),
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                        )
                    )
                }
            }
        }

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
                Spacer(modifier = Modifier.height(12.dp))

                if (dailyStops.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_stops_for_today),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(dailyStops) { stop ->
                            ItineraryStopRow(
                                stop = stop,
                                onMarkVisited = { onMarkStopVisited(stop.id) },
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

@Composable
private fun ItineraryStopRow(
    stop: ItineraryStop,
    onMarkVisited: () -> Unit,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val bgColor = if (stop.completed) {
        Color(0xFF438894).copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Order number / checkmark
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (stop.completed) Color(0xFF438894) else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    text = if (stop.completed) "✓" else "${stop.order}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (stop.completed) Color(0xFF254D4D) else MaterialTheme.colorScheme.onSurface
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
                Badge(containerColor = Color(0xFF438894)) {
                    Text(
                        text = "Visited",
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
                onClick = {
                    val uri = Uri.parse("google.navigation:q=${stop.latitude},${stop.longitude}&mode=d")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        // Fallback to browser if Google Maps not installed
                        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${stop.latitude},${stop.longitude}&travelmode=driving")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.weight(1f).height(34.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Directions", fontSize = 12.sp)
            }

            if (!stop.completed) {
                FilledTonalButton(
                    onClick = onMarkVisited,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("Mark Visited", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HealthFacilityCard(facility: HealthFacility) {
    val context = LocalContext.current
    val facilityTypeLabel = when (facility.type) {
        com.example.afyaquest.domain.model.FacilityType.HOSPITAL -> stringResource(R.string.facility_type_hospital)
        com.example.afyaquest.domain.model.FacilityType.CLINIC -> stringResource(R.string.facility_type_clinic)
        com.example.afyaquest.domain.model.FacilityType.HEALTH_CENTER -> stringResource(R.string.facility_type_health_center)
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
                onClick = {
                    val uri = Uri.parse("google.navigation:q=${facility.latitude},${facility.longitude}&mode=d")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${facility.latitude},${facility.longitude}&travelmode=driving")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Get Directions", fontSize = 13.sp)
            }
        }
    }
}

