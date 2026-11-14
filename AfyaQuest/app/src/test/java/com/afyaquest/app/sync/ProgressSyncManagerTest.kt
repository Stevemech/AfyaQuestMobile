package com.afyaquest.app.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.afyaquest.app.data.local.dao.PendingSyncDao
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.util.LearningProgressSnapshot
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.XpManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.io.IOException

/**
 * Account sync end to end against the real local stores (XP + learning progress DataStores) and
 * a mocked API: what is kept, what is merged, what is uploaded, and what must never leak between
 * accounts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProgressSyncManagerTest {

    @Mock private lateinit var apiService: ApiService
    @Mock private lateinit var tokenManager: TokenManager
    @Mock private lateinit var pendingSyncDao: PendingSyncDao

    private lateinit var xpManager: XpManager
    private lateinit var progressDataStore: ProgressDataStore
    private lateinit var sync: ProgressSyncManager

    @Before
    fun setup(): Unit = runBlocking {
        MockitoAnnotations.openMocks(this@ProgressSyncManagerTest)
        val context: Context = ApplicationProvider.getApplicationContext()
        xpManager = XpManager(context)
        progressDataStore = ProgressDataStore(context)
        // The DataStores are process-wide: start every test from an empty device.
        xpManager.clearAll()
        progressDataStore.clearAll()
        sync = ProgressSyncManager(apiService, tokenManager, xpManager, progressDataStore, pendingSyncDao)
        whenever(tokenManager.getIdToken()).thenReturn("token")
    }

    private fun serverAnswers(state: Map<String, Any>?, profilePoints: Int = 0) = runBlocking<Unit> {
        val body = mutableMapOf<String, Any>(
            "profile" to mapOf("totalPoints" to profilePoints.toDouble(), "lives" to 10.0),
            "progress" to emptyList<Any>()
        )
        if (state != null) body["state"] = state
        whenever(apiService.getUserProgress("Bearer token")).thenReturn(Response.success(body))
    }

    /** The server echoes whatever it is sent as the merged state. */
    private fun serverAcceptsUploads() = runBlocking<Unit> {
        whenever(apiService.updateUserProgress(eq("Bearer token"), any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val sent = invocation.arguments[1] as Map<String, Any>
            Response.success(mapOf("message" to "State synced", "state" to sent["state"]!!))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun uploadedState(): Map<String, Any> = runBlocking {
        val captor = argumentCaptor<Map<String, Any>>()
        verify(apiService).updateUserProgress(eq("Bearer token"), captor.capture())
        assertEquals("state_sync", captor.firstValue["type"])
        captor.firstValue["state"] as Map<String, Any>
    }

    @Test
    fun `a different account signing in never sees the previous account's progress`() = runBlocking<Unit> {
        // Account A used this phone and earned things.
        progressDataStore.setOwnerUserId("user-a")
        xpManager.addXP(300, "account A")
        xpManager.removeLives(4)
        progressDataStore.markVideoWatched("mod1-nervous-system")
        progressDataStore.markLessonCompleted("lesson-1")

        // Account B signs in; the server knows B's own progress.
        whenever(tokenManager.getUserId()).thenReturn("user-b")
        serverAnswers(
            mapOf(
                "totalPoints" to 120.0, "lives" to 7.0, "currentStreak" to 2.0, "lastStreakDate" to "2026-11-10",
                "watchedVideos" to listOf("mod4-antenatal-care"), "completedQuizzes" to emptyList<String>(),
                "completedLessons" to listOf("lesson-3"), "reportDates" to listOf("2026-11-09")
            )
        )
        serverAcceptsUploads()

        assertTrue(sync.onSignedIn("user-b"))

        val xp = xpManager.getSyncState()
        assertEquals(120, xp.totalXP)
        assertEquals(7, xp.lives)
        val progress = progressDataStore.getSnapshot()
        assertEquals(setOf("mod4-antenatal-care"), progress.watchedVideos)
        assertEquals(setOf("lesson-3"), progress.completedLessons)
        assertEquals(setOf("2026-11-09"), progress.reportDates)
        assertEquals("user-b", progressDataStore.getOwnerUserId())
        // Nothing A queued may be uploaded under B.
        verify(pendingSyncDao).deleteItemsOfOtherUsers("user-b")
    }

    @Test
    fun `XP earned offline survives the next sync and is uploaded`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        progressDataStore.setOwnerUserId("user-a")
        xpManager.addXP(200, "earned offline")
        progressDataStore.markVideoWatched("mod2-malnutrition")
        progressDataStore.markDailyQuestionsCompleted(correctAnswers = 2, totalQuestions = 3, xpEarned = 65, date = "2026-11-12")
        progressDataStore.markReportDate("2026-11-12")
        progressDataStore.setStateDirty(true)

        // The server is behind: it only has 150 XP and another video from a second phone.
        serverAnswers(
            mapOf(
                "totalPoints" to 150.0, "lives" to 10.0, "currentStreak" to 0.0, "lastStreakDate" to "",
                "watchedVideos" to listOf("mod1-female-reproductive"), "completedQuizzes" to emptyList<String>(),
                "completedLessons" to emptyList<String>(), "reportDates" to emptyList<String>()
            )
        )
        serverAcceptsUploads()

        assertTrue(sync.syncNow())

        // Local: the larger XP is kept, the id sets are the union.
        assertEquals(200, xpManager.getSyncState().totalXP)
        assertEquals(
            setOf("mod2-malnutrition", "mod1-female-reproductive"),
            progressDataStore.getSnapshot().watchedVideos
        )
        // Uploaded: everything this phone knows.
        val sent = uploadedState()
        assertEquals(200, sent["totalPoints"])
        assertEquals(listOf("mod1-female-reproductive", "mod2-malnutrition"), sent["watchedVideos"])
        assertEquals(listOf("2026-11-12"), sent["reportDates"])
        assertEquals(mapOf("date" to "2026-11-12", "correct" to 2, "total" to 3, "xp" to 65), sent["dailyQuestions"])
        assertFalse(progressDataStore.isStateDirty().first())
    }

    @Test
    fun `an account that never uploaded a snapshot keeps the XP on its profile`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        serverAnswers(state = null, profilePoints = 430)
        serverAcceptsUploads()

        assertTrue(sync.onSignedIn("user-a"))

        assertEquals(430, xpManager.getSyncState().totalXP)
    }

    @Test
    fun `without a connection nothing is lost and the change stays marked for upload`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        progressDataStore.setOwnerUserId("user-a")
        xpManager.addXP(75, "lesson, offline")
        progressDataStore.markLessonCompleted("lesson-7")
        progressDataStore.setStateDirty(true)
        whenever(apiService.getUserProgress(any())).thenAnswer { throw IOException("no network") }
        whenever(apiService.updateUserProgress(any(), any())).thenAnswer { throw IOException("no network") }

        assertFalse(sync.syncNow())
        assertFalse(sync.pushIfDirty())

        assertEquals(75, xpManager.getSyncState().totalXP)
        assertEquals(setOf("lesson-7"), progressDataStore.getSnapshot().completedLessons)
        assertTrue(progressDataStore.isStateDirty().first())
    }

    @Test
    fun `a rejected upload keeps the change marked for upload`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        progressDataStore.setOwnerUserId("user-a")
        xpManager.addXP(50, "quiz")
        progressDataStore.setStateDirty(true)
        whenever(apiService.updateUserProgress(any(), any()))
            .thenReturn(Response.error(500, "{}".toResponseBody()))

        assertFalse(sync.pushIfDirty())
        assertTrue(progressDataStore.isStateDirty().first())
    }

    @Test
    fun `progress is never uploaded under a different account`() = runBlocking<Unit> {
        progressDataStore.setOwnerUserId("user-a")
        xpManager.addXP(300, "account A")
        progressDataStore.setStateDirty(true)
        // Someone else's session is active (should not happen, but must be safe).
        whenever(tokenManager.getUserId()).thenReturn("user-b")

        assertFalse(sync.pushIfDirty())
        assertFalse(sync.syncNow())

        verify(apiService, never()).updateUserProgress(any(), any())
        verify(apiService, never()).getUserProgress(any())
    }

    @Test
    fun `nothing is uploaded when nothing changed`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        progressDataStore.setOwnerUserId("user-a")

        assertTrue(sync.pushIfDirty())

        verify(apiService, never()).updateUserProgress(any(), any())
    }

    @Test
    fun `lessons completed with an older app version are restored from their progress rows`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        val body = mapOf<String, Any>(
            "profile" to mapOf("totalPoints" to 75.0),
            "progress" to listOf(
                mapOf("lessonId" to "lesson-3", "completed" to true),
                mapOf("lessonId" to "lesson-4", "completed" to false),
                mapOf("lessonId" to "qa-row", "completed" to true)
            )
        )
        whenever(apiService.getUserProgress("Bearer token")).thenReturn(Response.success(body))
        serverAcceptsUploads()

        sync.onSignedIn("user-a")

        assertEquals(LearningProgressSnapshot(completedLessons = setOf("lesson-3")), progressDataStore.getSnapshot())
    }

    // ---- sign-in speed: nothing is uploaded back, and the network cannot hold sign-in ----

    private val serverState = mapOf(
        "totalPoints" to 290.0, "lives" to 10.0, "currentStreak" to 1.0, "lastStreakDate" to "2026-11-12",
        "longestStreak" to 1.0,
        "watchedVideos" to listOf("mod4-antenatal-care"), "completedQuizzes" to listOf("mod4-antenatal-care"),
        "completedLessons" to listOf("lesson-9"), "reportDates" to listOf("2026-11-12")
    )

    @Test
    fun `progress restored from the server is not uploaded straight back`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        serverAnswers(serverState)
        serverAcceptsUploads()

        assertTrue(sync.onSignedIn("user-a"))

        assertEquals(290, xpManager.getSyncState().totalXP)
        assertEquals(setOf("lesson-9"), progressDataStore.getSnapshot().completedLessons)
        // Everything on the phone came from the server: there is nothing to send.
        assertFalse(progressDataStore.isStateDirty().first())
        verify(apiService, never()).updateUserProgress(any(), any())
    }

    @Test
    fun `what the server is missing is still uploaded after a restore`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        progressDataStore.setOwnerUserId("user-a")
        // Finished with an older app version that kept no "needs upload" mark.
        progressDataStore.markLessonCompleted("lesson-2")
        assertFalse(progressDataStore.isStateDirty().first())
        serverAnswers(serverState)
        serverAcceptsUploads()

        assertTrue(sync.syncNow())

        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("lesson-2", "lesson-9"), uploadedState()["completedLessons"] as List<String>)
        assertFalse(progressDataStore.isStateDirty().first())
    }

    @Test
    fun `sign-in does not wait for a slow server`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        whenever(apiService.getUserProgress("Bearer token")).thenAnswer {
            Thread.sleep(1_500) // a cold server on a weak connection
            Response.success(mapOf<String, Any>("state" to serverState, "profile" to emptyMap<String, Any>(), "progress" to emptyList<Any>()))
        }
        serverAcceptsUploads()

        val started = System.currentTimeMillis()
        val restoredInTime = sync.onSignedIn("user-a", waitMs = 200)
        val waited = System.currentTimeMillis() - started

        assertFalse("the restore cannot have finished yet", restoredInTime)
        assertTrue("sign-in waited ${waited} ms for the server", waited < 1_000)
        // The phone already belongs to the account, so Home can be shown safely...
        assertEquals("user-a", progressDataStore.getOwnerUserId())
        // ...and the restore still completes in the background.
        val deadline = System.currentTimeMillis() + 5_000
        while (xpManager.getSyncState().totalXP != 290 && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertEquals(290, xpManager.getSyncState().totalXP)
    }

    @Test
    fun `handing the phone to another account is local and wipes at once`() = runBlocking<Unit> {
        progressDataStore.setOwnerUserId("user-a")
        xpManager.addXP(300, "account A")
        progressDataStore.markLessonCompleted("lesson-1")

        assertTrue("user-b has no history on this phone", sync.claimDevice("user-b"))

        assertEquals(0, xpManager.getSyncState().totalXP)
        assertTrue(progressDataStore.getSnapshot().completedLessons.isEmpty())
        assertEquals("user-b", progressDataStore.getOwnerUserId())
        verify(pendingSyncDao).deleteItemsOfOtherUsers("user-b")
        // No network was needed for any of that.
        verify(apiService, never()).getUserProgress(any())
        // The same account again is not a hand-over.
        assertFalse(sync.claimDevice("user-b"))
    }

    @Test
    fun `a reply that arrives after the phone changed hands is thrown away`() = runBlocking<Unit> {
        whenever(tokenManager.getUserId()).thenReturn("user-a")
        progressDataStore.setOwnerUserId("user-a")
        whenever(apiService.getUserProgress("Bearer token")).thenAnswer {
            // While A's progress is still on its way, B signs in on this phone.
            runBlocking { sync.claimDevice("user-b") }
            Response.success(mapOf<String, Any>("state" to serverState, "profile" to emptyMap<String, Any>(), "progress" to emptyList<Any>()))
        }

        assertFalse(sync.syncNow())

        // None of A's progress reached B's store.
        assertEquals(0, xpManager.getSyncState().totalXP)
        assertTrue(progressDataStore.getSnapshot().completedLessons.isEmpty())
        assertEquals("user-b", progressDataStore.getOwnerUserId())
    }
}
