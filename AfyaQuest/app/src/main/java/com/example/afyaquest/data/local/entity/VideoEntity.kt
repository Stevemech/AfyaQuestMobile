package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing a video module.
 */
@Entity(tableName = "videos")
@TypeConverters(DateConverter::class)
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // 'hygiene', 'nutrition', 'emergency', etc.
    val s3Key: String, // S3 object key
    val cloudFrontUrl: String, // CDN URL for streaming
    val thumbnailUrl: String? = null,
    val duration: Int, // in seconds
    val hasQuiz: Boolean = false,
    val quizId: String? = null,
    val points: Int = 20,
    val order: Int = 0,
    val isDownloaded: Boolean = false, // For offline viewing
    val localFilePath: String? = null, // Path to downloaded file
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
