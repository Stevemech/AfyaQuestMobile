package com.example.afyaquest.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for authentication API responses.
 */

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("idToken") val idToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Int,
    @SerializedName("user") val user: UserDto
)

data class RegisterResponse(
    @SerializedName("message") val message: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userConfirmed") val userConfirmed: Boolean
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String,
    @SerializedName("language") val language: String,
    @SerializedName("level") val level: Int,
    @SerializedName("totalPoints") val totalPoints: Int,
    @SerializedName("rank") val rank: String,
    @SerializedName("currentStreak") val currentStreak: Int,
    @SerializedName("isActive") val isActive: Boolean
)

data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("details") val details: String? = null
)
