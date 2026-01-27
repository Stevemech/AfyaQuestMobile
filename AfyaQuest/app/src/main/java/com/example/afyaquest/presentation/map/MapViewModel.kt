package com.example.afyaquest.presentation.map

import androidx.lifecycle.ViewModel
import com.example.afyaquest.domain.model.ClientHouse
import com.example.afyaquest.domain.model.FacilityType
import com.example.afyaquest.domain.model.HealthFacility
import com.example.afyaquest.domain.model.VisitStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Map screen
 * Manages health facilities and client houses data
 */
@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    // Default location: Kajiado Airport, Kenya
    val defaultLatitude = -1.8581
    val defaultLongitude = 36.9823

    private val _healthFacilities = MutableStateFlow<List<HealthFacility>>(emptyList())
    val healthFacilities: StateFlow<List<HealthFacility>> = _healthFacilities.asStateFlow()

    private val _clientHouses = MutableStateFlow<List<ClientHouse>>(emptyList())
    val clientHouses: StateFlow<List<ClientHouse>> = _clientHouses.asStateFlow()

    private val _selectedClient = MutableStateFlow<ClientHouse?>(null)
    val selectedClient: StateFlow<ClientHouse?> = _selectedClient.asStateFlow()

    private val _statusFilter = MutableStateFlow<VisitStatus?>(null)
    val statusFilter: StateFlow<VisitStatus?> = _statusFilter.asStateFlow()

    init {
        loadMapData()
    }

    /**
     * Load health facilities and client houses
     * In production, this would fetch from API
     */
    private fun loadMapData() {
        // Health facilities data
        _healthFacilities.value = listOf(
            HealthFacility(
                id = "1",
                name = "Kajiado Referral Hospital",
                type = FacilityType.HOSPITAL,
                latitude = -1.8522,
                longitude = 36.7820,
                servicesAvailable = listOf("Emergency", "Maternity", "Pediatrics", "Surgery", "Outpatient", "Laboratory"),
                distance = 20.3
            ),
            HealthFacility(
                id = "2",
                name = "AIC Kajiado Hospital",
                type = FacilityType.HOSPITAL,
                latitude = -1.8489,
                longitude = 36.7845,
                servicesAvailable = listOf("Emergency", "Maternity", "Pediatrics", "Outpatient", "X-Ray"),
                distance = 19.8
            ),
            HealthFacility(
                id = "3",
                name = "Kitengela Sub-County Hospital",
                type = FacilityType.HOSPITAL,
                latitude = -1.4737,
                longitude = 36.9532,
                servicesAvailable = listOf("Emergency", "Maternity", "Surgery", "Laboratory", "Pharmacy"),
                distance = 42.7
            ),
            HealthFacility(
                id = "4",
                name = "Kajiado Airport Dispensary",
                type = FacilityType.CLINIC,
                latitude = -1.8595,
                longitude = 36.9801,
                servicesAvailable = listOf("Primary Care", "Vaccination", "Family Planning"),
                distance = 0.3
            )
        )

        // Client houses data
        _clientHouses.value = listOf(
            ClientHouse(
                id = "1",
                address = "123 Airport Road",
                clientName = "Selina Nkoya Family",
                latitude = -1.8623,
                longitude = 36.9789,
                status = VisitStatus.TO_VISIT,
                distance = 0.5,
                description = "Maternal health check-up needed"
            ),
            ClientHouse(
                id = "2",
                address = "45 Bissil Road",
                clientName = "Ole Sankale Household",
                latitude = -1.8712,
                longitude = 36.9901,
                status = VisitStatus.TO_VISIT,
                distance = 1.8,
                description = "Child vaccination due"
            ),
            ClientHouse(
                id = "3",
                address = "78 Mashuuru Village",
                clientName = "Grace Nasieku",
                latitude = -1.8753,
                longitude = 36.8201,
                status = VisitStatus.VISITED,
                lastVisit = "2 days ago",
                distance = 16.7,
                description = "Follow-up completed"
            ),
            ClientHouse(
                id = "4",
                address = "22 Kajiado Town",
                clientName = "David Lekishon Family",
                latitude = -1.8512,
                longitude = 36.7798,
                status = VisitStatus.SCHEDULED,
                nextVisit = "Tomorrow 10:00 AM",
                distance = 20.1,
                description = "Family planning consultation"
            ),
            ClientHouse(
                id = "5",
                address = "89 Oloosirkon Area",
                clientName = "Peter Kisemei Household",
                latitude = -1.7901,
                longitude = 36.9267,
                status = VisitStatus.TO_VISIT,
                distance = 9.7,
                description = "Hypertension screening needed"
            ),
            ClientHouse(
                id = "6",
                address = "34 Magadi Road",
                clientName = "Mary Ole Sankale",
                latitude = -1.9012,
                longitude = 37.0123,
                status = VisitStatus.SCHEDULED,
                nextVisit = "Friday 2:00 PM",
                distance = 6.2,
                description = "Prenatal care appointment"
            )
        )
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
                    lastVisit = "Just now"
                )
            } else {
                client
            }
        }
    }
}
