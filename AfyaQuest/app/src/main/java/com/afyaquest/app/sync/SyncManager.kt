package com.afyaquest.app.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.afyaquest.app.data.local.dao.ChatMessageDao
import com.afyaquest.app.data.local.dao.PendingSyncDao
import com.afyaquest.app.data.local.dao.ReportDao
import com.afyaquest.app.data.local.entity.*
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.util.NetworkMonitor
import com.afyaquest.app.util.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What one upload pass achieved. */
data class SyncPassResult(
    /** Queued items (reports, quiz answers, chat questions, stop visits) uploaded in this pass. */
    val uploaded: Int,
    /** Queued items still waiting on the phone after the pass. */
    val remaining: Int,
    /** The XP / learning-progress snapshot is on the server. */
    val progressInSync: Boolean
) {
    /** Nothing is left on the phone that the server has not seen. */
    val complete: Boolean get() = remaining == 0 && progressInSync
}

/**
 * Manages offline sync operations
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingSyncDao: PendingSyncDao,
    private val networkMonitor: NetworkMonitor,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val reportDao: ReportDao,
    private val progressSyncManager: ProgressSyncManager,
    private val chatMessageDao: ChatMessageDao
) {

    private val workManager = WorkManager.getInstance(context)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    /** Upload passes never overlap: a second caller waits, then finds nothing left to send. */
    private val syncMutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // XP, lessons and videos finished without a connection have no queue row of their own.
        // Ask WorkManager for a pass as soon as there is a connection, so they are uploaded
        // within seconds of coming back online (also when the app is closed) instead of waiting
        // for the next periodic pass.
        scope.launch {
            progressSyncManager.hasUnsyncedChanges
                .distinctUntilChanged()
                .collect { unsynced -> if (unsynced) triggerImmediateSync() }
        }
    }

    /** Forget the last sync problem (it belonged to the session that just ended or began). */
    fun clearError() {
        _lastSyncError.value = null
    }

    /** Somebody is signed in on this phone, so there is an account to upload to. */
    fun isSignedIn(): Boolean = !tokenManager.getIdToken().isNullOrBlank()

    /**
     * Run sync immediately on the calling coroutine (not via WorkManager).
     * Returns the total number of items synced.
     */
    suspend fun syncNow(): Int = runSyncPass()?.uploaded ?: 0

    /**
     * One full upload pass: queued reports, quiz answers, chat questions and stop visits, then the
     * XP / learning-progress snapshot.
     *
     * The periodic worker, the "connection is back" worker and the Home screen can all ask for a
     * pass at the same moment. They are serialised here, because two passes running side by side
     * each read the same queue and uploaded every item twice (a question asked offline was
     * answered twice).
     *
     * Returns null when nothing could be attempted: no connection, or nobody is signed in.
     */
    suspend fun runSyncPass(): SyncPassResult? = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            _isSyncing.value = true
            _lastSyncError.value = null
            try {
                // Nobody is signed in: nothing to upload and nothing to complain about. (Setting
                // an error here left a "Session expired" card on Home right after signing in.)
                if (tokenManager.getIdToken().isNullOrBlank()) return@withLock null

                if (!networkMonitor.isCurrentlyConnected()) {
                    _lastSyncError.value = "No internet connection"
                    return@withLock null
                }

                val reports = syncReports()
                val quizzes = syncQuizzes()
                val chats = syncChats()
                val visits = syncClientVisits()
                // XP, streak and learning progress travel as one account snapshot.
                val progressInSync = progressSyncManager.pushIfDirty()

                val remaining = pendingSyncDao.getUnsyncedReports().size +
                    pendingSyncDao.getUnsyncedQuizzes().size +
                    pendingSyncDao.getUnsyncedChats().size +
                    pendingSyncDao.getUnsyncedClientVisits().size
                if (remaining > 0) {
                    _lastSyncError.value = "Sync failed for $remaining item(s). Will retry."
                } else if (!progressInSync) {
                    _lastSyncError.value = "Could not upload your progress. Will retry."
                }

                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                pendingSyncDao.deleteSyncedReports(sevenDaysAgo)
                pendingSyncDao.deleteSyncedQuizzes(sevenDaysAgo)
                pendingSyncDao.deleteSyncedChats(sevenDaysAgo)
                pendingSyncDao.deleteSyncedClientVisits(sevenDaysAgo)

                SyncPassResult(
                    uploaded = reports + quizzes + chats + visits,
                    remaining = remaining,
                    progressInSync = progressInSync
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncManager", "sync pass failed: ${e.message}")
                _lastSyncError.value = "Sync failed: ${e.message}"
                null
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Flow combining all unsynced counts. Progress (XP, streak, watched videos, ...) that has not
     * been uploaded yet counts as one item, so the Home indicator never says "All synced" while
     * something earned offline is still only on this phone.
     */
    val totalUnsyncedCount: Flow<Int> = combine(
        pendingSyncDao.getUnsyncedReportsCount(),
        pendingSyncDao.getUnsyncedQuizzesCount(),
        pendingSyncDao.getUnsyncedChatsCount(),
        pendingSyncDao.getUnsyncedClientVisitsCount(),
        progressSyncManager.hasUnsyncedChanges
    ) { reports, quizzes, chats, visits, progressDirty ->
        reports + quizzes + chats + visits + (if (progressDirty) 1 else 0)
    }

    /**
     * Schedule periodic sync work
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, // Repeat interval
            TimeUnit.MINUTES,
            5, // Flex interval
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Trigger immediate sync
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        // APPEND, not REPLACE: replacing cancels a pass that is in the middle of an upload. The
        // request may already have reached the server, and cancelling before the queue row is
        // marked as sent uploads the same item again on the next pass.
        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            oneTimeWorkRequest
        )
    }

    /**
     * Cancel all sync work
     */
    fun cancelSync() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }

    /**
     * Queue report for sync
     */
    suspend fun queueReport(report: PendingReportEntity): Long {
        val id = pendingSyncDao.insertPendingReport(report)
        triggerImmediateSync()
        return id
    }

    /** Take a report out of the upload queue (it was deleted before it could be sent). */
    suspend fun dropQueuedReport(userId: String, date: String) {
        pendingSyncDao.deleteUnsyncedReportsForDate(userId, date)
    }

    /**
     * Queue quiz result for sync
     */
    suspend fun queueQuiz(quiz: PendingQuizEntity): Long {
        val id = pendingSyncDao.insertPendingQuiz(quiz)
        triggerImmediateSync()
        return id
    }

    /**
     * Queue chat message for sync
     */
    suspend fun queueChat(chat: PendingChatEntity): Long {
        val id = pendingSyncDao.insertPendingChat(chat)
        triggerImmediateSync()
        return id
    }

    /**
     * Queue client visit for sync
     */
    suspend fun queueClientVisit(visit: PendingClientVisitEntity): Long {
        val id = pendingSyncDao.insertPendingClientVisit(visit)
        triggerImmediateSync()
        return id
    }

    /**
     * Sync pending reports to the API
     * @return number of reports synced
     */
    suspend fun syncReports(): Int {
        val unsyncedReports = pendingSyncDao.getUnsyncedReports()
        var syncedCount = 0

        val idToken = tokenManager.getIdToken() ?: return 0

        for (report in unsyncedReports) {
            try {
                val requestBody = mapOf<String, Any>(
                    "date" to report.date,
                    "patientsVisited" to report.patientsVisited,
                    "vaccinationsGiven" to report.vaccinationsGiven,
                    "healthEducation" to report.healthEducation,
                    "challenges" to report.challenges,
                    "notes" to report.notes
                )

                val response = apiService.createReport("Bearer $idToken", requestBody)

                if (response.isSuccessful) {
                    // Mark pending report as synced
                    pendingSyncDao.markReportSynced(report.id)

                    // The server keeps one report per day (the latest), so every local report of
                    // that day is now uploaded. Marking only one left a same-day second report
                    // showing "Pending Sync" forever.
                    reportDao.markReportsSyncedForDate(report.userId, report.date)

                    syncedCount++
                    Log.d("SyncManager", "Synced report: ${report.id}")
                } else {
                    Log.e("SyncManager", "Failed to sync report ${report.id}: ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync report ${report.id}: ${e.message}")
            }
        }

        return syncedCount
    }

    /**
     * Sync pending quiz results
     * @return number of quizzes synced
     */
    suspend fun syncQuizzes(): Int {
        // Daily question answers are applied locally (XP/lives). Mark as synced
        // so the pending count clears. Module quiz results use a separate path.
        val unsyncedQuizzes = pendingSyncDao.getUnsyncedQuizzes()
        var syncedCount = 0
        for (quiz in unsyncedQuizzes) {
            pendingSyncDao.markQuizSynced(quiz.id)
            syncedCount++
        }
        return syncedCount
    }

    /**
     * Sync pending chat messages
     * @return number of chats synced
     */
    suspend fun syncChats(): Int {
        val unsyncedChats = pendingSyncDao.getUnsyncedChats()
        if (unsyncedChats.isEmpty()) return 0
        var syncedCount = 0

        val idToken = tokenManager.getIdToken() ?: return 0

        for (chat in unsyncedChats) {
            try {
                // The server keeps the conversation itself; only the question is sent.
                val requestBody = mapOf("message" to chat.message)

                val response = apiService.sendChatMessage(
                    "Bearer $idToken",
                    requestBody
                )

                if (response.isSuccessful) {
                    val answer = response.body()?.get("response")
                    pendingSyncDao.markChatSynced(
                        chat.id,
                        received = true,
                        response = answer ?: "Synced"
                    )
                    // The question was asked offline: show it as sent and add Fred's answer to
                    // the conversation, where the chat screen picks it up on its own.
                    chatMessageDao.markQuestionDelivered(chat.userId, chat.message)
                    if (!answer.isNullOrBlank()) {
                        chatMessageDao.insertMessage(
                            ChatMessageEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                userId = chat.userId,
                                message = answer,
                                role = "assistant",
                                isSynced = true
                            )
                        )
                    }
                    syncedCount++
                    Log.d("SyncManager", "Synced chat: ${chat.id}")
                } else {
                    Log.e("SyncManager", "Failed to sync chat ${chat.id}: ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync chat ${chat.id}: ${e.message}")
            }
        }

        return syncedCount
    }

    /**
     * Sync pending client visits (itinerary stop completions) to the API
     * @return number of visits synced
     */
    suspend fun syncClientVisits(): Int {
        val unsyncedVisits = pendingSyncDao.getUnsyncedClientVisits()
        var syncedCount = 0

        val idToken = tokenManager.getIdToken() ?: return 0

        for (visit in unsyncedVisits) {
            try {
                val requestBody = mapOf<String, Any>(
                    "type" to "itinerary_stop_complete",
                    "itemId" to visit.clientId,
                    "date" to visit.visitDate
                )

                val response = apiService.updateUserProgress("Bearer $idToken", requestBody)

                if (response.isSuccessful) {
                    pendingSyncDao.markClientVisitSynced(visit.id)
                    syncedCount++
                    Log.d("SyncManager", "Synced visit: ${visit.id}")
                } else {
                    Log.e("SyncManager", "Failed to sync visit ${visit.id}: ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync visit ${visit.id}: ${e.message}")
            }
        }

        return syncedCount
    }
}
