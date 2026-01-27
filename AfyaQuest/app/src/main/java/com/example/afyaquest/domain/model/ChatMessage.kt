package com.example.afyaquest.domain.model

import java.time.LocalDateTime

/**
 * Chat message model
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Request for sending a chat message
 */
data class ChatRequest(
    val message: String,
    val conversationHistory: List<ConversationMessage>
)

/**
 * Conversation message for API
 */
data class ConversationMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

/**
 * Response from chat API
 */
data class ChatResponse(
    val success: Boolean,
    val response: String,
    val provider: String? = null,
    val timestamp: String? = null
)
