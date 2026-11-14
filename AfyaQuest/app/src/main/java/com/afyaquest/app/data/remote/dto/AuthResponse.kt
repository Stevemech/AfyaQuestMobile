package com.afyaquest.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data transfer objects for authentication API responses.
 */

/**
 * Every field has a default on purpose. Gson fills a Kotlin class by reflection and happily puts
 * null into a non-null `String` when the JSON has no such field, which then blows up far from
 * here. With defaults Gson uses the constructor, and a missing field is simply null / absent.
 */
data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("idToken") val idToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("expiresIn") val expiresIn: Int? = null,
    @SerializedName("user") val user: UserDto? = null
)

data class RegisterResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("userConfirmed") val userConfirmed: Boolean? = null
)

/**
 * A profile as the server returns it. Old accounts and admin accounts do not have every field,
 * so all of them are optional here; `AuthRepository` fills the gaps when it maps to [User].
 */
data class UserDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("level") val level: Int? = null,
    @SerializedName("totalPoints") val totalPoints: Int? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("currentStreak") val currentStreak: Int? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("organization") val organization: String? = null,
    /** "active" while clocked in, "inactive" after clocking out, absent before the first clock-in. */
    @SerializedName("manualStatus") val manualStatus: String? = null
)

data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("details") val details: String? = null
)

// ==================== Clock In/Out ====================

data class ClockActionResponse(
    @SerializedName("message") val message: String,
    @SerializedName("manualStatus") val manualStatus: String,
    @SerializedName("timestamp") val timestamp: String
)

// ==================== Organizations ====================

data class OrganizationsResponse(
    val organizations: List<OrganizationDto>
)

data class OrganizationDto(
    val id: String,
    val name: String,
    val location: String
)

// ==================== Itineraries ====================

data class ItinerariesResponse(
    val itineraries: List<ItineraryDto>
)

data class ItineraryDto(
    val date: String,
    val stops: List<ItineraryStopDto>,
    val status: String
)

data class ItineraryStopDto(
    val order: Int,
    val houseId: String? = null,
    val label: String,
    val address: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val completed: Boolean = false
)

// ==================== Assignments ====================

data class AssignmentsResponse(
    val assignments: List<AssignmentDto>
)

data class AssignmentDto(
    val type: String? = null,
    val moduleId: String? = null,
    val lessonId: String? = null,
    val status: String? = null,
    val mandatory: Boolean = false,
    val dueDate: String? = null,
    val assignedAt: String? = null
)
