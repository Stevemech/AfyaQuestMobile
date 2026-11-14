package com.afyaquest.app.di

import com.afyaquest.app.BuildConfig
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.TimeoutDns
import com.afyaquest.app.data.remote.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // TODO: Replace with your actual API URL from local.properties
    private const val BASE_URL = "https://tkytlmt856.execute-api.us-east-1.amazonaws.com/prod/"

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenAuthenticator: TokenAuthenticator): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Every request has a hard ceiling. Before, the only limits were 30 s per connection
        // attempt (one per server address) and nothing at all for the name lookup, so on a bad
        // network a spinner could turn for minutes with no error and no way out.
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .dns(TimeoutDns())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS) // Fred's answers can take a while to come back
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
