package com.example.afyaquest.data.remote

import com.example.afyaquest.data.remote.dto.*
import com.example.afyaquest.domain.model.DailyQuestionsResponse
import com.example.afyaquest.domain.model.QuizSubmissionRequest
import com.example.afyaquest.domain.model.QuizSubmissionResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for AfyaQuest backend.
 */
interface ApiService {

    // ==================== Authentication ====================

    @POST("auth/register")
    suspend fun register(
        @Body request: Map<String, String>
    ): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: Map<String, String>
    ): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserDto>

    @PUT("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    // ==================== Questions ====================

    @GET("questions/daily")
    suspend fun getDailyQuestions(
        @Header("Authorization") token: String
    ): Response<DailyQuestionsResponse>

    // ==================== Chat ====================

    @POST("chat/message")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @GET("chat/history")
    suspend fun getChatHistory(
        @Header("Authorization") token: String
    ): Response<List<Map<String, Any>>>

    // ==================== Progress ====================

    @GET("progress")
    suspend fun getUserProgress(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("progress/quiz")
    suspend fun submitQuiz(
        @Header("Authorization") token: String,
        @Body request: QuizSubmissionRequest
    ): Response<QuizSubmissionResponse>

    // ==================== Reports ====================

    @POST("reports")
    suspend fun createReport(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("reports")
    suspend fun getAllReports(
        @Header("Authorization") token: String
    ): Response<List<Map<String, Any>>>
}
