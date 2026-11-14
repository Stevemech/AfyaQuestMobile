package com.afyaquest.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.afyaquest.app.sync.ProgressSyncManager
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.TokenManager
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Afya Quest.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class AfyaQuestApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Lazy: building these pulls in the HTTP client, WorkManager and the encrypted token file.
    // None of that is needed to draw the first screen, so it is created off the main thread
    // instead of holding up every start of the app.
    @Inject
    lateinit var syncManager: Lazy<SyncManager>

    @Inject
    lateinit var progressSyncManager: Lazy<ProgressSyncManager>

    @Inject
    lateinit var tokenManager: Lazy<TokenManager>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        appScope.launch {
            // Open the encrypted token file ahead of the first API call.
            tokenManager.get().warmUp()

            // Schedule periodic sync for offline data
            syncManager.get().schedulePeriodicSync()

            // Upload XP / learning progress to the account whenever it changes
            progressSyncManager.get().start()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
