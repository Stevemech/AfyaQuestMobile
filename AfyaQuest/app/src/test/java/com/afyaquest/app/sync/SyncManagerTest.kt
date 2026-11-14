package com.afyaquest.app.sync

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.afyaquest.app.data.local.dao.ChatMessageDao
import com.afyaquest.app.data.local.dao.PendingSyncDao
import com.afyaquest.app.data.local.dao.ReportDao
import com.afyaquest.app.data.local.entity.ChatMessageEntity
import com.afyaquest.app.data.local.entity.PendingChatEntity
import com.afyaquest.app.data.local.entity.PendingReportEntity
import com.afyaquest.app.data.local.entity.PendingQuizEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.util.NetworkMonitor
import com.afyaquest.app.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

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

    @Mock
    private lateinit var progressSyncManager: ProgressSyncManager

    @Mock
    private lateinit var chatMessageDao: ChatMessageDao

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

        whenever(progressSyncManager.hasUnsyncedChanges).thenReturn(flowOf(false))

        syncManager = SyncManager(
            context, pendingSyncDao, networkMonitor, apiService, tokenManager, reportDao,
            progressSyncManager, chatMessageDao
        )
    }

    @Test
    fun `queueReport inserts report to database`() = runBlocking<Unit> {
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
    fun `queueQuiz inserts quiz to database`() = runBlocking<Unit> {
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
    fun `syncReports returns count of synced items`() = runBlocking<Unit> {
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
        whenever(apiService.createReport(any(), any())).thenReturn(Response.success(emptyMap()))

        val count = syncManager.syncReports()

        assertEquals(1, count)
        verify(apiService).createReport(eq("Bearer test-token"), any())
        verify(pendingSyncDao).markReportSynced(1)
        // Every local report of that day is uploaded now (the server keeps one per day).
        verify(reportDao).markReportsSyncedForDate("user1", "2024-01-27")
    }

    @Test
    fun `syncReports returns 0 when no unsynced items`() = runBlocking<Unit> {
        whenever(pendingSyncDao.getUnsyncedReports()).thenReturn(emptyList())
        whenever(tokenManager.getIdToken()).thenReturn("test-token")

        val count = syncManager.syncReports()

        assertEquals(0, count)
    }

    @Test
    fun `a question asked offline is answered into the conversation when it syncs`() = runBlocking<Unit> {
        val queued = PendingChatEntity(id = 7, userId = "user1", message = "Signs of dehydration?", conversationHistory = "[]")
        whenever(pendingSyncDao.getUnsyncedChats()).thenReturn(listOf(queued))
        whenever(tokenManager.getIdToken()).thenReturn("test-token")
        whenever(apiService.sendChatMessage(eq("Bearer test-token"), any()))
            .thenReturn(Response.success(mapOf("response" to "Look for sunken eyes.", "messageId" to "m1")))

        val count = syncManager.syncChats()

        assertEquals(1, count)
        verify(apiService).sendChatMessage("Bearer test-token", mapOf("message" to "Signs of dehydration?"))
        verify(pendingSyncDao).markChatSynced(7, true, "Look for sunken eyes.")
        verify(chatMessageDao).markQuestionDelivered("user1", "Signs of dehydration?")
        val saved = argumentCaptor<ChatMessageEntity>()
        verify(chatMessageDao).insertMessage(saved.capture())
        assertEquals("assistant", saved.firstValue.role)
        assertEquals("Look for sunken eyes.", saved.firstValue.message)
        assertEquals("user1", saved.firstValue.userId)
    }

    @Test
    fun `a queued question stays queued while the server cannot be reached`() = runBlocking<Unit> {
        val queued = PendingChatEntity(id = 7, userId = "user1", message = "Hello", conversationHistory = "[]")
        whenever(pendingSyncDao.getUnsyncedChats()).thenReturn(listOf(queued))
        whenever(tokenManager.getIdToken()).thenReturn("test-token")
        whenever(apiService.sendChatMessage(any(), any())).thenAnswer { throw java.io.IOException("offline") }

        assertEquals(0, syncManager.syncChats())

        verify(pendingSyncDao, never()).markChatSynced(any(), any(), any())
        verify(chatMessageDao, never()).insertMessage(any())
    }

    // ---- one pass at a time ----

    /** A queue with one chat question that stays "unsynced" until it has been marked as sent. */
    private fun stubOneQueuedQuestion(answerDelayMs: Long = 0) = runBlocking {
        val queued = PendingChatEntity(id = 7, userId = "user1", message = "How much ORS?", conversationHistory = "[]")
        val sent = java.util.concurrent.atomic.AtomicBoolean(false)
        whenever(networkMonitor.isCurrentlyConnected()).thenReturn(true)
        whenever(tokenManager.getIdToken()).thenReturn("test-token")
        whenever(pendingSyncDao.getUnsyncedReports()).thenReturn(emptyList())
        whenever(pendingSyncDao.getUnsyncedQuizzes()).thenReturn(emptyList())
        whenever(pendingSyncDao.getUnsyncedClientVisits()).thenReturn(emptyList())
        whenever(pendingSyncDao.getUnsyncedChats()).thenAnswer { if (sent.get()) emptyList() else listOf(queued) }
        whenever(pendingSyncDao.markChatSynced(any(), any(), any())).thenAnswer { sent.set(true) }
        whenever(progressSyncManager.pushIfDirty()).thenReturn(true)
        whenever(apiService.sendChatMessage(any(), any())).thenAnswer {
            // A slow network: the window in which a second pass used to read the same queue.
            Thread.sleep(answerDelayMs)
            Response.success(mapOf("response" to "About 50-100 ml after each loose stool."))
        }
    }

    @Test
    fun `two passes started together upload a queued question only once`() = runBlocking<Unit> {
        stubOneQueuedQuestion(answerDelayMs = 300)

        val results = listOf(
            async(Dispatchers.Default) { syncManager.runSyncPass() },
            async(Dispatchers.Default) { syncManager.runSyncPass() }
        ).awaitAll()

        // The question reached Fred once and one answer was added to the conversation.
        verify(apiService, times(1)).sendChatMessage(any(), any())
        verify(chatMessageDao, times(1)).insertMessage(any())
        // One pass did the upload; the other waited and found the queue empty.
        assertEquals(listOf(0, 1), results.map { it!!.uploaded }.sorted())
        assertTrue(results.all { it!!.complete })
        assertFalse(syncManager.isSyncing.value)
    }

    @Test
    fun `a pass reports what is still waiting on the phone`() = runBlocking<Unit> {
        stubOneQueuedQuestion()
        whenever(apiService.sendChatMessage(any(), any())).thenAnswer { throw java.io.IOException("timeout") }

        val pass = syncManager.runSyncPass()!!

        assertEquals(0, pass.uploaded)
        assertEquals(1, pass.remaining)
        assertFalse(pass.complete)
        assertNotNull(syncManager.lastSyncError.value)
    }

    @Test
    fun `no pass is attempted without a connection or without a login`() = runBlocking<Unit> {
        stubOneQueuedQuestion()

        whenever(networkMonitor.isCurrentlyConnected()).thenReturn(false)
        assertNull(syncManager.runSyncPass())
        assertEquals(0, syncManager.syncNow())

        whenever(networkMonitor.isCurrentlyConnected()).thenReturn(true)
        whenever(tokenManager.getIdToken()).thenReturn(null)
        assertNull(syncManager.runSyncPass())
        assertFalse(syncManager.isSignedIn())

        verify(apiService, never()).sendChatMessage(any(), any())
    }

    @Test
    fun `the background worker retries until nothing is left on the phone`() {
        val done = SyncPassResult(uploaded = 2, remaining = 0, progressInSync = true)
        assertEquals(ListenableWorker.Result.success(), SyncWorker.resultFor(done, signedIn = true))
        // Nothing queued and nothing to upload is a success too.
        assertEquals(
            ListenableWorker.Result.success(),
            SyncWorker.resultFor(done.copy(uploaded = 0), signedIn = true)
        )
        // Something did not go through: try again later, even if other items were uploaded.
        assertEquals(
            ListenableWorker.Result.retry(),
            SyncWorker.resultFor(done.copy(remaining = 1), signedIn = true)
        )
        assertEquals(
            ListenableWorker.Result.retry(),
            SyncWorker.resultFor(done.copy(progressInSync = false), signedIn = true)
        )
        // No connection: retry. Nobody signed in: nothing to retry for.
        assertEquals(ListenableWorker.Result.retry(), SyncWorker.resultFor(null, signedIn = true))
        assertEquals(ListenableWorker.Result.success(), SyncWorker.resultFor(null, signedIn = false))
    }

    // ---- progress earned offline ----

    private fun immediateSyncRequests(): Int =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork("immediate_sync").get().size

    @Test
    fun `progress waiting for upload asks for a sync as soon as there is a connection`() {
        // Nothing is waiting: no extra work is scheduled (setup() built that manager).
        Thread.sleep(300)
        assertEquals(0, immediateSyncRequests())

        // XP earned offline: there is no queue row for it, so the manager schedules the pass itself.
        whenever(progressSyncManager.hasUnsyncedChanges).thenReturn(flowOf(true))
        SyncManager(
            context, pendingSyncDao, networkMonitor, apiService, tokenManager, reportDao,
            progressSyncManager, chatMessageDao
        )

        val deadline = System.currentTimeMillis() + 3_000
        while (immediateSyncRequests() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertEquals(1, immediateSyncRequests())
    }

    @Test
    fun `progress that is not uploaded yet counts as one unsynced item`() = runBlocking<Unit> {
        whenever(pendingSyncDao.getUnsyncedReportsCount()).thenReturn(flowOf(2))
        whenever(progressSyncManager.hasUnsyncedChanges).thenReturn(flowOf(true))
        val manager = SyncManager(
            context, pendingSyncDao, networkMonitor, apiService, tokenManager, reportDao,
            progressSyncManager, chatMessageDao
        )

        assertEquals(3, manager.totalUnsyncedCount.first())
    }
}
