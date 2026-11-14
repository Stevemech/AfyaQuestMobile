package com.afyaquest.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Background worker for syncing offline data.
 *
 * It runs every 15 minutes while there is a connection, and once more as soon as the connection
 * returns after something was queued offline. The upload itself is [SyncManager.runSyncPass], which
 * is shared with the Home screen so that two passes never run side by side.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        resultFor(syncManager.runSyncPass(), syncManager.isSignedIn())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        const val WORK_NAME = "sync_work"

        /**
         * Nothing left on the phone is a success. Anything that did not go through is retried
         * (with WorkManager's backoff), except when nobody is signed in: there is nothing to
         * retry for until the next login, which starts its own sync.
         */
        fun resultFor(pass: SyncPassResult?, signedIn: Boolean): Result = when {
            pass == null -> if (signedIn) Result.retry() else Result.success()
            pass.complete -> Result.success()
            else -> Result.retry()
        }
    }
}
