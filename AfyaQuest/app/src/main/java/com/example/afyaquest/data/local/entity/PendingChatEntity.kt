package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending chat message waiting to be synced
 */
@Entity(tableName = "pending_chats")
data class PendingChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val message: String,
    val conversationHistory: String, // JSON string
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val responseReceived: Boolean = false,
    val response: String? = null
)
