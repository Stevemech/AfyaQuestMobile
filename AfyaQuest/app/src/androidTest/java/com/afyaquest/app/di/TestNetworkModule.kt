package com.afyaquest.app.di

import com.afyaquest.app.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Replaces the real network module in instrumented tests.
 *
 * The API points at a local port nothing listens on, so every call fails at once, exactly as it
 * does on a phone with no connection. UI tests therefore never read or write the production
 * backend, and they exercise the offline paths of each screen.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService =
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1:9/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.SECONDS)
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
}
