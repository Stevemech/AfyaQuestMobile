package com.example.afyaquest.domain.model

/**
 * Domain model representing a user.
 */
data class User(
    val id: String,
    val email: String,
    val name: String,
    val phone: String? = null,
    val role: String = "cha", // cha, supervisor, admin
    val language: String = "en",
    val level: Int = 1,
    val totalPoints: Int = 0,
    val rank: String = "Bronze CHA",
    val currentStreak: Int = 0,
    val isActive: Boolean = true
)
