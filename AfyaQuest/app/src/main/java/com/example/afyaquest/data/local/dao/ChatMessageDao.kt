package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ChatMessage operations.
 */
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getChatHistory(userId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY createdAt ASC")
    fun getChatHistoryFlow(userId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedMessages(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isSynced = 1 WHERE id = :messageId")
    suspend fun markMessageAsSynced(messageId: String)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun deleteChatHistory(userId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}
