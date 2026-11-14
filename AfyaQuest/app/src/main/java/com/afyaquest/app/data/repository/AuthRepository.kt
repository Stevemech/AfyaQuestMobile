package com.afyaquest.app.data.repository

import com.afyaquest.app.data.local.dao.UserDao
import com.afyaquest.app.data.local.entity.UserEntity
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.UserDto
import com.afyaquest.app.domain.model.User
import com.afyaquest.app.util.AuthErrors
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.apiErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.InterruptedIOException
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

    /** The profile as last fetched, so the screens that open together share one request. */
    private val profileLock = Mutex()
    private var cachedProfile: CachedProfile? = null

    private class CachedProfile(val user: User, val fetchedAt: Long)

    /**
     * Register a new user. On failure the message is one of [AuthErrors] or the server's own text.
     */
    suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String?,
        role: String = "cha",
        organization: String? = null
    ): Resource<String> = withContext(Dispatchers.IO) {
        guarded(REGISTER_TIMEOUT_MS) {
            val request = mutableMapOf(
                "email" to email,
                "password" to password,
                "name" to name,
                "role" to role
            )
            if (!phone.isNullOrBlank()) request["phone"] = phone
            if (!organization.isNullOrBlank()) request["organization"] = organization

            val response = apiService.register(request)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.message.orEmpty())
            } else {
                val serverText = apiErrorMessage(response.errorBody()?.string(), "")
                Resource.Error(
                    if (response.code() >= 500 || serverText.isBlank()) AuthErrors.SERVER else serverText
                )
            }
        }
    }

    /**
     * Sign in. Nothing is stored unless everything succeeded: a session saved halfway would open
     * Home on the next launch with nobody really signed in.
     *
     * The whole attempt has one deadline, so the Sign In button can never spin for ever. On
     * failure the message is one of [AuthErrors] or the server's own text.
     */
    suspend fun login(email: String, password: String): Resource<User> = withContext(Dispatchers.IO) {
        guarded(SIGN_IN_TIMEOUT_MS) {
            val response = apiService.login(mapOf("email" to email, "password" to password))
            if (!response.isSuccessful) {
                val serverText = apiErrorMessage(response.errorBody()?.string(), "")
                return@guarded Resource.Error(AuthErrors.forSignIn(response.code(), serverText))
            }

            val body = response.body()
            val dto = body?.user
            val user = dto?.toUserOrNull()
            val accessToken = body?.accessToken
            val idToken = body?.idToken
            val refreshToken = body?.refreshToken
            if (dto == null || user == null ||
                accessToken.isNullOrBlank() || idToken.isNullOrBlank() || refreshToken.isNullOrBlank()
            ) {
                // The password was right but the account has no usable profile behind it.
                return@guarded Resource.Error(AuthErrors.ACCOUNT_INCOMPLETE)
            }

            tokenManager.saveSession(accessToken, idToken, refreshToken, user.id)
            userDao.insertUser(user.toEntity())
            profileLock.withLock { cachedProfile = CachedProfile(user, System.currentTimeMillis()) }
            Resource.Success(user)
        }
    }

    /**
     * The signed-in user's profile.
     *
     * Home, Profile and Settings each ask for it when they open. Callers that arrive while a
     * request is on its way wait for that one, and a profile fetched in the last
     * [PROFILE_FRESH_MS] is reused, so opening the app costs one request instead of three.
     */
    fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        emit(withContext(Dispatchers.IO) { fetchCurrentUser() })
    }

    private suspend fun fetchCurrentUser(): Resource<User> = profileLock.withLock {
        cachedProfile
            ?.takeIf { System.currentTimeMillis() - it.fetchedAt < PROFILE_FRESH_MS && it.user.id == tokenManager.getUserId() }
            ?.let { return@withLock Resource.Success(it.user) }

        guarded(PROFILE_TIMEOUT_MS) {
            val idToken = tokenManager.getIdToken()
            if (idToken.isNullOrBlank()) return@guarded Resource.Error("Not authenticated")

            val response = apiService.getCurrentUser("Bearer $idToken")
            val user = response.body()?.toUserOrNull()
            if (!response.isSuccessful || user == null) {
                return@guarded Resource.Error("Failed to get user: ${response.code()}")
            }
            userDao.insertUser(user.toEntity())
            cachedProfile = CachedProfile(user, System.currentTimeMillis())
            Resource.Success(user)
        }
    }

    /**
     * Get user from local database.
     */
    suspend fun getLocalUser(userId: String): User? {
        return userDao.getUserById(userId)?.toUser()
    }

    /**
     * Clock in or clock out.
     * @param action "clock_in" or "clock_out"
     */
    fun clockAction(action: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        emit(
            withContext(Dispatchers.IO) {
                guarded(PROFILE_TIMEOUT_MS) {
                    val idToken = tokenManager.getIdToken()
                    if (idToken.isNullOrBlank()) return@guarded Resource.Error("Not authenticated")

                    val response = apiService.clockAction("Bearer $idToken", mapOf("action" to action))
                    if (response.isSuccessful) {
                        // The cached profile still carries the old clock status.
                        profileLock.withLock { cachedProfile = null }
                        val fallback = if (action == "clock_in") "active" else "inactive"
                        Resource.Success(response.body()?.manualStatus ?: fallback)
                    } else {
                        Resource.Error("Failed to ${action.replace('_', ' ')}")
                    }
                }
            }
        )
    }

    /**
     * Logout user.
     */
    fun logout() {
        tokenManager.clearTokens()
        cachedProfile = null
    }

    /**
     * Check if user is logged in.
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isTokenValid()
    }

    /**
     * Runs a network action with a deadline and turns every failure into an explainable error.
     * The deadline is a coroutine timeout, so it also ends a request stuck in a name lookup.
     */
    private suspend fun <T> guarded(timeoutMs: Long, action: suspend () -> Resource<T>): Resource<T> = try {
        withTimeout(timeoutMs) { action() }
    } catch (e: TimeoutCancellationException) {
        Resource.Error(AuthErrors.TIMEOUT)
    } catch (e: CancellationException) {
        throw e
    } catch (e: InterruptedIOException) {
        // OkHttp's own timeouts (connect, read, whole call).
        Resource.Error(AuthErrors.TIMEOUT)
    } catch (e: IOException) {
        Resource.Error(AuthErrors.NO_CONNECTION)
    } catch (e: Exception) {
        Resource.Error(AuthErrors.SERVER)
    }

    // Mapping

    /** Null when the profile has no id: there is no account to sign in to. */
    private fun UserDto.toUserOrNull(): User? {
        val userId = id?.takeIf { it.isNotBlank() } ?: return null
        return User(
            id = userId,
            email = email.orEmpty(),
            name = name.orEmpty(),
            phone = phone,
            role = role?.takeIf { it.isNotBlank() } ?: "cha",
            language = language?.takeIf { it.isNotBlank() } ?: "en",
            level = level ?: 0,
            totalPoints = totalPoints ?: 0,
            rank = rank?.takeIf { it.isNotBlank() } ?: "Beginner",
            currentStreak = currentStreak ?: 0,
            isActive = isActive ?: true,
            organization = organization,
            clockedIn = manualStatus == "active"
        )
    }

    private fun User.toEntity() = UserEntity(
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
        organization = organization,
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
        isActive = isActive,
        organization = organization
    )

    private companion object {
        /** Long enough for a cold server on a slow link, short enough that nobody gives up first. */
        const val SIGN_IN_TIMEOUT_MS = 20_000L
        const val REGISTER_TIMEOUT_MS = 25_000L
        const val PROFILE_TIMEOUT_MS = 20_000L

        /** Screens that open within this window share one profile request. */
        const val PROFILE_FRESH_MS = 30_000L
    }
}
