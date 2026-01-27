package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing user achievements/badges.
 */
@Entity(
    tableName = "achievements",
    primaryKeys = ["userId", "achievementId"]
)
@TypeConverters(DateConverter::class)
data class AchievementEntity(
    val userId: String,
    val achievementId: String,
    val achievementName: String,
    val description: String,
    val category: String, // 'streak', 'quiz', 'video', 'report', 'level'
    val iconUrl: String? = null,
    val unlockedDate: Date = Date(),
    val isSynced: Boolean = false
)
