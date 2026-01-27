package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.StringListConverter

/**
 * Room entity representing a daily question.
 */
@Entity(tableName = "questions")
@TypeConverters(StringListConverter::class)
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val date: String, // Format: YYYY-MM-DD
    val question: String,
    val options: List<String>, // List of 4 options
    val correctAnswer: String, // The correct option text
    val correctAnswerIndex: Int, // Index (0-3) of correct answer
    val explanation: String,
    val category: String, // 'hygiene', 'nutrition', etc.
    val points: Int = 30,
    val difficulty: String = "beginner", // 'beginner', 'intermediate', 'advanced'
    val order: Int = 0 // Order for the day (1-3)
)
