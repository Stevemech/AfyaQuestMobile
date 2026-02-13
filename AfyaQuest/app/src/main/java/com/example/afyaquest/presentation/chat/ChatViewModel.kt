package com.example.afyaquest.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.repository.ChatRepository
import com.example.afyaquest.domain.model.ChatMessage
import com.example.afyaquest.domain.model.ChatRequest
import com.example.afyaquest.domain.model.ConversationMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for AI Chat screen
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Add initial greeting from Fred
        addInitialGreeting()
    }

    /**
     * Add Fred's greeting message
     */
    private fun addInitialGreeting() {
        val greeting = ChatMessage(
            id = "initial",
            text = "Hey there! I'm Fred, your friendly AI health assistant for Afya Quest. I'm here to help you with health education, study tips, and any questions about the platform. What can I help you learn today? 😊",
            isUser = false,
            timestamp = LocalDateTime.now()
        )
        _messages.value = listOf(greeting)
    }

    /**
     * Send a user message
     */
    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            try {
                // Add user message
                val userMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = text.trim(),
                    isUser = true,
                    timestamp = LocalDateTime.now()
                )
                _messages.value = _messages.value + userMessage

                // Set loading state
                _isLoading.value = true
                _errorMessage.value = null

                // Prepare conversation history (skip initial greeting)
                val conversationHistory = _messages.value
                    .drop(1) // Skip initial greeting
                    .map { msg ->
                        ConversationMessage(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        )
                    }

                // Send to API
                val request = ChatRequest(
                    message = text.trim(),
                    conversationHistory = conversationHistory
                )

                val result = chatRepository.sendMessage(request)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response != null && response.success) {
                        // Add AI response
                        val aiMessage = ChatMessage(
                            id = (System.currentTimeMillis() + 1).toString(),
                            text = response.response,
                            isUser = false,
                            timestamp = LocalDateTime.now()
                        )
                        _messages.value = _messages.value + aiMessage
                    } else {
                        throw Exception("Invalid response from server")
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }

            } catch (e: Exception) {
                // Add error message
                val errorMsg = ChatMessage(
                    id = (System.currentTimeMillis() + 1).toString(),
                    text = "Sorry, I encountered an error. Please try again. ${e.message ?: ""}",
                    isUser = false,
                    timestamp = LocalDateTime.now()
                )
                _messages.value = _messages.value + errorMsg
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Format time for display
     */
    fun formatTime(timestamp: LocalDateTime): String {
        return String.format(
            "%02d:%02d",
            timestamp.hour,
            timestamp.minute
        )
    }
}
