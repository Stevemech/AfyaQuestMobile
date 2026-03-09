package com.example.afyaquest.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.afyaquest.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages video module downloads for offline playback.
 * When a module is assigned to the user by an admin, this manager queues the download.
 * It monitors internet connectivity and starts downloading when a good connection is available.
 */
@Singleton
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
) {

    companion object {
        private const val TAG = "VideoDownloadManager"
        const val VIDEO_DIR = "video_modules"
    }

    private val _downloadingModules = MutableStateFlow<Set<String>>(emptySet())
    val downloadingModules: StateFlow<Set<String>> = _downloadingModules.asStateFlow()

    private val _downloadedModules = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModules: StateFlow<Set<String>> = _downloadedModules.asStateFlow()

    /**
     * Queue a video module for download.
     * Uses WorkManager with network constraints so the download only starts when
     * a good connection is established.
     */
    fun queueDownload(moduleId: String, videoUrl: String) {
        Log.d(TAG, "Queueing download for module $moduleId: $videoUrl")

        _downloadingModules.value = _downloadingModules.value + moduleId

        val inputData = workDataOf(
            VideoDownloadWorker.KEY_MODULE_ID to moduleId,
            VideoDownloadWorker.KEY_VIDEO_URL to videoUrl
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag("video_download_$moduleId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "video_download_$moduleId",
                ExistingWorkPolicy.KEEP,
                downloadWork
            )
    }

    /**
     * Queue downloads for all assigned modules that haven't been downloaded yet.
     */
    fun queueAssignedModuleDownloads(assignedModuleIds: List<String>, videoUrls: Map<String, String>) {
        for (moduleId in assignedModuleIds) {
            if (!_downloadedModules.value.contains(moduleId)) {
                val url = videoUrls[moduleId] ?: continue
                queueDownload(moduleId, url)
            }
        }
    }

    /**
     * Mark a module as downloaded (called by the worker on success).
     */
    fun markDownloaded(moduleId: String) {
        _downloadedModules.value = _downloadedModules.value + moduleId
        _downloadingModules.value = _downloadingModules.value - moduleId
    }

    /**
     * Mark a download as failed (called by the worker on failure).
     */
    fun markFailed(moduleId: String) {
        _downloadingModules.value = _downloadingModules.value - moduleId
    }

    /**
     * Check if a module is downloaded locally.
     */
    fun isModuleDownloaded(moduleId: String): Boolean {
        val videoDir = context.getExternalFilesDir(VIDEO_DIR) ?: return false
        val file = videoDir.resolve("module_$moduleId.mp4")
        return file.exists() && file.length() > 0
    }

    /**
     * Get the local file path for a downloaded module.
     */
    fun getLocalFilePath(moduleId: String): String? {
        val videoDir = context.getExternalFilesDir(VIDEO_DIR) ?: return null
        val file = videoDir.resolve("module_$moduleId.mp4")
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    /**
     * Cancel a pending or in-progress download.
     */
    fun cancelDownload(moduleId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("video_download_$moduleId")
        _downloadingModules.value = _downloadingModules.value - moduleId
    }

    /**
     * Initialize by checking which modules are already downloaded on disk.
     */
    fun refreshDownloadedState() {
        val videoDir = context.getExternalFilesDir(VIDEO_DIR) ?: return
        if (!videoDir.exists()) return

        val downloaded = mutableSetOf<String>()
        videoDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("module_") && file.length() > 0) {
                val moduleId = file.nameWithoutExtension.removePrefix("module_")
                downloaded.add(moduleId)
            }
        }
        _downloadedModules.value = downloaded
    }
}
