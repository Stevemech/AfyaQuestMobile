package com.afyaquest.app.data.repository

import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.ItinerariesResponse
import com.afyaquest.app.data.remote.dto.ItineraryDto
import com.afyaquest.app.data.remote.dto.ItineraryStopDto
import com.afyaquest.app.domain.model.ItineraryStop
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate

/**
 * Today's route: which itinerary counts as "today", what is kept on the phone, and what the map
 * falls back to without a connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ItineraryRepositoryTest {

    @Mock private lateinit var apiService: ApiService
    @Mock private lateinit var tokenManager: TokenManager
    @Mock private lateinit var progressDataStore: ProgressDataStore

    private lateinit var repository: ItineraryRepository

    private val today = DateUtils.todayIso()
    private val tomorrow = LocalDate.now().plusDays(1).toString()

    private fun stop(order: Int, houseId: String?, completed: Boolean = false) = ItineraryStopDto(
        order = order,
        houseId = houseId,
        label = "Household $order",
        address = "Kibera",
        latitude = -1.31,
        longitude = 36.78,
        completed = completed
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(tokenManager.getIdToken()).thenReturn("token")
        repository = ItineraryRepository(apiService, tokenManager, progressDataStore)
    }

    private fun serverReturns(vararg itineraries: ItineraryDto) = runBlocking {
        whenever(apiService.getItineraries("Bearer token"))
            .thenReturn(Response.success(ItinerariesResponse(itineraries.toList())))
    }

    @Test
    fun `only today's itinerary counts as the stops for the day`() = runBlocking<Unit> {
        serverReturns(
            ItineraryDto(date = tomorrow, stops = listOf(stop(1, "T-1"), stop(2, "T-2")), status = "active"),
            ItineraryDto(date = today, stops = listOf(stop(1, "H-1"), stop(2, "H-2"), stop(3, "H-3")), status = "active")
        )

        val stops = repository.refreshToday()!!

        assertEquals(listOf("H-1", "H-2", "H-3"), stops.map { it.id })
        verify(progressDataStore).setItineraryTotal(eq(3), any())
    }

    @Test
    fun `a day without an itinerary is an empty route, not an error`() = runBlocking<Unit> {
        serverReturns(ItineraryDto(date = tomorrow, stops = listOf(stop(1, "T-1")), status = "active"))

        assertTrue(repository.todayStops()!!.isEmpty())
        verify(progressDataStore).setItineraryTotal(eq(0), any())
    }

    @Test
    fun `the downloaded route is kept on the phone`() = runBlocking<Unit> {
        serverReturns(ItineraryDto(date = today, stops = listOf(stop(1, "H-1")), status = "active"))

        repository.refreshToday()

        val saved = argumentCaptor<String>()
        verify(progressDataStore).cacheItinerary(saved.capture(), any())
        val restored = Gson().fromJson(saved.firstValue, Array<ItineraryStop>::class.java)
        assertEquals("H-1", restored.single().id)
        assertEquals("Household 1", restored.single().label)
    }

    @Test
    fun `visits the server already knows about are marked on this phone`() = runBlocking<Unit> {
        serverReturns(
            ItineraryDto(
                date = today,
                stops = listOf(stop(1, "H-1", completed = true), stop(2, "H-2"), stop(3, "H-3", completed = true)),
                status = "active"
            )
        )

        repository.refreshToday()

        verify(progressDataStore).markStopsCompleted(eq(listOf("H-1", "H-3")), any())
    }

    @Test
    fun `nothing is marked when the server reports no visits`() = runBlocking<Unit> {
        serverReturns(ItineraryDto(date = today, stops = listOf(stop(1, "H-1")), status = "active"))

        repository.refreshToday()

        verify(progressDataStore, never()).markStopsCompleted(any(), any())
    }

    @Test
    fun `a stop without a house id still gets a stable id`() = runBlocking<Unit> {
        serverReturns(ItineraryDto(date = today, stops = listOf(stop(1, ""), stop(2, null)), status = "active"))

        assertEquals(listOf("stop-1", "stop-2"), repository.refreshToday()!!.map { it.id })
    }

    @Test
    fun `without a connection the route saved earlier today is used`() = runBlocking<Unit> {
        whenever(apiService.getItineraries(any())).thenAnswer { throw java.io.IOException("offline") }
        val saved = listOf(ItineraryStop(1, "H-1", "Household 1", "Kibera", -1.31, 36.78))
        whenever(progressDataStore.getCachedItinerary(any())).thenReturn(Gson().toJson(saved))

        assertNull(repository.refreshToday())
        assertEquals(saved, repository.todayStops())
        // A failed download must not wipe what Home knows about today's route.
        verify(progressDataStore, never()).setItineraryTotal(any(), any())
    }

    @Test
    fun `with no connection and no saved route there is nothing to show`() = runBlocking<Unit> {
        whenever(apiService.getItineraries(any())).thenReturn(Response.error(500, okhttp3.ResponseBody.create(null, "")))
        whenever(progressDataStore.getCachedItinerary(any())).thenReturn(null)

        assertNull(repository.todayStops())
    }

    @Test
    fun `a damaged saved route is ignored instead of crashing the map`() = runBlocking<Unit> {
        whenever(apiService.getItineraries(any())).thenAnswer { throw java.io.IOException("offline") }
        whenever(progressDataStore.getCachedItinerary(any())).thenReturn("{not json")

        assertNull(repository.todayStops())
    }

    @Test
    fun `nobody signed in means no request is made`() = runBlocking<Unit> {
        whenever(tokenManager.getIdToken()).thenReturn(null)
        whenever(progressDataStore.getCachedItinerary(any())).thenReturn(null)

        assertNull(repository.todayStops())
        verify(apiService, never()).getItineraries(any())
    }
}
