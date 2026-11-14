package com.afyaquest.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Application used while instrumented tests run.
 *
 * It stands in for [AfyaQuestApplication]: no periodic sync is scheduled and nothing is uploaded.
 * WorkManager's default initializer is removed in the manifest, so the application has to hand it
 * a configuration, otherwise the first screen that touches the sync queue crashes.
 */
open class TestBaseApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

/** Hilt generates `AfyaQuestTestApp_Application` from this, on top of [TestBaseApplication]. */
@CustomTestApplication(TestBaseApplication::class)
interface AfyaQuestTestApp

/** Instrumentation runner that starts the Hilt test application (see `testInstrumentationRunner`). */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, AfyaQuestTestApp_Application::class.java.name, context)
}
