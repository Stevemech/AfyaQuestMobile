package com.example.afyaquest.data.repository

import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.domain.model.DailyQuestionsResponse
import com.example.afyaquest.domain.model.QuizSubmissionRequest
import com.example.afyaquest.domain.model.QuizSubmissionResponse
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for daily questions and quiz submissions
 */
@Singleton
class QuestionsRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Get daily questions from API
     */
    fun getDailyQuestions(): Flow<Resource<DailyQuestionsResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = tokenManager.getAccessToken()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }

            val response = apiService.getDailyQuestions("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error(response.message() ?: "Failed to get daily questions"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }

    /**
     * Submit quiz results to API
     */
    fun submitQuiz(request: QuizSubmissionRequest): Flow<Resource<QuizSubmissionResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = tokenManager.getAccessToken()
            if (token == null) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }

            val response = apiService.submitQuiz("Bearer $token", request)

            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error(response.message() ?: "Failed to submit quiz"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }
}
