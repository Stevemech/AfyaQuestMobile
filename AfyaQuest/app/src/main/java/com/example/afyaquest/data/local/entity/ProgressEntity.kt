package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity tracking user progress on lessons and videos.
 */
@Entity(
    tableName = "progress",
    primaryKeys = ["userId", "contentId", "contentType"]
)
@TypeConverters(DateConverter::class)
data class ProgressEntity(
    val userId: String,
    val contentId: String, // Lesson ID or Video ID
    val contentType: String, // 'lesson' or 'video'
    val completed: Boolean = false,
    val score: Int? = null, // Quiz score if applicable
    val progressPercentage: Int = 0, // 0-100
    val watchTime: Int? = null, // For videos, in seconds
    val isSynced: Boolean = false,
    val completedAt: Date? = null,
    val lastAccessedAt: Date = Date()
)
