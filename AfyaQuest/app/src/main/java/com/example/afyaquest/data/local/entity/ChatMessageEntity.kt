package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing a chat message with the AI assistant (Steve).
 */
@Entity(tableName = "chat_messages")
@TypeConverters(DateConverter::class)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val message: String,
    val role: String, // 'user' or 'assistant'
    val response: String? = null, // Populated for assistant messages
    val isSynced: Boolean = false,
    val createdAt: Date = Date()
)
