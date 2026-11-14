package com.afyaquest.app.sync

import android.util.Log
import com.afyaquest.app.data.local.dao.PendingSyncDao
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.util.DailyQuestionsResult
import com.afyaquest.app.util.LearningProgressSnapshot
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.XpManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account state as the server holds it (lambda user-updateProgress, type "state_sync").
 * Parsing is kept apart from the networking so it can be unit-tested with plain maps.
 */
data class ServerProgressState(
    val totalPoints: Int = 0,
    val lives: Int? = null,
    val currentStreak: Int = 0,
    val lastStreakDate: String = "",
    val longestStreak: Int = 0,
    val progress: LearningProgressSnapshot = LearningProgressSnapshot()
) {
    companion object {
        /** [map] is the decoded JSON object; numbers arrive as Double, lists as List<Any?>. */
        fun fromMap(map: Map<*, *>?): ServerProgressState? {
            if (map == null) return null
            val daily = (map["dailyQuestions"] as? Map<*, *>)?.let { d ->
                val date = d["date"] as? String ?: return@let null
                DailyQuestionsResult(
                    date = date,
                    correctAnswers = d["correct"].toIntOrZero(),
                    totalQuestions = d["total"].toIntOrZero(),
                    xpEarned = d["xp"].toIntOrZero()
                )
            }
            return ServerProgressState(
                totalPoints = map["totalPoints"].toIntOrZero(),
                lives = (map["lives"] as? Number)?.toInt(),
                currentStreak = map["currentStreak"].toIntOrZero(),
                lastStreakDate = map["lastStreakDate"] as? String ?: "",
                longestStreak = map["longestStreak"].toIntOrZero(),
                progress = LearningProgressSnapshot(
                    watchedVideos = map["watchedVideos"].toIdSet(),
                    completedQuizzes = map["completedQuizzes"].toIdSet(),
                    completedLessons = map["completedLessons"].toIdSet(),
                    reportDates = map["reportDates"].toIdSet(),
                    dailyQuestions = daily
                )
            )
        }

        /** The JSON body of a "state_sync" upload. */
        fun uploadBody(xp: XpManager.SyncState, progress: LearningProgressSnapshot): Map<String, Any> {
            val state = mutableMapOf<String, Any>(
                "totalPoints" to xp.totalXP,
                "lives" to xp.lives,
                "currentStreak" to xp.storedStreak,
                "lastStreakDate" to xp.lastStreakDate,
                "longestStreak" to xp.longestStreak,
                "watchedVideos" to progress.watchedVideos.sorted(),
                "completedQuizzes" to progress.completedQuizzes.sorted(),
                "completedLessons" to progress.completedLessons.sorted(),
                "reportDates" to progress.reportDates.sorted()
            )
            progress.dailyQuestions?.let { daily ->
                state["dailyQuestions"] = mapOf(
                    "date" to daily.date,
                    "correct" to daily.correctAnswers,
                    "total" to daily.totalQuestions,
                    "xp" to daily.xpEarned
                )
            }
            return mapOf("type" to "state_sync", "state" to state)
        }

        private fun Any?.toIntOrZero(): Int = (this as? Number)?.toInt()?.coerceAtLeast(0) ?: 0

        private fun Any?.toIdSet(): Set<String> =
            (this as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
    }
}

/**
 * Keeps this device's XP, streak, lives and learning progress in step with the account.
 *
 * Everything is earned and stored locally first, so the app works offline. This class
 *  - flags the local state as "dirty" whenever the user earns something and uploads it shortly
 *    after; an upload that fails (no network) is retried by [SyncManager] / [SyncWorker];
 *  - on sign-in and on app start pulls the account state and merges it in, which restores
 *    progress on a new phone and never loses XP earned offline;
 *  - wipes local progress when a different account signs in, so one person's progress is never
 *    shown to, or uploaded as, another's.
 *
 * Two locks keep that safe without ever making sign-in wait for the network:
 *  - [syncLock] lets one pull/upload sequence run at a time; it is held across network calls;
 *  - [storeLock] guards the rewrite of the local stores and is never held across the network.
 *    A reply that arrives after the phone changed hands ([epoch] moved on) is thrown away.
 */
@OptIn(FlowPreview::class)
@Singleton
class ProgressSyncManager @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val xpManager: XpManager,
    private val progressDataStore: ProgressDataStore,
    private val pendingSyncDao: PendingSyncDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()
    private val storeLock = Mutex()
    private val watching = AtomicBoolean(false)

    /** Goes up every time the phone's progress store is handed to an account. */
    private val epoch = AtomicInteger(0)

    /**
     * The local state exactly as the last pull or upload left it. A change of the stores that
     * leads to this value came from the server, not from the user, and needs no upload.
     */
    @Volatile
    private var lastSyncedLocal: LocalState? = null

    /** When the last successful pull finished ([System.nanoTime]); 0 = not for this owner yet. */
    @Volatile
    private var lastPullAt = 0L

    @Volatile
    private var lastPullSucceeded = false

    /** How long a pull counts as fresh, so Home does not repeat the one sign-in just made. */
    internal var pullFreshMs = 30_000L

    /** True while local progress has changes that the server has not confirmed. */
    val hasUnsyncedChanges: Flow<Boolean> = progressDataStore.isStateDirty()

    /** Start watching local progress. Safe to call more than once. */
    fun start() {
        if (!watching.compareAndSet(false, true)) return
        scope.launch {
            combine(xpManager.syncStateFlow(), progressDataStore.snapshotFlow()) { xp, progress -> LocalState(xp, progress) }
                .distinctUntilChanged()
                .drop(1) // what was already stored when the app started is not a change
                // A merge writes two stores one after the other; wait for both before judging.
                .debounce(SETTLE_MS)
                .filter { it != lastSyncedLocal }
                .onEach { progressDataStore.setStateDirty(true) }
                .debounce(UPLOAD_DEBOUNCE_MS)
                .collect { pushIfDirty() }
        }
    }

    /**
     * Hand this phone's progress store to [userId]: wipe it when it belonged to somebody else.
     * Local work only, so it is quick and safe to wait for before Home is shown.
     *
     * @return true when the phone holds no history for this account (new phone, or another
     * account used it), in which case the server's lives are the truth.
     */
    suspend fun claimDevice(userId: String): Boolean = withContext(Dispatchers.IO) {
        storeLock.withLock {
            val previousOwner = progressDataStore.getOwnerUserId()
            if (previousOwner == userId) return@withLock false
            epoch.incrementAndGet()
            lastSyncedLocal = null
            lastPullAt = 0L
            if (previousOwner != null) {
                xpManager.clearAll()
                progressDataStore.clearAll()
                pendingSyncDao.deleteItemsOfOtherUsers(userId)
            }
            progressDataStore.setOwnerUserId(userId)
            true
        }
    }

    /**
     * Call right after a successful sign-in. Makes sure the progress on this device belongs to
     * [userId] (always finished when this returns), then restores the account's progress from the
     * server and uploads what the server is missing, in the background.
     *
     * With [waitMs] the call returns as soon as the restore has arrived or the time is up,
     * whichever is first, and it only waits at all on a phone that holds nothing for this
     * account (a new phone, or another account used it): there Home would otherwise open on
     * zeros. A phone that already has the account's progress shows it immediately. Either way a
     * slow network can no longer hold the sign-in screen.
     *
     * Without [waitMs] the call waits for the whole exchange (used by tests).
     *
     * @return true when the restore had arrived by the time the call returned.
     */
    suspend fun onSignedIn(userId: String, waitMs: Long = Long.MAX_VALUE): Boolean {
        val newOnThisPhone = claimDevice(userId)
        val restored = CompletableDeferred<Boolean>()
        val exchange = scope.launch {
            syncLock.withLock {
                // The waiting sign-in screen only needs the restore; the upload follows on its own.
                restored.complete(pull(adoptLives = newOnThisPhone))
                push(force = false)
            }
        }
        return when {
            waitMs == Long.MAX_VALUE -> {
                exchange.join()
                lastPullSucceeded
            }
            !newOnThisPhone -> false
            else -> withTimeoutOrNull(waitMs) { restored.await() } ?: false
        }
    }

    /**
     * Pull, merge and upload. Used when the app starts with someone already signed in, and when
     * Home opens. A pull that finished a moment ago (sign-in does one) is not repeated.
     */
    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        syncLock.withLock {
            val userId = tokenManager.getUserId() ?: return@withLock false
            val owner = progressDataStore.getOwnerUserId()
            if (owner == null) {
                // Upgrade from a version that did not track the owner: whoever is signed in owns it.
                progressDataStore.setOwnerUserId(userId)
            } else if (owner != userId) {
                return@withLock false
            }
            val justPulled = lastPullAt != 0L && System.nanoTime() - lastPullAt < pullFreshMs * 1_000_000
            val pulled = if (justPulled) true else pull(adoptLives = false)
            push(force = false)
            pulled
        }
    }

    /** Upload local progress if it changed since the last confirmed upload. True when in sync. */
    suspend fun pushIfDirty(): Boolean = withContext(Dispatchers.IO) {
        syncLock.withLock { push(force = false) }
    }

    private suspend fun localState() = LocalState(xpManager.getSyncState(), progressDataStore.getSnapshot())

    private suspend fun pull(adoptLives: Boolean): Boolean {
        val ok = pullOnce(adoptLives)
        lastPullSucceeded = ok
        if (ok) lastPullAt = System.nanoTime()
        return ok
    }

    private suspend fun pullOnce(adoptLives: Boolean): Boolean = try {
        val startedIn = epoch.get()
        val token = tokenManager.getIdToken()
        val response = if (token != null) apiService.getUserProgress("Bearer $token") else null
        val body = response?.takeIf { it.isSuccessful }?.body()
        if (body == null) {
            false
        } else {
            val state = ServerProgressState.fromMap(body["state"] as? Map<*, *>)
            val profile = body["profile"] as? Map<*, *>
            // An account that never uploaded a snapshot still has its totals on the profile.
            val profilePoints = (profile?.get("totalPoints") as? Number)?.toInt() ?: 0
            val profileLives = (profile?.get("lives") as? Number)?.toInt()
            val remote = state?.progress ?: LearningProgressSnapshot()
            storeLock.withLock {
                // The phone was handed to another account while the request was on its way.
                if (epoch.get() != startedIn) return@withLock false
                xpManager.mergeFromServer(
                    totalXP = maxOf(state?.totalPoints ?: 0, profilePoints),
                    lives = state?.lives ?: profileLives,
                    streak = state?.currentStreak ?: 0,
                    lastStreakDate = state?.lastStreakDate ?: "",
                    adoptLives = adoptLives,
                    longestStreak = state?.longestStreak ?: 0
                )
                progressDataStore.mergeFromServer(
                    remote.copy(completedLessons = remote.completedLessons + legacyCompletedLessons(body))
                )
                val local = localState()
                lastSyncedLocal = local
                // Upload only what the server is missing. Data that just came from the server is
                // not sent straight back, which used to cost an extra request on every sign-in.
                if (state == null || !state.covers(local)) progressDataStore.setStateDirty(true)
                true
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "Could not load account progress (will retry): ${e.message}")
        false
    }

    /** Lessons completed with an app version from before snapshots are stored as PROGRESS rows. */
    private fun legacyCompletedLessons(body: Map<String, Any>): Set<String> =
        (body["progress"] as? List<*>).orEmpty()
            .filterIsInstance<Map<*, *>>()
            .filter { it["completed"] == true }
            .mapNotNull { it["lessonId"] as? String }
            .filter { it.startsWith("lesson-") }
            .toSet()

    private suspend fun push(force: Boolean): Boolean = try {
        val startedIn = epoch.get()
        val userId = tokenManager.getUserId()
        val token = tokenManager.getIdToken()
        when {
            !force && !progressDataStore.isStateDirty().first() -> true
            userId == null || token == null -> false
            // Never upload one account's progress under another account.
            progressDataStore.getOwnerUserId() != userId -> false
            else -> {
                val uploaded = localState()
                val response = apiService.updateUserProgress(
                    "Bearer $token",
                    ServerProgressState.uploadBody(uploaded.xp, uploaded.progress)
                )
                if (!response.isSuccessful) {
                    false
                } else {
                    // The server answers with the merged state (it may know more than this device).
                    val merged = ServerProgressState.fromMap(response.body()?.get("state") as? Map<*, *>)
                    storeLock.withLock {
                        // The phone was handed to another account while the upload was on its way.
                        if (epoch.get() != startedIn) return@withLock false
                        if (merged != null) {
                            xpManager.mergeFromServer(
                                totalXP = merged.totalPoints,
                                lives = merged.lives,
                                streak = merged.currentStreak,
                                lastStreakDate = merged.lastStreakDate,
                                adoptLives = false,
                                longestStreak = merged.longestStreak
                            )
                            progressDataStore.mergeFromServer(merged.progress)
                        }
                        val local = localState()
                        lastSyncedLocal = local
                        // Something may have been earned while the upload was in flight: stay dirty then.
                        val inSync = if (merged != null) merged.covers(local) else local == uploaded
                        if (inSync) progressDataStore.setStateDirty(false)
                        true
                    }
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(TAG, "Progress upload failed (will retry): ${e.message}")
        false
    }

    /** XP, streak and learning progress as stored on this phone. */
    private data class LocalState(val xp: XpManager.SyncState, val progress: LearningProgressSnapshot)

    /** The server already has everything in [local] (so there is nothing to upload). */
    private fun ServerProgressState.covers(local: LocalState): Boolean =
        totalPoints >= local.xp.totalXP &&
            lives == local.xp.lives &&
            currentStreak == local.xp.storedStreak &&
            lastStreakDate == local.xp.lastStreakDate &&
            longestStreak >= local.xp.longestStreak &&
            progress.watchedVideos.containsAll(local.progress.watchedVideos) &&
            progress.completedQuizzes.containsAll(local.progress.completedQuizzes) &&
            progress.completedLessons.containsAll(local.progress.completedLessons) &&
            progress.reportDates.containsAll(local.progress.reportDates) &&
            (local.progress.dailyQuestions == null || progress.dailyQuestions == local.progress.dailyQuestions)

    private companion object {
        const val TAG = "ProgressSync"
        const val UPLOAD_DEBOUNCE_MS = 2_000L

        /** Long enough for the two store writes of one merge to both show up. */
        const val SETTLE_MS = 300L
    }
}
