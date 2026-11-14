import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// Export Room schemas (enables migration verification + history).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.afyaquest.app"
    compileSdk = 36

    val localProperties = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.let { load(FileInputStream(it)) }
    }

    defaultConfig {
        applicationId = "com.afyaquest.app"
        minSdk = 27
        targetSdk = 36
        versionCode = 4
        versionName = "1.4"

        // Starts the Hilt test application, so screens that use hiltViewModel() can be tested.
        testInstrumentationRunner = "com.afyaquest.app.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            localProperties.getProperty("google.maps.apiKey", "")
    }

    signingConfigs {
        create("release") {
            // Credentials live in ~/.gradle/gradle.properties, never in the repo.
            // Without them the release build stays unsigned rather than failing.
            val storeFilePath = providers.gradleProperty("AFYAQUEST_STORE_FILE").orNull
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("AFYAQUEST_STORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("AFYAQUEST_KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("AFYAQUEST_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            if (providers.gradleProperty("AFYAQUEST_STORE_FILE").orNull != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            // Phones are ARM. `-PphonesOnly` leaves out the x86 libraries that only emulators on
            // Intel/AMD computers need, which makes the APK about 17 MB smaller (handwriting
            // recognition ships one large native library per processor type).
            if (providers.gradleProperty("phonesOnly").isPresent) {
                ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    // Generates the factories HiltWorkerFactory needs for @HiltWorker classes. Without it
    // WorkManager cannot construct SyncWorker / the video workers and background sync never runs.
    ksp(libs.androidx.hilt.compiler)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)

    // Video Player
    implementation(libs.exoplayer)

    // ML Kit Digital Ink Recognition
    implementation(libs.mlkit.digital.ink.recognition)

    // WorkManager (for sync)
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Encrypted SharedPreferences
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}