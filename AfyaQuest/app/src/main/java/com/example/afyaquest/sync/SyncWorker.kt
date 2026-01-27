package com.example.afyaquest.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.afyaquest.data.local.dao.PendingSyncDao
import com.example.afyaquest.util.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for syncing offline data
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor,
    private val pendingSyncDao: PendingSyncDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check network connectivity
            if (!networkMonitor.isCurrentlyConnected()) {
                return@withContext Result.retry()
            }

            // Sync pending reports
            val reportsSynced = syncManager.syncReports()

            // Sync pending quiz results
            val quizzesSynced = syncManager.syncQuizzes()

            // Sync pending chat messages
            val chatsSynced = syncManager.syncChats()

            // Sync pending client visits
            val visitsSynced = syncManager.syncClientVisits()

            // Clean up old synced data (older than 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            pendingSyncDao.deleteSyncedReports(sevenDaysAgo)
            pendingSyncDao.deleteSyncedQuizzes(sevenDaysAgo)
            pendingSyncDao.deleteSyncedChats(sevenDaysAgo)
            pendingSyncDao.deleteSyncedClientVisits(sevenDaysAgo)

            // Determine result
            val totalSynced = reportsSynced + quizzesSynced + chatsSynced + visitsSynced

            return@withContext if (totalSynced > 0) {
                Result.success()
            } else {
                Result.success() // No items to sync is still success
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync_work"
    }
}
