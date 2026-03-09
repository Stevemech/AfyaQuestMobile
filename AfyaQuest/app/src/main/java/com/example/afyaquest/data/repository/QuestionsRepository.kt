package com.example.afyaquest.data.repository

import com.example.afyaquest.data.local.LocalDailyQuestions
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.domain.model.DailyQuestionsResponse
import com.example.afyaquest.domain.model.QuizSubmissionRequest
import com.example.afyaquest.domain.model.QuizSubmissionResponse
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for daily questions and quiz submissions.
 * Questions are loaded from the local question bank bundled in the APK,
 * so they work fully offline without internet.
 */
@Singleton
class QuestionsRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Get daily questions from local question bank (works offline).
     * Returns 3 questions rotated daily based on the day of year.
     */
    fun getDailyQuestions(): Flow<Resource<DailyQuestionsResponse>> = flow {
        emit(Resource.Loading())

        try {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR) +
                    (calendar.get(Calendar.YEAR) * 366) // Ensure uniqueness across years
            val questions = LocalDailyQuestions.getQuestionsForDay(dayOfYear)

            val dateStr = String.format(
                "%d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            emit(Resource.Success(DailyQuestionsResponse(date = dateStr, questions = questions)))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load questions"))
        }
    }

    /**
     * Submit quiz results to API (best-effort, does not block completion).
     */
    fun submitQuiz(request: QuizSubmissionRequest): Flow<Resource<QuizSubmissionResponse>> = flow {
        emit(Resource.Loading())

        try {
            val token = tokenManager.getIdToken()
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
