package com.example.afyaquest.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deletes a locally cached video file 24 hours after the user completes
 * the associated quiz. Frees device storage while the video remains
 * streamable from S3.
 */
@HiltWorker
class VideoOffloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val videoDownloadManager: VideoDownloadManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODULE_ID = "module_id"
        private const val TAG = "VideoOffloadWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val moduleId = inputData.getString(KEY_MODULE_ID)
            ?: return@withContext Result.failure()

        Log.d(TAG, "Offloading video for module $moduleId")

        return@withContext if (videoDownloadManager.deleteLocalVideo(moduleId)) {
            Log.d(TAG, "Successfully offloaded module $moduleId")
            Result.success()
        } else {
            Log.d(TAG, "Nothing to offload for module $moduleId (already removed or never downloaded)")
            Result.success()
        }
    }
}
