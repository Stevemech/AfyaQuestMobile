package com.example.afyaquest.sync

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.afyaquest.data.local.dao.PendingSyncDao
import com.example.afyaquest.data.local.dao.ReportDao
import com.example.afyaquest.data.local.entity.PendingReportEntity
import com.example.afyaquest.data.local.entity.PendingQuizEntity
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.util.NetworkMonitor
import com.example.afyaquest.util.TokenManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SyncManager
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SyncManagerTest {

    private lateinit var context: Context

    @Mock
    private lateinit var pendingSyncDao: PendingSyncDao

    @Mock
    private lateinit var networkMonitor: NetworkMonitor

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var tokenManager: TokenManager

    @Mock
    private lateinit var reportDao: ReportDao

    private lateinit var syncManager: SyncManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // Mock flow responses
        whenever(pendingSyncDao.getUnsyncedReportsCount()).thenReturn(flowOf(0))
        whenever(pendingSyncDao.getUnsyncedQuizzesCount()).thenReturn(flowOf(0))
        whenever(pendingSyncDao.getUnsyncedChatsCount()).thenReturn(flowOf(0))
        whenever(pendingSyncDao.getUnsyncedClientVisitsCount()).thenReturn(flowOf(0))

        syncManager = SyncManager(context, pendingSyncDao, networkMonitor, apiService, tokenManager, reportDao)
    }

    @Test
    fun `queueReport inserts report to database`() = runBlocking {
        val report = PendingReportEntity(
            userId = "user1",
            date = "2024-01-27",
            patientsVisited = 5,
            vaccinationsGiven = 10,
            healthEducation = "2 sessions",
            challenges = "Test challenges",
            notes = "Test notes"
        )

        whenever(pendingSyncDao.insertPendingReport(report)).thenReturn(1L)

        val id = syncManager.queueReport(report)

        assertEquals(1L, id)
        verify(pendingSyncDao).insertPendingReport(report)
    }

    @Test
    fun `queueQuiz inserts quiz to database`() = runBlocking {
        val quiz = PendingQuizEntity(
            userId = "user1",
            questionId = "q1",
            selectedAnswer = 2,
            isCorrect = true,
            pointsEarned = 30,
            livesChange = 2
        )

        whenever(pendingSyncDao.insertPendingQuiz(quiz)).thenReturn(1L)

        val id = syncManager.queueQuiz(quiz)

        assertEquals(1L, id)
        verify(pendingSyncDao).insertPendingQuiz(quiz)
    }

    @Test
    fun `syncReports returns count of synced items`() = runBlocking {
        val reports = listOf(
            PendingReportEntity(
                id = 1,
                userId = "user1",
                date = "2024-01-27",
                patientsVisited = 5,
                vaccinationsGiven = 10,
                healthEducation = "2 sessions",
                challenges = "Test",
                notes = "Test"
            )
        )

        whenever(pendingSyncDao.getUnsyncedReports()).thenReturn(reports)
        whenever(tokenManager.getIdToken()).thenReturn("test-token")

        val count = syncManager.syncReports()

        assertEquals(1, count)
        verify(pendingSyncDao).markReportSynced(1)
    }

    @Test
    fun `syncReports returns 0 when no unsynced items`() = runBlocking {
        whenever(pendingSyncDao.getUnsyncedReports()).thenReturn(emptyList())
        whenever(tokenManager.getIdToken()).thenReturn("test-token")

        val count = syncManager.syncReports()

        assertEquals(0, count)
    }
}
