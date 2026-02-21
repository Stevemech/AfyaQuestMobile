package com.example.afyaquest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing a user profile.
 * Mirrors the User model from the backend.
 */
@Entity(tableName = "users")
@TypeConverters(DateConverter::class)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val role: String, // 'cha', 'supervisor', 'admin'
    val phone: String? = null,
    val location: String? = null,
    val supervisorId: String? = null,
    val language: String = "en", // 'en' or 'sw'
    val level: Int = 0,
    val totalPoints: Int = 0,
    val rank: String = "Beginner",
    val profilePictureUrl: String? = null,
    val currentStreak: Int = 0,
    val lastActiveDate: Date? = null,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
