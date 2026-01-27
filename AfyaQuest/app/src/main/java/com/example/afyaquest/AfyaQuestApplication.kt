package com.example.afyaquest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Afya Quest.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class AfyaQuestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize application-level components here if needed
    }
}
