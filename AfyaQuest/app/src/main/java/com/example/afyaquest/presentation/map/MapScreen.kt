package com.example.afyaquest.presentation.map

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.afyaquest.domain.model.ClientHouse
import com.example.afyaquest.domain.model.HealthFacility
import com.example.afyaquest.domain.model.ItineraryStop
import com.example.afyaquest.domain.model.VisitStatus
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

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
    val clientHouses by viewModel.clientHouses.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val filteredClients = remember(clientHouses, statusFilter) {
        if (statusFilter == null) clientHouses
        else clientHouses.filter { it.status == statusFilter }
    }
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

    var showClientDetails by remember { mutableStateOf<ClientHouse?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    showClientDetails?.let { client ->
        ClientDetailsDialog(
            client = client,
            onDismiss = { showClientDetails = null },
            onMarkVisited = {
                viewModel.markClientAsVisited(client.id)
                showClientDetails = null
            }
        )
    }

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
                    text = { Text(stringResource(R.string.client_stops)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.health_facilities)) },
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> MapAndItineraryTab(
                    dailyStops = dailyStops,
                    fullRoutePoints = fullRoutePoints,
                    liveLocationLat = liveLocationLat,
                    liveLocationLng = liveLocationLng,
                    defaultLat = viewModel.defaultLatitude,
                    defaultLng = viewModel.defaultLongitude,
                    defaultZoom = viewModel.defaultZoom
                )
                1 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = statusFilter == null,
                                onClick = { viewModel.setStatusFilter(null) },
                                label = { Text(stringResource(R.string.all)) }
                            )
                            FilterChip(
                                selected = statusFilter == VisitStatus.TO_VISIT,
                                onClick = { viewModel.setStatusFilter(VisitStatus.TO_VISIT) },
                                label = { Text(stringResource(R.string.to_visit)) }
                            )
                            FilterChip(
                                selected = statusFilter == VisitStatus.VISITED,
                                onClick = { viewModel.setStatusFilter(VisitStatus.VISITED) },
                                label = { Text(stringResource(R.string.visited)) }
                            )
                            FilterChip(
                                selected = statusFilter == VisitStatus.SCHEDULED,
                                onClick = { viewModel.setStatusFilter(VisitStatus.SCHEDULED) },
                                label = { Text(stringResource(R.string.scheduled)) }
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredClients) { client ->
                                ClientHouseCard(
                                    client = client,
                                    onClick = { showClientDetails = client }
                                )
                            }
                        }
                    }
                }
                2 -> LazyColumn(
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
    fullRoutePoints: List<Pair<Double, Double>>,
    liveLocationLat: Double,
    liveLocationLng: Double,
    defaultLat: Double,
    defaultLng: Double,
    defaultZoom: Float
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(defaultLat, defaultLng), defaultZoom)
    }
    val routeLatLngs = fullRoutePoints.map { LatLng(it.first, it.second) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map preview (in-app) with live location and route
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = remember { MapProperties() },
                uiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }
            ) {
                // Route: live location → stop 1 → stop 2 → …
                routeLatLngs.takeIf { it.size >= 2 }?.let { points ->
                    Polyline(
                        points = points,
                        color = MaterialTheme.colorScheme.primary,
                        width = 12f,
                        geodesic = true
                    )
                }
                // Live location marker: real device GPS (Fused Location Provider) or default until first fix
                Marker(
                    state = MarkerState(position = LatLng(liveLocationLat, liveLocationLng)),
                    title = stringResource(R.string.you_are_here),
                    snippet = stringResource(R.string.your_location),
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_CYAN
                    )
                )
                dailyStops.forEach { stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                        title = "${stop.order}. ${stop.label}",
                        snippet = stop.address
                    )
                }
            }
        }

        // "These are your stops for the day, do them in this order"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Take remaining space so the list can scroll instead of being cut off.
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
                            ItineraryStopRow(stop = stop)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.map_location_disclaimer),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ItineraryStopRow(stop: ItineraryStop) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                text = "${stop.order}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
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
    }
}

@Composable
fun ClientHouseCard(
    client: ClientHouse,
    onClick: () -> Unit
) {
    val statusColor = when (client.status) {
        VisitStatus.TO_VISIT -> Color(0xFFFF9800)
        VisitStatus.VISITED -> Color(0xFF4CAF50)
        VisitStatus.SCHEDULED -> Color(0xFF2196F3)
    }

    val statusText = when (client.status) {
        VisitStatus.TO_VISIT -> stringResource(R.string.to_visit)
        VisitStatus.VISITED -> stringResource(R.string.visited)
        VisitStatus.SCHEDULED -> stringResource(R.string.scheduled)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.clientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = client.address,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                client.description?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Status badge
                    Badge(
                        containerColor = statusColor,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    // Distance badge
                    client.distance?.let { distance ->
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = "📍 ${distance}km",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthFacilityCard(facility: HealthFacility) {
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
        }
    }
}

@Composable
fun ClientDetailsDialog(
    client: ClientHouse,
    onDismiss: () -> Unit,
    onMarkVisited: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(client.clientName) },
        text = {
            Column {
                Text("${stringResource(R.string.address)}: ${client.address}")
                Spacer(modifier = Modifier.height(8.dp))

                client.description?.let {
                    Text("${stringResource(R.string.description)}: $it")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                client.distance?.let {
                    Text("${stringResource(R.string.distance)}: ${it}km")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (client.status) {
                    VisitStatus.VISITED -> {
                        client.lastVisit?.let {
                            Text("${stringResource(R.string.last_visit)}: $it")
                        }
                    }
                    VisitStatus.SCHEDULED -> {
                        client.nextVisit?.let {
                            Text("${stringResource(R.string.next_visit)}: $it")
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (client.status != VisitStatus.VISITED) {
                Button(onClick = onMarkVisited) {
                    Text(stringResource(R.string.mark_as_visited))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}