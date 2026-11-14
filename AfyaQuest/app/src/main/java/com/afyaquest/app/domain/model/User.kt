package com.afyaquest.app.domain.model

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
    val level: Int = 0,
    val totalPoints: Int = 0,
    val rank: String = "Beginner",
    val currentStreak: Int = 0,
    /** The account is enabled. This is NOT the clock-in status, see [clockedIn]. */
    val isActive: Boolean = true,
    val organization: String? = null,
    /** Clocked in for the field day (the profile's manualStatus, the value supervisors see). */
    val clockedIn: Boolean = false
)
