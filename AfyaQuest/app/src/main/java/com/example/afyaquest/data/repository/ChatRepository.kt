package com.example.afyaquest.data.repository

import android.content.Context
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.ChatRequest
import com.example.afyaquest.domain.model.ChatResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AI Chat functionality
 */
@Singleton
class ChatRepository @Inject constructor(
    @ApplicationContext private val context: Context
    // TODO: Inject ApiService when backend is ready
) {

    /**
     * Send a message to the AI assistant
     * In production, this will call POST /chat/message
     */
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return try {
            // TODO: Replace with actual API call
            // val response = apiService.sendChatMessage(request)

            // Simulate API call with delay
            kotlinx.coroutines.delay(1500)

            // Mock response
            val mockResponse = ChatResponse(
                success = true,
                response = generateMockResponse(request.message),
                provider = "bedrock",
                timestamp = System.currentTimeMillis().toString()
            )

            Result.success(mockResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get chat history (currently managed client-side)
     */
    suspend fun getChatHistory(): Result<List<ChatResponse>> {
        return try {
            // TODO: Implement if backend adds chat history storage
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mock response generator for testing
     * Uses localized strings based on app's current language.
     * TODO: Remove when backend is integrated
     */
    private fun generateMockResponse(userMessage: String): String {
        val lowerMessage = userMessage.lowercase()
        // Swahili trigger words for when user writes in Swahili
        val s = { resId: Int -> context.getString(resId) }

        return when {
            lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("hey") ||
            lowerMessage.contains("habari") || lowerMessage.contains("jambo") || lowerMessage.contains("mambo") -> {
                s(R.string.fred_response_hello)
            }
            lowerMessage.contains("handwashing") || lowerMessage.contains("wash hands") ||
            lowerMessage.contains("kuosha") || lowerMessage.contains("mikono") -> {
                s(R.string.fred_response_handwashing)
            }
            lowerMessage.contains("malaria") -> {
                s(R.string.fred_response_malaria)
            }
            lowerMessage.contains("vaccination") || lowerMessage.contains("vaccine") ||
            lowerMessage.contains("chanjo") -> {
                s(R.string.fred_response_vaccination)
            }
            lowerMessage.contains("nutrition") || lowerMessage.contains("diet") ||
            lowerMessage.contains("lishe") || lowerMessage.contains("chakula") -> {
                s(R.string.fred_response_nutrition)
            }
            lowerMessage.contains("xp") || lowerMessage.contains("points") || lowerMessage.contains("level") -> {
                s(R.string.fred_response_xp)
            }
            lowerMessage.contains("streak") || lowerMessage.contains("mfululizo") -> {
                s(R.string.fred_response_streak)
            }
            lowerMessage.contains("thanks") || lowerMessage.contains("thank you") ||
            lowerMessage.contains("asante") || lowerMessage.contains("shukrani") -> {
                s(R.string.fred_response_thanks)
            }
            lowerMessage.contains("cpr") -> {
                s(R.string.fred_response_cpr)
            }
            else -> {
                s(R.string.fred_response_default)
            }
        }
    }
}
