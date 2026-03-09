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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Background worker that downloads a video module from S3 to local storage.
 * WorkManager handles the network constraint — this worker only runs when
 * the device has a network connection.
 */
@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val videoDownloadManager: VideoDownloadManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODULE_ID = "module_id"
        const val KEY_VIDEO_URL = "video_url"
        private const val TAG = "VideoDownloadWorker"
        private const val BUFFER_SIZE = 8192
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val moduleId = inputData.getString(KEY_MODULE_ID)
            ?: return@withContext Result.failure()
        val videoUrl = inputData.getString(KEY_VIDEO_URL)
            ?: return@withContext Result.failure()

        Log.d(TAG, "Starting download for module $moduleId from $videoUrl")

        try {
            val videoDir = applicationContext.getExternalFilesDir(VideoDownloadManager.VIDEO_DIR)
                ?: return@withContext Result.failure()

            if (!videoDir.exists()) {
                videoDir.mkdirs()
            }

            val targetFile = File(videoDir, "module_$moduleId.mp4")
            val tempFile = File(videoDir, "module_${moduleId}.tmp")

            // If already downloaded, mark and skip
            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d(TAG, "Module $moduleId already downloaded at ${targetFile.absolutePath}")
                videoDownloadManager.markDownloaded(moduleId)
                return@withContext Result.success()
            }

            // Download to temp file first, then rename on success
            val url = URL(videoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.requestMethod = "GET"

            try {
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error: ${connection.responseCode} for module $moduleId")
                    videoDownloadManager.markFailed(moduleId)
                    return@withContext Result.retry()
                }

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                        }

                        Log.d(TAG, "Downloaded ${totalBytesRead / 1024}KB for module $moduleId")
                    }
                }

                // Rename temp -> final
                if (tempFile.renameTo(targetFile)) {
                    Log.d(TAG, "Module $moduleId downloaded successfully to ${targetFile.absolutePath}")
                    videoDownloadManager.markDownloaded(moduleId)
                    return@withContext Result.success()
                } else {
                    Log.e(TAG, "Failed to rename temp file for module $moduleId")
                    tempFile.delete()
                    videoDownloadManager.markFailed(moduleId)
                    return@withContext Result.retry()
                }

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for module $moduleId: ${e.message}", e)
            videoDownloadManager.markFailed(moduleId)
            return@withContext Result.retry()
        }
    }
}
