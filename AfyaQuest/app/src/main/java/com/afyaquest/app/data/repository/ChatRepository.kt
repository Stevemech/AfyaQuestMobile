package com.afyaquest.app.data.repository

import android.util.Log
import com.afyaquest.app.data.local.dao.ChatMessageDao
import com.afyaquest.app.data.local.entity.ChatMessageEntity
import com.afyaquest.app.data.local.entity.PendingChatEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.domain.model.ChatMessage
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** What happened to a message the user sent to Fred. */
enum class ChatSendResult {
    /** Fred answered; the answer is in the conversation. */
    ANSWERED,

    /** No connection: the question is saved and will be sent, and answered, when back online. */
    QUEUED,

    /** The server could not answer (not a connectivity problem). The user can resend. */
    FAILED
}

/**
 * Conversation with Fred, the AI health assistant (POST /chat/message).
 *
 * The conversation is kept in the local database, so it is still there after the app restarts
 * and without a connection. A question asked offline is queued through [SyncManager]; when the
 * queue is flushed the answer is written to the same table and simply appears in the chat.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val chatMessageDao: ChatMessageDao,
    private val syncManager: SyncManager
) {

    /** The signed-in user's conversation, oldest first. Empty when nobody is signed in. */
    fun observeMessages(): Flow<List<ChatMessage>> {
        val userId = tokenManager.getUserId() ?: return flowOf(emptyList())
        return chatMessageDao.getChatHistoryFlow(userId).map { rows -> rows.map { it.toChatMessage() } }
    }

    /**
     * On a phone that has no conversation yet (new install, new phone), load the recent
     * conversation the server kept. Best effort: silently does nothing offline.
     */
    suspend fun restoreHistoryIfEmpty() {
        try {
            val userId = tokenManager.getUserId() ?: return
            val token = tokenManager.getIdToken() ?: return
            if (chatMessageDao.getChatHistory(userId).isNotEmpty()) return
            val response = apiService.getChatHistory("Bearer $token")
            val exchanges = response.takeIf { it.isSuccessful }?.body() ?: return
            exchanges.forEach { exchange ->
                val question = exchange["message"] as? String ?: return@forEach
                val answer = exchange["response"] as? String
                val at = parseInstant(exchange["createdAt"] as? String)
                val id = exchange["id"] as? String ?: UUID.randomUUID().toString()
                chatMessageDao.insertMessage(
                    ChatMessageEntity(
                        id = "$id-q", userId = userId, message = question, role = ROLE_USER,
                        isSynced = true, createdAt = Date(at.toEpochMilli())
                    )
                )
                if (!answer.isNullOrBlank()) {
                    chatMessageDao.insertMessage(
                        ChatMessageEntity(
                            id = "$id-a", userId = userId, message = answer, role = ROLE_ASSISTANT,
                            isSynced = true, createdAt = Date(at.toEpochMilli() + 1)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Chat history not restored: ${e.message}")
        }
    }

    /** Add [text] to the conversation and ask Fred. */
    suspend fun send(text: String): ChatSendResult {
        val userId = tokenManager.getUserId() ?: return ChatSendResult.FAILED
        val messageId = UUID.randomUUID().toString()
        chatMessageDao.insertMessage(
            ChatMessageEntity(id = messageId, userId = userId, message = text, role = ROLE_USER, isSynced = false)
        )
        return deliver(userId, messageId, text)
    }

    /** Ask again for a question that is already in the conversation but got no answer. */
    suspend fun resend(text: String): ChatSendResult {
        val userId = tokenManager.getUserId() ?: return ChatSendResult.FAILED
        val pending = chatMessageDao.getUnsyncedMessages()
            .lastOrNull { it.userId == userId && it.role == ROLE_USER && it.message == text }
            ?: return send(text)
        return deliver(userId, pending.id, text)
    }

    private suspend fun deliver(userId: String, messageId: String, text: String): ChatSendResult {
        val token = tokenManager.getIdToken() ?: return ChatSendResult.FAILED
        return try {
            val response = apiService.sendChatMessage("Bearer $token", mapOf("message" to text))
            val answer = response.body()?.get("response")
            if (response.isSuccessful && !answer.isNullOrBlank()) {
                chatMessageDao.markMessageAsSynced(messageId)
                chatMessageDao.insertMessage(
                    ChatMessageEntity(
                        id = UUID.randomUUID().toString(), userId = userId, message = answer,
                        role = ROLE_ASSISTANT, isSynced = true
                    )
                )
                ChatSendResult.ANSWERED
            } else {
                Log.d(TAG, "Fred could not answer: HTTP ${response.code()}")
                ChatSendResult.FAILED
            }
        } catch (e: IOException) {
            // No connection: keep the question and let the sync queue deliver it later.
            syncManager.queueChat(PendingChatEntity(userId = userId, message = text, conversationHistory = "[]"))
            ChatSendResult.QUEUED
        } catch (e: Exception) {
            Log.d(TAG, "Fred request failed: ${e.message}")
            ChatSendResult.FAILED
        }
    }

    private fun ChatMessageEntity.toChatMessage() = ChatMessage(
        id = id,
        text = message,
        isUser = role == ROLE_USER,
        timestamp = LocalDateTime.ofInstant(createdAt.toInstant(), ZoneId.systemDefault()),
        pending = role == ROLE_USER && !isSynced
    )

    private fun parseInstant(value: String?): Instant = try {
        Instant.parse(value)
    } catch (e: Exception) {
        Instant.now()
    }

    companion object {
        private const val TAG = "ChatRepository"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
