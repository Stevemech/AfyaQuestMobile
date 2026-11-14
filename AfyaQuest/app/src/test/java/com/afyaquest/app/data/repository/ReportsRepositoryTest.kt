package com.afyaquest.app.data.repository

import com.afyaquest.app.data.local.dao.ReportDao
import com.afyaquest.app.data.local.entity.ReportEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

/** Restoring the report history of an account on a phone that does not have it yet. */
class ReportsRepositoryTest {

    @Mock private lateinit var reportDao: ReportDao
    @Mock private lateinit var syncManager: SyncManager
    @Mock private lateinit var tokenManager: TokenManager
    @Mock private lateinit var progressDataStore: ProgressDataStore
    @Mock private lateinit var apiService: ApiService

    private lateinit var repository: ReportsRepository

    // Numbers arrive from JSON as doubles.
    private val uploaded = listOf<Map<String, Any>>(
        mapOf(
            "id" to "r-2", "date" to "2026-11-12", "patientsVisited" to 6.0, "vaccinationsGiven" to 2.0,
            "healthEducation" to "Nutrition", "challenges" to "Rain", "notes" to ""
        ),
        mapOf("id" to "r-1", "date" to "2026-11-11", "patientsVisited" to 4.0, "vaccinationsGiven" to 0.0)
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(tokenManager.getUserId()).thenReturn("user-1")
        whenever(tokenManager.getIdToken()).thenReturn("token")
        repository = ReportsRepository(reportDao, syncManager, tokenManager, progressDataStore, apiService)
    }

    @Test
    fun `a new phone gets the reports the account already uploaded`() = runBlocking<Unit> {
        whenever(reportDao.getReportsByUser("user-1")).thenReturn(emptyList())
        whenever(apiService.getAllReports("Bearer token")).thenReturn(Response.success(uploaded))

        assertEquals(2, repository.restoreFromServerIfEmpty())

        val saved = argumentCaptor<ReportEntity>()
        verify(reportDao, times(2)).insertReport(saved.capture())
        val today = saved.firstValue
        assertEquals("r-2", today.id)
        assertEquals("user-1", today.userId)
        assertEquals("2026-11-12", today.date)
        assertEquals(6, today.patientsVisited)
        assertEquals(2, today.vaccinationsGiven)
        assertEquals("Nutrition", today.healthEducation)
        assertEquals("Rain", today.challenges)
        // Already on the server: must not show as "Pending Sync" or be uploaded again.
        assertTrue(saved.allValues.all { it.isSynced })
        // Missing optional fields do not break the restore.
        assertEquals("", saved.secondValue.healthEducation)
        // The days count towards the account's progress (achievements, quick stats).
        verify(progressDataStore).markReportDate("2026-11-12")
        verify(progressDataStore).markReportDate("2026-11-11")
    }

    @Test
    fun `a phone that already has reports is left alone`() = runBlocking<Unit> {
        whenever(reportDao.getReportsByUser("user-1"))
            .thenReturn(listOf(ReportEntity(id = "local", userId = "user-1", date = "2026-11-12")))

        assertEquals(0, repository.restoreFromServerIfEmpty())

        // Not even asked for: a report deleted from the history must not come back.
        verify(apiService, never()).getAllReports(any())
        verify(reportDao, never()).insertReport(any())
    }

    @Test
    fun `nothing is restored without a connection or when nobody is signed in`() = runBlocking<Unit> {
        whenever(reportDao.getReportsByUser("user-1")).thenReturn(emptyList())
        whenever(apiService.getAllReports(any())).thenAnswer { throw java.io.IOException("offline") }
        assertEquals(0, repository.restoreFromServerIfEmpty())

        whenever(tokenManager.getIdToken()).thenReturn(null)
        assertEquals(0, repository.restoreFromServerIfEmpty())

        verify(reportDao, never()).insertReport(any())
    }

    @Test
    fun `a server row without a date is skipped`() = runBlocking<Unit> {
        whenever(reportDao.getReportsByUser("user-1")).thenReturn(emptyList())
        whenever(apiService.getAllReports("Bearer token")).thenReturn(
            Response.success(listOf(mapOf<String, Any>("id" to "broken"), uploaded[0]))
        )

        assertEquals(1, repository.restoreFromServerIfEmpty())
        verify(reportDao, times(1)).insertReport(any())
    }

    // ---- deleting and re-submitting ----

    @Test
    fun `a day that was already reported stays reported after its report is deleted`() = runBlocking<Unit> {
        // The saved report is gone, but the account's record of report days still has the date.
        whenever(reportDao.getReportByUserAndDate("user-1", "2026-11-12")).thenReturn(null)
        whenever(progressDataStore.getReportDates()).thenReturn(kotlinx.coroutines.flow.flowOf(setOf("2026-11-12")))

        // So a second report that day earns no second XP.
        assertTrue(repository.hasReportForToday("2026-11-12"))
        assertEquals(false, repository.hasReportForToday("2026-11-13"))
    }

    @Test
    fun `deleting a report that was not uploaded yet also cancels its upload`() = runBlocking<Unit> {
        whenever(reportDao.getReportById("r-local"))
            .thenReturn(ReportEntity(id = "r-local", userId = "user-1", date = "2026-11-12", isSynced = false))

        repository.deleteReport("r-local")

        verify(reportDao).deleteReportById("r-local")
        verify(syncManager).dropQueuedReport("user-1", "2026-11-12")
    }

    @Test
    fun `deleting an uploaded report leaves the queue alone`() = runBlocking<Unit> {
        whenever(reportDao.getReportById("r-sent"))
            .thenReturn(ReportEntity(id = "r-sent", userId = "user-1", date = "2026-11-12", isSynced = true))

        repository.deleteReport("r-sent")

        verify(reportDao).deleteReportById("r-sent")
        verify(syncManager, never()).dropQueuedReport(any(), any())
    }
}
