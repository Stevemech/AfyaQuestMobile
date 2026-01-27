package com.example.afyaquest.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.domain.model.ClientHouse
import com.example.afyaquest.domain.model.HealthFacility
import com.example.afyaquest.domain.model.VisitStatus

/**
 * Map/Itinerary screen
 * Shows list of client houses to visit and health facilities
 * TODO: Integrate Google Maps for visual map display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val healthFacilities by viewModel.healthFacilities.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val filteredClients = viewModel.getFilteredClients()

    var showClientDetails by remember { mutableStateOf<ClientHouse?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // Show client details dialog
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
                title = { Text("Daily Itinerary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Tabs for Client Houses vs Health Facilities
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Client Houses") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Health Facilities") }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Client Houses tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Status filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = statusFilter == null,
                                onClick = { viewModel.setStatusFilter(null) },
                                label = { Text("All") }
                            )
                            FilterChip(
                                selected = statusFilter == VisitStatus.TO_VISIT,
                                onClick = { viewModel.setStatusFilter(VisitStatus.TO_VISIT) },
                                label = { Text("To Visit") }
                            )
                            FilterChip(
                                selected = statusFilter == VisitStatus.VISITED,
                                onClick = { viewModel.setStatusFilter(VisitStatus.VISITED) },
                                label = { Text("Visited") }
                            )
                            FilterChip(
                                selected = statusFilter == VisitStatus.SCHEDULED,
                                onClick = { viewModel.setStatusFilter(VisitStatus.SCHEDULED) },
                                label = { Text("Scheduled") }
                            )
                        }

                        // Client houses list
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
                1 -> {
                    // Health Facilities tab
                    LazyColumn(
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
        VisitStatus.TO_VISIT -> "To Visit"
        VisitStatus.VISITED -> "Visited"
        VisitStatus.SCHEDULED -> "Scheduled"
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
                        text = facility.type.name.replace("_", " "),
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
                text = "Services:",
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
                Text("Address: ${client.address}")
                Spacer(modifier = Modifier.height(8.dp))

                client.description?.let {
                    Text("Description: $it")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                client.distance?.let {
                    Text("Distance: ${it}km")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (client.status) {
                    VisitStatus.VISITED -> {
                        client.lastVisit?.let {
                            Text("Last visit: $it")
                        }
                    }
                    VisitStatus.SCHEDULED -> {
                        client.nextVisit?.let {
                            Text("Next visit: $it")
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (client.status != VisitStatus.VISITED) {
                Button(onClick = onMarkVisited) {
                    Text("Mark as Visited")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
