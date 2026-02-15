package com.example.afyaquest.domain.model

/**
 * Health facility marker on map
 */
data class HealthFacility(
    val id: String,
    val name: String,
    val type: FacilityType,
    val latitude: Double,
    val longitude: Double,
    val servicesAvailable: List<String>,
    val distance: Double? = null
)

enum class FacilityType {
    HOSPITAL,
    CLINIC,
    HEALTH_CENTER
}

/**
 * Client house marker on map
 */
data class ClientHouse(
    val id: String,
    val address: String,
    val clientName: String,
    val latitude: Double,
    val longitude: Double,
    val status: VisitStatus,
    val lastVisit: String? = null,
    val nextVisit: String? = null,
    val distance: Double? = null,
    val description: String? = null
)

enum class VisitStatus {
    TO_VISIT,
    VISITED,
    SCHEDULED
}

/**
 * A single stop in the daily itinerary (ordered).
 * Used to show "these are your stops for the day, do them in this order".
 */
data class ItineraryStop(
    val order: Int,
    val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null
)
