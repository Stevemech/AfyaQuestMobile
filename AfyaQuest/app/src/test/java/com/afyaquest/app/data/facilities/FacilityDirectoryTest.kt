package com.afyaquest.app.data.facilities

import com.afyaquest.app.domain.model.FacilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the bundled directory itself (the JSON is data that people will edit) and the lookups. */
class FacilityDirectoryTest {

    // Unit tests run with the module directory as working directory.
    private val directory = FacilityDirectory.parse(
        File("src/main/assets/facilities/facility_directory.json").readText()
    )

    /** Service keys the Map screen knows how to name (MapScreen.serviceLabel). */
    private val knownServices = setOf(
        "emergency", "maternity", "pediatrics", "surgery", "outpatient", "laboratory", "xray",
        "primary_care", "vaccination", "family_planning", "pharmacy", "maternal_health", "nutrition",
        "mental_health"
    )

    @Test
    fun `the bundled directory is well formed`() {
        assertTrue(directory.regions.isNotEmpty())
        val regionIds = directory.regions.map { it.id }
        assertEquals(regionIds.size, regionIds.toSet().size)
        val facilityIds = directory.regions.flatMap { it.facilities }.map { it.id }
        assertEquals("facility ids must be unique", facilityIds.size, facilityIds.toSet().size)

        directory.regions.forEach { region ->
            assertTrue("${region.id} has no facilities", region.facilities.isNotEmpty())
            region.facilities.forEach { facility ->
                assertTrue("${facility.id} has no name", facility.name.isNotBlank())
                assertTrue("${facility.id} latitude", facility.latitude in -90.0..90.0)
                assertTrue("${facility.id} longitude", facility.longitude in -180.0..180.0)
                // A facility listed for a region must actually be in or near it.
                val fromCentre = FacilityDirectory.distanceKm(
                    region.centerLatitude, region.centerLongitude, facility.latitude, facility.longitude
                )
                assertTrue("${facility.id} is ${fromCentre.toInt()} km from ${region.id}", fromCentre < 25)
                facility.servicesAvailable.forEach { service ->
                    assertTrue("${facility.id} lists unknown service '$service'", service in knownServices)
                }
            }
        }
    }

    @Test
    fun `each organization gets the facilities of its own area`() {
        val kibera = directory.regionFor("kibera-east")!!
        assertEquals("nairobi-kibera", kibera.id)
        assertTrue(kibera.facilities.any { it.name == "Kenyatta National Hospital" && it.type == FacilityType.HOSPITAL })
        assertTrue(kibera.facilities.any { it.name == "Kibera D.O. Health Centre" && it.type == FacilityType.HEALTH_CENTER })

        val mathare = directory.regionFor("mathare")!!
        assertEquals("nairobi-mathare", mathare.id)
        assertTrue(mathare.facilities.any { it.name == "Pumwani Maternity Hospital" })

        // Kenyan organizations never get the Guatemalan list, wherever the phone happens to be.
        assertEquals("nairobi-kibera", directory.regionFor("kibera-east", 14.64, -90.82)!!.id)
    }

    @Test
    fun `an organization without a region gets the region the phone is in`() {
        assertEquals("chimaltenango", directory.regionFor("new-org", 14.65, -90.81)!!.id)
        assertEquals("nairobi-mathare", directory.regionFor(null, -1.2610, 36.8600)!!.id)
        assertEquals("nairobi-kibera", directory.regionFor("", -1.3140, 36.7880)!!.id)
    }

    @Test
    fun `far from every region or with no position the first region is used`() {
        val first = directory.regions.first().id
        assertEquals(first, directory.regionFor("new-org", 37.42, -122.08)!!.id) // California
        assertEquals(first, directory.regionFor(null)!!.id)
        assertNull(FacilityDirectory(emptyList()).regionFor("kibera-east"))
    }

    @Test
    fun `distance is the great-circle distance in kilometres`() {
        // Nairobi to Mombasa is about 440 km in a straight line.
        val nairobiMombasa = FacilityDirectory.distanceKm(-1.2921, 36.8219, -4.0435, 39.6682)
        assertEquals(440.0, nairobiMombasa, 10.0)
        assertEquals(0.0, FacilityDirectory.distanceKm(-1.3, 36.8, -1.3, 36.8), 0.0001)
        // One degree of latitude is about 111 km.
        assertEquals(111.2, FacilityDirectory.distanceKm(0.0, 36.8, 1.0, 36.8), 0.5)
    }

    @Test
    fun `facilities are listed nearest first once the position is known`() {
        val kibera = directory.regionFor("kibera-east")!!
        // Standing at the Kibera D.O. Health Centre.
        val here = kibera.facilities.first { it.id == "ke-kibera-do-hc" }
        val sorted = FacilityDirectory.withDistances(kibera.facilities, here.latitude, here.longitude)

        assertEquals("ke-kibera-do-hc", sorted.first().id)
        assertEquals(0.0, sorted.first().distance!!, 0.001)
        val distances = sorted.map { it.distance!! }
        assertEquals(distances.sorted(), distances)
        // Kenyatta National Hospital is a couple of kilometres away, not hundreds.
        assertTrue(sorted.first { it.id == "ke-knh" }.distance!! in 1.0..4.0)
    }

    @Test
    fun `without a position the directory order is kept and no distance is invented`() {
        val kibera = directory.regionFor("kibera-east")!!
        val listed = FacilityDirectory.withDistances(kibera.facilities, null, null)

        assertEquals(kibera.facilities.map { it.id }, listed.map { it.id })
        assertTrue(listed.all { it.distance == null })
        assertNotNull(listed.firstOrNull())
    }
}
