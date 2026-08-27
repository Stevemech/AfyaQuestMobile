package com.afyaquest.app.presentation.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.R
import com.afyaquest.app.data.repository.ChatRepository
import com.afyaquest.app.domain.model.ChatMessage
import com.afyaquest.app.domain.model.ChatRequest
import com.afyaquest.app.domain.model.ConversationMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val GREETING_ID = "initial"
        private const val ERROR_ID_PREFIX = "error-"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Localized, user-safe error text (never raw exception text). */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** The last user message that failed to send, kept so it can be resent with one tap. */
    private val _failedMessage = MutableStateFlow<String?>(null)
    val failedMessage: StateFlow<String?> = _failedMessage.asStateFlow()

    init {
        // Add initial greeting from Fred
        addInitialGreeting()
    }

    /**
     * Add Fred's greeting message
     */
    private fun addInitialGreeting() {
        val greeting = ChatMessage(
            id = GREETING_ID,
            text = context.getString(R.string.fred_greeting),
            isUser = false,
            timestamp = LocalDateTime.now()
        )
        _messages.value = listOf(greeting)
    }

    /** True once the user has sent at least one message (hides the suggestion chips). */
    fun hasUserMessages(): Boolean = _messages.value.any { it.isUser }

    /**
     * Send a user message
     */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        // Add user message bubble immediately
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = trimmed,
            isUser = true,
            timestamp = LocalDateTime.now()
        )
        _messages.value = _messages.value + userMessage
        deliver(trimmed)
    }

    /**
     * Resend the last failed message. The user's bubble is already in the list,
     * so only the error bubble is removed before trying again.
     */
    fun retryFailedMessage() {
        val text = _failedMessage.value ?: return
        if (_isLoading.value) return
        _messages.value = _messages.value.filterNot { it.id.startsWith(ERROR_ID_PREFIX) }
        deliver(text)
    }

    private fun deliver(text: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _failedMessage.value = null

                // Conversation history: skip the greeting and any error bubbles
                val conversationHistory = _messages.value
                    .filter { it.id != GREETING_ID && !it.id.startsWith(ERROR_ID_PREFIX) }
                    .map { msg ->
                        ConversationMessage(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        )
                    }

                val request = ChatRequest(
                    message = text,
                    conversationHistory = conversationHistory
                )

                val result = chatRepository.sendMessage(request)
                val response = result.getOrNull()
                if (result.isSuccess && response != null && response.success) {
                    val aiMessage = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        text = response.response,
                        isUser = false,
                        timestamp = LocalDateTime.now()
                    )
                    _messages.value = _messages.value + aiMessage
                } else {
                    throw result.exceptionOrNull() ?: IllegalStateException("Invalid response from server")
                }
            } catch (e: Exception) {
                Log.d("ChatViewModel", "Fred reply failed: ${e.message}")
                val generic = context.getString(R.string.something_went_wrong)
                val errorBubble = ChatMessage(
                    id = ERROR_ID_PREFIX + System.currentTimeMillis(),
                    text = generic,
                    isUser = false,
                    timestamp = LocalDateTime.now()
                )
                _messages.value = _messages.value + errorBubble
                _errorMessage.value = generic
                _failedMessage.value = text
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
