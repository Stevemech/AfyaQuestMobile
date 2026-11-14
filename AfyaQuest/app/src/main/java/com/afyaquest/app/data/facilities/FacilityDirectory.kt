package com.afyaquest.app.data.facilities

import com.afyaquest.app.domain.model.FacilityType
import com.afyaquest.app.domain.model.HealthFacility
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One area the app is used in, with the health facilities a CHV there refers people to. */
data class FacilityRegion(
    val id: String,
    val name: String,
    /** Organization ids (as on the user profile) that work in this region. */
    val organizations: List<String>,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val facilities: List<HealthFacility>
)

/**
 * The bundled health-facility directory (`assets/facilities/facility_directory.json`).
 *
 * Like the triage tree it is data, not code: adding a region or correcting a facility is an edit
 * to the JSON. It is bundled rather than fetched so the list and "Get directions" work offline.
 * Parsing is done by hand (no reflection), so release builds need no keep rules for it.
 */
class FacilityDirectory(val regions: List<FacilityRegion>) {

    /**
     * The region a user should see: the one their [organization] works in; failing that the one
     * nearest to where they are (within [NEAREST_REGION_MAX_KM]); failing that the first region.
     * Null only for an empty directory.
     */
    fun regionFor(organization: String?, latitude: Double? = null, longitude: Double? = null): FacilityRegion? {
        if (!organization.isNullOrBlank()) {
            regions.firstOrNull { organization in it.organizations }?.let { return it }
        }
        if (latitude != null && longitude != null) {
            regions
                .map { it to distanceKm(latitude, longitude, it.centerLatitude, it.centerLongitude) }
                .filter { it.second <= NEAREST_REGION_MAX_KM }
                .minByOrNull { it.second }
                ?.let { return it.first }
        }
        return regions.firstOrNull()
    }

    companion object {
        const val NEAREST_REGION_MAX_KM = 150.0
        private const val EARTH_RADIUS_KM = 6371.0

        fun parse(json: String): FacilityDirectory {
            val root = JsonParser.parseString(json).asJsonObject
            val regions = root.getAsJsonArray("regions").map { it.asJsonObject }.map { region ->
                val center = region.getAsJsonObject("center")
                FacilityRegion(
                    id = region.get("id").asString,
                    name = region.get("name").asString,
                    organizations = region.getAsJsonArray("organizations").map { it.asString },
                    centerLatitude = center.get("lat").asDouble,
                    centerLongitude = center.get("lng").asDouble,
                    facilities = region.getAsJsonArray("facilities").map { parseFacility(it.asJsonObject) }
                )
            }
            return FacilityDirectory(regions)
        }

        private fun parseFacility(facility: JsonObject) = HealthFacility(
            id = facility.get("id").asString,
            name = facility.get("name").asString,
            type = when (facility.get("type").asString) {
                "hospital" -> FacilityType.HOSPITAL
                "health_center" -> FacilityType.HEALTH_CENTER
                else -> FacilityType.CLINIC
            },
            latitude = facility.get("lat").asDouble,
            longitude = facility.get("lng").asDouble,
            servicesAvailable = facility.getAsJsonArray("services")?.map { it.asString } ?: emptyList()
        )

        /** Great-circle distance in kilometres (haversine). */
        fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
            return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
        }

        /**
         * [facilities] with their distance from the given point filled in, nearest first. Without
         * a position the list is returned in directory order with no distances.
         */
        fun withDistances(facilities: List<HealthFacility>, latitude: Double?, longitude: Double?): List<HealthFacility> {
            if (latitude == null || longitude == null) return facilities.map { it.copy(distance = null) }
            return facilities
                .map { it.copy(distance = distanceKm(latitude, longitude, it.latitude, it.longitude)) }
                .sortedBy { it.distance }
        }
    }
}
