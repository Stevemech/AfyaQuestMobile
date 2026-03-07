package com.example.afyaquest.data.remote

import com.example.afyaquest.util.TokenManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    companion object {
        private const val COGNITO_URL = "https://cognito-idp.af-south-1.amazonaws.com/"
        private const val CLIENT_ID = "3oj94klb6jejp4lbal9ninv870"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we already tried refreshing
        if (response.request.header("X-Token-Refreshed") != null) {
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        val newTokens = refreshTokens(refreshToken) ?: return null

        tokenManager.saveTokens(
            newTokens.accessToken,
            newTokens.idToken,
            refreshToken
        )

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .header("X-Token-Refreshed", "true")
            .build()
    }

    private fun refreshTokens(refreshToken: String): CognitoTokenResponse? {
        return try {
            val json = Gson().toJson(
                CognitoRefreshRequest(
                    authFlow = "REFRESH_TOKEN_AUTH",
                    clientId = CLIENT_ID,
                    authParameters = mapOf("REFRESH_TOKEN" to refreshToken)
                )
            )

            val request = Request.Builder()
                .url(COGNITO_URL)
                .addHeader("Content-Type", "application/x-amz-json-1.1")
                .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth")
                .post(json.toRequestBody("application/x-amz-json-1.1".toMediaType()))
                .build()

            val client = OkHttpClient.Builder().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val result = Gson().fromJson(body, CognitoAuthResult::class.java)
                result.authenticationResult
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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
        @SerializedName("AccessToken") val accessToken: String,
        @SerializedName("IdToken") val idToken: String
    )
}
