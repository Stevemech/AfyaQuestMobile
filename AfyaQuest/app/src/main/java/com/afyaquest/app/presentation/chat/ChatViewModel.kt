package com.afyaquest.app.presentation.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.R
import com.afyaquest.app.data.repository.ChatRepository
import com.afyaquest.app.data.repository.ChatSendResult
import com.afyaquest.app.domain.model.ChatMessage
import com.afyaquest.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the chat with Fred.
 *
 * The conversation itself lives in the local database (see [ChatRepository]); this class adds
 * Fred's greeting on top, and the transient states around sending: typing, a failed send that
 * can be retried, and the note shown when a question was asked without a connection.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    companion object {
        private const val GREETING_ID = "initial"
        private const val ERROR_ID = "error"
    }

    /** Fixed at creation so it does not jump around while the screen is open. */
    private val greeting = ChatMessage(
        id = GREETING_ID,
        text = context.getString(R.string.fred_greeting),
        isUser = false,
        timestamp = LocalDateTime.now()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Whether Fred can be reached right now; questions asked while this is false are queued. */
    val isOnline: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), networkMonitor.isCurrentlyConnected())

    /** Localized, user-safe error text (never raw exception text). */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** The last user message Fred could not answer, kept so it can be resent with one tap. */
    private val _failedMessage = MutableStateFlow<String?>(null)
    val failedMessage: StateFlow<String?> = _failedMessage.asStateFlow()

    /** Greeting, then the stored conversation, then an error bubble while a send has failed. */
    val messages: StateFlow<List<ChatMessage>> =
        combine(chatRepository.observeMessages(), _errorMessage) { stored, error ->
            val history = listOf(greeting.copy(timestamp = stored.firstOrNull()?.timestamp ?: greeting.timestamp)) + stored
            if (error == null) history
            else history + ChatMessage(id = ERROR_ID, text = error, isUser = false, timestamp = LocalDateTime.now())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf(greeting))

    init {
        // A new phone starts with the conversation the account already had.
        viewModelScope.launch { chatRepository.restoreHistoryIfEmpty() }
    }

    /** Send a user message. */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isLoading.value) return
        deliver(trimmed) { chatRepository.send(trimmed) }
    }

    /** Resend the last failed message (it is already in the conversation). */
    fun retryFailedMessage() {
        val text = _failedMessage.value ?: return
        if (_isLoading.value) return
        deliver(text) { chatRepository.resend(text) }
    }

    private fun deliver(text: String, send: suspend () -> ChatSendResult) {
        _isLoading.value = true
        _errorMessage.value = null
        _failedMessage.value = null
        viewModelScope.launch {
            val result = try {
                send()
            } catch (e: Exception) {
                ChatSendResult.FAILED
            }
            if (result == ChatSendResult.FAILED) {
                _errorMessage.value = context.getString(R.string.something_went_wrong)
                _failedMessage.value = text
            }
            _isLoading.value = false
        }
    }

    /** Clear error message */
    fun clearError() {
        _errorMessage.value = null
    }

    /** Format time for display */
    fun formatTime(timestamp: LocalDateTime): String =
        String.format(Locale.ROOT, "%02d:%02d", timestamp.hour, timestamp.minute)
}
