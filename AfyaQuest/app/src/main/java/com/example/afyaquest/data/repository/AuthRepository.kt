package com.example.afyaquest.data.repository

import com.example.afyaquest.data.local.dao.UserDao
import com.example.afyaquest.data.local.entity.UserEntity
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.data.remote.dto.UserDto
import com.example.afyaquest.domain.model.User
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations.
 * Handles API calls and local data persistence.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) {

    /**
     * Register a new user.
     */
    fun register(
        email: String,
        password: String,
        name: String,
        phone: String?,
        role: String = "cha"
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        try {
            val request = mutableMapOf(
                "email" to email,
                "password" to password,
                "name" to name,
                "role" to role
            )
            if (!phone.isNullOrBlank()) {
                request["phone"] = phone
            }

            val response = apiService.register(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    tokenManager.saveUserId(body.userId)
                    emit(Resource.Success(body.message))
                } else {
                    emit(Resource.Error("Registration failed: Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(errorBody ?: "Registration failed"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Network error: ${e.localizedMessage}"))
        } catch (e: IOException) {
            emit(Resource.Error("Connection error: Please check your internet"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.localizedMessage}"))
        }
    }

    /**
     * Login user and save tokens.
     */
    fun login(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())

        try {
            val request = mapOf(
                "email" to email,
                "password" to password
            )

            val response = apiService.login(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Save tokens
                    tokenManager.saveTokens(
                        body.accessToken,
                        body.idToken,
                        body.refreshToken
                    )
                    tokenManager.saveUserId(body.user.id)

                    // Save user to local database
                    val userEntity = body.user.toUserEntity()
                    userDao.insertUser(userEntity)

                    // Map to domain model
                    val user = body.user.toUser()
                    emit(Resource.Success(user))
                } else {
                    emit(Resource.Error("Login failed: Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error(errorBody ?: "Invalid email or password"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Network error: ${e.localizedMessage}"))
        } catch (e: IOException) {
            emit(Resource.Error("Connection error: Please check your internet"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.localizedMessage}"))
        }
    }

    /**
     * Get current user from API.
     */
    fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())

        try {
            val idToken = tokenManager.getIdToken()
            if (idToken.isNullOrBlank()) {
                emit(Resource.Error("Not authenticated"))
                return@flow
            }

            val response = apiService.getCurrentUser("Bearer $idToken")

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Save to local database
                    val userEntity = body.toUserEntity()
                    userDao.insertUser(userEntity)

                    // Map to domain model
                    val user = body.toUser()
                    emit(Resource.Success(user))
                } else {
                    emit(Resource.Error("Failed to get user"))
                }
            } else {
                emit(Resource.Error("Failed to get user: ${response.code()}"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Network error: ${e.localizedMessage}"))
        } catch (e: IOException) {
            emit(Resource.Error("Connection error"))
        } catch (e: Exception) {
            emit(Resource.Error("Unexpected error: ${e.localizedMessage}"))
        }
    }

    /**
     * Get user from local database.
     */
    suspend fun getLocalUser(userId: String): User? {
        return userDao.getUserById(userId)?.toUser()
    }

    /**
     * Logout user.
     */
    fun logout() {
        tokenManager.clearTokens()
    }

    /**
     * Check if user is logged in.
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isTokenValid()
    }

    // Mapping extensions

    private fun UserDto.toUser() = User(
        id = id,
        email = email,
        name = name,
        phone = phone,
        role = role,
        language = language,
        level = level,
        totalPoints = totalPoints,
        rank = rank,
        currentStreak = currentStreak,
        isActive = isActive
    )

    private fun UserDto.toUserEntity() = UserEntity(
        id = id,
        name = name,
        email = email,
        role = role,
        phone = phone,
        language = language,
        level = level,
        totalPoints = totalPoints,
        rank = rank,
        currentStreak = currentStreak,
        isActive = isActive,
        createdAt = Date(),
        updatedAt = Date()
    )

    private fun UserEntity.toUser() = User(
        id = id,
        email = email,
        name = name,
        phone = phone,
        role = role,
        language = language,
        level = level,
        totalPoints = totalPoints,
        rank = rank,
        currentStreak = currentStreak,
        isActive = isActive
    )
}
