package com.afyaquest.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in session: who is signed in, since when, and the tokens that prove it.
 *
 * Two files are kept:
 *  - the tokens, in an encrypted preferences file. Opening it means Android Keystore work that
 *    takes tens to hundreds of milliseconds, so it is opened on first use, not at start-up, and
 *    the values are then kept in memory;
 *  - who is signed in and when they signed in, in an ordinary preferences file. That is not a
 *    secret, and it is all the app needs to decide "Home or Login" before the first frame.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sessionPrefs: SharedPreferences =
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy { openSecurePrefs() }

    /** Tokens as last read or written, so API calls do not decrypt the file every time. */
    private val cache = HashMap<String, String?>()

    private val _sessionExpired = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Fires when the server no longer accepts the session (the refresh token has run out or was
     * revoked). The session is already cleared when this fires; the UI goes back to Login.
     */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private fun createSecurePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * A file restored from a backup, or copied from another phone, cannot be decrypted: its key
     * never leaves the Keystore of the phone that wrote it. Start clean in that case (the user
     * signs in again) instead of crashing on every launch.
     */
    private fun openSecurePrefs(): SharedPreferences = try {
        createSecurePrefs()
    } catch (e: Exception) {
        context.deleteSharedPreferences(SECURE_PREFS)
        sessionPrefs.edit().clear().apply()
        createSecurePrefs()
    }

    /** Open the encrypted file ahead of the first API call. Call from a background thread. */
    fun warmUp() {
        getIdToken()
    }

    /** Store a new session after a successful sign-in. */
    fun saveSession(accessToken: String, idToken: String, refreshToken: String, userId: String) {
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_ID_TOKEN, idToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        synchronized(cache) {
            cache[KEY_ACCESS_TOKEN] = accessToken
            cache[KEY_ID_TOKEN] = idToken
            cache[KEY_REFRESH_TOKEN] = refreshToken
        }
        sessionPrefs.edit()
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_SIGNED_IN_AT, System.currentTimeMillis())
            .putBoolean(KEY_MIGRATED, true)
            .apply()
    }

    /**
     * Store refreshed tokens. The sign-in time is deliberately left alone: the refresh token runs
     * out a fixed time after sign-in however often it is used, so a refresh must not make the
     * session look younger than it is.
     */
    fun updateTokens(accessToken: String, idToken: String, refreshToken: String? = null) {
        val editor = securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_ID_TOKEN, idToken)
        if (!refreshToken.isNullOrBlank()) editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
        synchronized(cache) {
            cache[KEY_ACCESS_TOKEN] = accessToken
            cache[KEY_ID_TOKEN] = idToken
            if (!refreshToken.isNullOrBlank()) cache[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    fun getAccessToken(): String? = secureValue(KEY_ACCESS_TOKEN)

    fun getIdToken(): String? = secureValue(KEY_ID_TOKEN)

    fun getRefreshToken(): String? = secureValue(KEY_REFRESH_TOKEN)

    private fun secureValue(key: String): String? = synchronized(cache) {
        if (!cache.containsKey(key)) cache[key] = securePrefs.getString(key, null)
        cache[key]
    }

    /**
     * Somebody is signed in and the session is still within the refresh token's lifetime.
     * Reads only the ordinary preferences file, so it is safe on the main thread at start-up.
     */
    fun isTokenValid(): Boolean {
        migrateFromEncryptedOnlyStorage()
        val signedInAt = sessionPrefs.getLong(KEY_SIGNED_IN_AT, 0L)
        val age = System.currentTimeMillis() - signedInAt
        return sessionPrefs.getString(KEY_USER_ID, null) != null && age in 0 until SESSION_LIFETIME_MS
    }

    fun getUserId(): String? {
        migrateFromEncryptedOnlyStorage()
        return sessionPrefs.getString(KEY_USER_ID, null)
    }

    /** Sign out locally: forget the tokens and who was signed in. */
    fun clearTokens() {
        securePrefs.edit().clear().apply()
        synchronized(cache) { cache.clear() }
        sessionPrefs.edit().clear().putBoolean(KEY_MIGRATED, true).apply()
    }

    /** The server has ended the session: sign out locally and tell the UI. */
    fun expireSession() {
        if (getUserId() == null) return
        clearTokens()
        _sessionExpired.tryEmit(Unit)
    }

    /**
     * Versions up to 1.3 kept the user id and the sign-in time inside the encrypted file. Copy
     * them over once, so an update does not sign anybody out.
     */
    private fun migrateFromEncryptedOnlyStorage() {
        if (sessionPrefs.getBoolean(KEY_MIGRATED, false)) return
        synchronized(this) {
            if (sessionPrefs.getBoolean(KEY_MIGRATED, false)) return
            val editor = sessionPrefs.edit().putBoolean(KEY_MIGRATED, true)
            val oldUserId = securePrefs.getString(KEY_USER_ID, null)
            val oldTimestamp = securePrefs.getLong(LEGACY_KEY_TOKEN_TIMESTAMP, 0L)
            if (oldUserId != null && securePrefs.getString(KEY_REFRESH_TOKEN, null) != null) {
                editor.putString(KEY_USER_ID, oldUserId).putLong(KEY_SIGNED_IN_AT, oldTimestamp)
            }
            editor.commit()
        }
    }

    companion object {
        const val SECURE_PREFS = "afyaquest_secure_prefs"
        const val SESSION_PREFS = "afyaquest_session"

        /** Cognito's refresh token is valid for 30 days from sign-in. */
        const val SESSION_LIFETIME_MS = 30L * 24 * 60 * 60 * 1000

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val LEGACY_KEY_TOKEN_TIMESTAMP = "token_timestamp"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SIGNED_IN_AT = "signed_in_at"
        private const val KEY_MIGRATED = "migrated_from_encrypted"
    }
}
