package com.example.afyaquest.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.afyaquest.data.local.dao.PendingSyncDao
import com.example.afyaquest.data.local.dao.ReportDao
import com.example.afyaquest.data.local.entity.*
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.util.NetworkMonitor
import com.example.afyaquest.util.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
    private val reportDao: ReportDao
) {

    private val workManager = WorkManager.getInstance(context)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    /**
     * Run sync immediately on the calling coroutine (not via WorkManager).
     * Returns the total number of items synced.
     */
    suspend fun syncNow(): Int = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext 0
        _isSyncing.value = true
        _lastSyncError.value = null
        try {
            if (!networkMonitor.isCurrentlyConnected()) {
                _lastSyncError.value = "No internet connection"
                return@withContext 0
            }

            val token = tokenManager.getIdToken()
            if (token.isNullOrBlank()) {
                _lastSyncError.value = "Session expired. Please log in again."
                return@withContext 0
            }

            val reports = syncReports()
            val quizzes = syncQuizzes()
            val chats = syncChats()
            val visits = syncClientVisits()

            val total = reports + quizzes + chats + visits

            // Check if items remain unsynced after attempting sync
            if (total == 0) {
                val remaining = pendingSyncDao.getUnsyncedReports().size +
                    pendingSyncDao.getUnsyncedQuizzes().size +
                    pendingSyncDao.getUnsyncedChats().size +
                    pendingSyncDao.getUnsyncedClientVisits().size
                if (remaining > 0) {
                    _lastSyncError.value = "Sync failed for $remaining item(s). Will retry."
                }
            }

            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            pendingSyncDao.deleteSyncedReports(sevenDaysAgo)
            pendingSyncDao.deleteSyncedQuizzes(sevenDaysAgo)
            pendingSyncDao.deleteSyncedChats(sevenDaysAgo)
            pendingSyncDao.deleteSyncedClientVisits(sevenDaysAgo)

            total
        } catch (e: Exception) {
            Log.e("SyncManager", "syncNow failed: ${e.message}")
            _lastSyncError.value = "Sync failed: ${e.message}"
            0
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Flow combining all unsynced counts
     */
    val totalUnsyncedCount: Flow<Int> = combine(
        pendingSyncDao.getUnsyncedReportsCount(),
        pendingSyncDao.getUnsyncedQuizzesCount(),
        pendingSyncDao.getUnsyncedChatsCount(),
        pendingSyncDao.getUnsyncedClientVisitsCount()
    ) { reports, quizzes, chats, visits ->
        reports + quizzes + chats + visits
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

        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
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

                    // Also mark the main report entity as synced if it exists
                    val reportEntity = reportDao.getReportByUserAndDate(report.userId, report.date)
                    if (reportEntity != null) {
                        reportDao.markReportAsSynced(reportEntity.id)
                    }

                    syncedCount++
                    Log.d("SyncManager", "Synced report: ${report.id}")
                } else {
                    Log.e("SyncManager", "Failed to sync report ${report.id}: ${response.code()}")
                }
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
                val requestBody = mapOf(
                    "message" to chat.message,
                    "conversationHistory" to chat.conversationHistory
                )

                val response = apiService.sendChatMessage(
                    "Bearer $idToken",
                    requestBody
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    pendingSyncDao.markChatSynced(
                        chat.id,
                        received = true,
                        response = responseBody?.get("response") ?: "Synced"
                    )
                    syncedCount++
                    Log.d("SyncManager", "Synced chat: ${chat.id}")
                } else {
                    Log.e("SyncManager", "Failed to sync chat ${chat.id}: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync chat ${chat.id}: ${e.message}")
            }
        }

        return syncedCount
    }

    /**
     * Sync pending client visits
     * @return number of visits synced
     */
    suspend fun syncClientVisits(): Int {
        val unsyncedVisits = pendingSyncDao.getUnsyncedClientVisits()
        var syncedCount = 0

        for (visit in unsyncedVisits) {
            try {
                // TODO: Call API to update client visit status
                Log.d("SyncManager", "Syncing client visit: ${visit.id}")

                // Mark as synced
                pendingSyncDao.markClientVisitSynced(visit.id)
                syncedCount++

            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync visit ${visit.id}: ${e.message}")
            }
        }

        return syncedCount
    }
}
