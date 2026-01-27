package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending quiz result waiting to be synced
 */
@Entity(tableName = "pending_quizzes")
data class PendingQuizEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val questionId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val livesChange: Int,
    val submittedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
