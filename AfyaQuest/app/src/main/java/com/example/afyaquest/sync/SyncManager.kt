package com.example.afyaquest.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.afyaquest.data.local.dao.PendingSyncDao
import com.example.afyaquest.data.local.entity.*
import com.example.afyaquest.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val networkMonitor: NetworkMonitor
) {

    private val workManager = WorkManager.getInstance(context)

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
     * Sync pending reports
     * @return number of reports synced
     */
    suspend fun syncReports(): Int {
        val unsyncedReports = pendingSyncDao.getUnsyncedReports()
        var syncedCount = 0

        for (report in unsyncedReports) {
            try {
                // TODO: Call API to submit report
                // val result = reportsRepository.submitReport(report)

                // For now, simulate successful sync
                Log.d("SyncManager", "Syncing report: ${report.id}")

                // Mark as synced
                pendingSyncDao.markReportSynced(report.id)
                syncedCount++

            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync report ${report.id}: ${e.message}")
                // Continue with next item
            }
        }

        return syncedCount
    }

    /**
     * Sync pending quiz results
     * @return number of quizzes synced
     */
    suspend fun syncQuizzes(): Int {
        val unsyncedQuizzes = pendingSyncDao.getUnsyncedQuizzes()
        var syncedCount = 0

        for (quiz in unsyncedQuizzes) {
            try {
                // TODO: Call API to submit quiz result
                // val result = questionsRepository.submitQuiz(quiz)

                Log.d("SyncManager", "Syncing quiz: ${quiz.id}")

                // Mark as synced
                pendingSyncDao.markQuizSynced(quiz.id)
                syncedCount++

            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync quiz ${quiz.id}: ${e.message}")
            }
        }

        return syncedCount
    }

    /**
     * Sync pending chat messages
     * @return number of chats synced
     */
    suspend fun syncChats(): Int {
        val unsyncedChats = pendingSyncDao.getUnsyncedChats()
        var syncedCount = 0

        for (chat in unsyncedChats) {
            try {
                // TODO: Call API to send chat message
                // val result = chatRepository.sendMessage(chat)

                Log.d("SyncManager", "Syncing chat: ${chat.id}")

                // Mark as synced with mock response
                pendingSyncDao.markChatSynced(
                    chat.id,
                    received = true,
                    response = "Message synced"
                )
                syncedCount++

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
                // val result = mapRepository.updateClientStatus(visit)

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
