package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import com.example.afyaquest.data.local.converters.StringListConverter
import java.util.Date

/**
 * Room entity representing a learning lesson.
 * Supports bilingual content (English and Swahili).
 */
@Entity(tableName = "lessons")
@TypeConverters(DateConverter::class, StringListConverter::class)
data class LessonEntity(
    @PrimaryKey
    val id: String,
    val titleEn: String,
    val titleSw: String,
    val descriptionEn: String,
    val descriptionSw: String,
    val category: String, // 'hygiene', 'nutrition', 'emergency', 'prevention', 'maternal', 'general'
    val difficulty: String = "beginner", // 'beginner', 'intermediate', 'advanced'
    val contentEn: String,
    val contentSw: String,
    val objectivesEn: List<String> = emptyList(),
    val objectivesSw: List<String> = emptyList(),
    val duration: Int, // in minutes
    val points: Int = 50,
    val prerequisites: List<String> = emptyList(), // List of lesson IDs
    val quizId: String? = null,
    val order: Int = 0,
    val isPublished: Boolean = false,
    val createdBy: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
