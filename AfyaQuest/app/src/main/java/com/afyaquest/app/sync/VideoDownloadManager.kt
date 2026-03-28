package com.afyaquest.app.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.afyaquest.app.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
) {

    companion object {
        private const val TAG = "VideoDownloadManager"
        const val VIDEO_DIR = "video_modules"
        private const val OFFLOAD_DELAY_HOURS = 24L
    }

    private val _downloadingModules = MutableStateFlow<Set<String>>(emptySet())
    val downloadingModules: StateFlow<Set<String>> = _downloadingModules.asStateFlow()

    private val _downloadedModules = MutableStateFlow<Set<String>>(emptySet())
    val downloadedModules: StateFlow<Set<String>> = _downloadedModules.asStateFlow()

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
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("video_download_$moduleId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "video_download_$moduleId",
                ExistingWorkPolicy.KEEP,
                downloadWork
            )
    }

    fun queueAssignedModuleDownloads(assignedModuleIds: List<String>, videoUrls: Map<String, String>) {
        for (moduleId in assignedModuleIds) {
            if (!_downloadedModules.value.contains(moduleId)) {
                val url = videoUrls[moduleId] ?: continue
                queueDownload(moduleId, url)
            }
        }
    }

    /**
     * Schedule video offload 24 hours after quiz completion.
     * Called when a user completes a video's quiz.
     */
    fun scheduleOffload(moduleId: String) {
        Log.d(TAG, "Scheduling offload for module $moduleId in $OFFLOAD_DELAY_HOURS hours")

        val inputData = workDataOf(VideoOffloadWorker.KEY_MODULE_ID to moduleId)

        val offloadWork = OneTimeWorkRequestBuilder<VideoOffloadWorker>()
            .setInputData(inputData)
            .setInitialDelay(OFFLOAD_DELAY_HOURS, TimeUnit.HOURS)
            .addTag("video_offload_$moduleId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "video_offload_$moduleId",
                ExistingWorkPolicy.REPLACE,
                offloadWork
            )
    }

    fun markDownloaded(moduleId: String) {
        _downloadedModules.value = _downloadedModules.value + moduleId
        _downloadingModules.value = _downloadingModules.value - moduleId
    }

    fun markFailed(moduleId: String) {
        _downloadingModules.value = _downloadingModules.value - moduleId
    }

    fun markOffloaded(moduleId: String) {
        _downloadedModules.value = _downloadedModules.value - moduleId
    }

    fun isModuleDownloaded(moduleId: String): Boolean {
        val videoDir = context.getExternalFilesDir(VIDEO_DIR) ?: return false
        val file = videoDir.resolve("module_$moduleId.mp4")
        return file.exists() && file.length() > 0
    }

    fun getLocalFilePath(moduleId: String): String? {
        val videoDir = context.getExternalFilesDir(VIDEO_DIR) ?: return null
        val file = videoDir.resolve("module_$moduleId.mp4")
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    fun deleteLocalVideo(moduleId: String): Boolean {
        val videoDir = context.getExternalFilesDir(VIDEO_DIR) ?: return false
        val file = videoDir.resolve("module_$moduleId.mp4")
        return if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                _downloadedModules.value = _downloadedModules.value - moduleId
                Log.d(TAG, "Offloaded video for module $moduleId")
            }
            deleted
        } else false
    }

    fun cancelDownload(moduleId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("video_download_$moduleId")
        _downloadingModules.value = _downloadingModules.value - moduleId
    }

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
