package com.afyaquest.app.data.remote

import com.afyaquest.app.util.TokenManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers a 401 from the API by refreshing the session once and repeating the request.
 *
 * Home fires several requests together; when the id token has run out they all come back 401 at
 * the same moment. The refresh is therefore done by one of them while the others wait, and the
 * ones that waited simply reuse the new token.
 *
 * When Cognito itself rejects the refresh token (30 days after sign-in, or revoked) the session
 * is over: it is cleared and the app returns to Login, instead of sitting on a Home screen where
 * every request quietly fails.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    /** One client for all refreshes, with deadlines of its own. */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(TimeoutDns())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    /** Where sessions are refreshed. Only tests point this somewhere else. */
    internal var refreshUrl: String = COGNITO_URL

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        // This request already carried a refreshed token and was still refused: give up.
        if (request.header(HEADER_REFRESHED) != null) return null
        // Not one of ours (no session token on it): nothing to refresh.
        val failedToken = request.header("Authorization")?.removePrefix("Bearer ") ?: return null

        val idToken = synchronized(this) {
            val current = tokenManager.getIdToken()
            if (current != null && current != failedToken) {
                // Another request refreshed while this one was waiting for the lock.
                current
            } else {
                val refreshToken = tokenManager.getRefreshToken() ?: return null
                when (val outcome = refresh(refreshToken)) {
                    is RefreshOutcome.Refreshed -> {
                        tokenManager.updateTokens(
                            accessToken = outcome.accessToken,
                            idToken = outcome.idToken,
                            refreshToken = outcome.refreshToken
                        )
                        outcome.idToken
                    }
                    RefreshOutcome.SessionOver -> {
                        tokenManager.expireSession()
                        return null
                    }
                    // No connection or a server hiccup: the session may well be fine, keep it.
                    RefreshOutcome.Unavailable -> return null
                }
            }
        }

        return request.newBuilder()
            .header("Authorization", "Bearer $idToken")
            .header(HEADER_REFRESHED, "true")
            .build()
    }

    private fun refresh(refreshToken: String): RefreshOutcome = try {
        val json = gson.toJson(
            CognitoRefreshRequest(
                authFlow = "REFRESH_TOKEN_AUTH",
                clientId = CLIENT_ID,
                authParameters = mapOf("REFRESH_TOKEN" to refreshToken)
            )
        )
        val request = Request.Builder()
            .url(refreshUrl)
            .addHeader("Content-Type", "application/x-amz-json-1.1")
            .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth")
            .post(json.toRequestBody("application/x-amz-json-1.1".toMediaType()))
            .build()

        refreshClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
            when {
                response.isSuccessful -> {
                    val tokens = body?.let { gson.fromJson(it, CognitoAuthResult::class.java) }?.authenticationResult
                    val newIdToken = tokens?.idToken
                    val newAccessToken = tokens?.accessToken
                    if (newIdToken.isNullOrBlank() || newAccessToken.isNullOrBlank()) {
                        RefreshOutcome.Unavailable
                    } else {
                        RefreshOutcome.Refreshed(newAccessToken, newIdToken, tokens.refreshToken)
                    }
                }
                isSessionOver(response.code, body) -> RefreshOutcome.SessionOver
                else -> RefreshOutcome.Unavailable
            }
        }
    } catch (e: Exception) {
        RefreshOutcome.Unavailable
    }

    private sealed class RefreshOutcome {
        data class Refreshed(val accessToken: String, val idToken: String, val refreshToken: String?) : RefreshOutcome()
        object SessionOver : RefreshOutcome()
        object Unavailable : RefreshOutcome()
    }

    private data class CognitoRefreshRequest(
        @SerializedName("AuthFlow") val authFlow: String,
        @SerializedName("ClientId") val clientId: String,
        @SerializedName("AuthParameters") val authParameters: Map<String, String>
    )

    private data class CognitoAuthResult(
        @SerializedName("AuthenticationResult") val authenticationResult: CognitoTokenResponse?
    )

    data class CognitoTokenResponse(
        @SerializedName("AccessToken") val accessToken: String?,
        @SerializedName("IdToken") val idToken: String?,
        /** Only present when the user pool rotates refresh tokens. */
        @SerializedName("RefreshToken") val refreshToken: String? = null
    )

    companion object {
        private const val COGNITO_URL = "https://cognito-idp.us-east-1.amazonaws.com/"
        private const val CLIENT_ID = "3qkth9j9bcvvhl9b6q7ml7qih"
        private const val HEADER_REFRESHED = "X-Token-Refreshed"

        /**
         * Cognito answers 400 with `NotAuthorizedException` (expired or revoked refresh token) or
         * `UserNotFoundException` (account deleted) when the session cannot continue. Anything
         * else (throttling, a 5xx) is temporary.
         */
        fun isSessionOver(httpCode: Int, body: String?): Boolean =
            httpCode in 400..499 && body != null &&
                (body.contains("NotAuthorizedException") || body.contains("UserNotFoundException"))
    }
}
