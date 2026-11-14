package com.afyaquest.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.remote.dto.OrganizationDto
import com.afyaquest.app.data.repository.AuthRepository
import com.afyaquest.app.domain.model.User
import com.afyaquest.app.sync.ProgressSyncManager
import com.afyaquest.app.sync.SyncManager
import com.afyaquest.app.util.AuthErrors
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.apiErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * ViewModel for authentication screens.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val languageManager: LanguageManager,
    private val apiService: ApiService,
    private val progressSyncManager: ProgressSyncManager,
    private val syncManager: SyncManager
) : ViewModel() {

    val currentLanguage: StateFlow<String> = languageManager.getCurrentLanguageFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // Prefer persisted selection so the UI doesn't briefly/default to English.
            initialValue = languageManager.getCurrentLanguageBlocking()
        )

    suspend fun changeLanguage(languageCode: String) {
        languageManager.setLanguage(languageCode)
    }

    private companion object {
        /** How long sign-in waits for the account's progress before opening Home anyway. */
        const val SIGN_IN_RESTORE_WAIT_MS = 1_500L
        const val SIGN_OUT_SYNC_TIMEOUT_MS = 3_000L
        const val ORGANIZATIONS_TIMEOUT_MS = 15_000L
    }

    private val _loginState = MutableStateFlow<Resource<User>?>(null)
    val loginState: StateFlow<Resource<User>?> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<Resource<String>?>(null)
    val registerState: StateFlow<Resource<String>?> = _registerState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _organizations = MutableStateFlow<List<OrganizationDto>>(emptyList())
    val organizations: StateFlow<List<OrganizationDto>> = _organizations.asStateFlow()

    init {
        checkLoginStatus()
    }

    /**
     * Check if user is already logged in.
     */
    private fun checkLoginStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
    }

    /** null = not asked yet, Loading, Success, or Error when the list could not be loaded. */
    private val _organizationsState = MutableStateFlow<Resource<Unit>?>(null)
    val organizationsState: StateFlow<Resource<Unit>?> = _organizationsState.asStateFlow()

    /**
     * Load the organizations for the sign-up form. Only that screen needs them, so it asks;
     * before, every start of the app and every visit to Login fetched the list. Safe to call
     * again (Retry).
     */
    fun loadOrganizations() {
        if (_organizationsState.value is Resource.Loading) return
        if (_organizations.value.isNotEmpty()) return
        _organizationsState.value = Resource.Loading()
        viewModelScope.launch {
            _organizationsState.value = try {
                val response = withTimeoutOrNull(ORGANIZATIONS_TIMEOUT_MS) { apiService.getOrganizations() }
                val list = response?.takeIf { it.isSuccessful }?.body()?.organizations.orEmpty()
                if (list.isEmpty()) {
                    Resource.Error(AuthErrors.NO_CONNECTION)
                } else {
                    _organizations.value = list
                    Resource.Success(Unit)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Resource.Error(AuthErrors.NO_CONNECTION)
            }
        }
    }

    /**
     * Sign in.
     *
     * Home opens as soon as the password is accepted and this phone's progress store has been
     * handed to the account (local work). Restoring the account's progress from the server runs
     * in the background; it is given a short head start so Home usually opens with the right
     * numbers, but a slow network no longer holds the Sign In button for seconds.
     */
    fun login(email: String, password: String) {
        // The keyboard's Done key and the button can both fire; one attempt at a time.
        if (_loginState.value is Resource.Loading) return
        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            val result = authRepository.login(email.trim(), password)
            if (result is Resource.Success) {
                result.data?.let { user ->
                    // A failure in here must not undo a successful sign-in or crash the screen;
                    // Home repeats the restore when it loads.
                    runCatching {
                        // Carry over the login-screen language choice into the user session.
                        languageManager.setCurrentUser(user.id, inheritScreenLanguage = true)
                        progressSyncManager.onSignedIn(user.id, waitMs = SIGN_IN_RESTORE_WAIT_MS)
                    }
                }
                syncManager.clearError()
                _isLoggedIn.value = true
            }
            _loginState.value = result
        }
    }

    /**
     * Register new user.
     */
    fun register(
        email: String,
        password: String,
        name: String,
        phone: String?,
        organization: String? = null
    ) {
        if (_registerState.value is Resource.Loading) return
        _registerState.value = Resource.Loading()
        viewModelScope.launch {
            _registerState.value = authRepository.register(
                email.trim(), password, name.trim(), phone?.trim(), organization = organization
            )
        }
    }

    /**
     * Logout user. Progress earned offline is uploaded first (briefly, best effort) because the
     * session is needed for that; whatever could not be sent stays on the device, tagged with
     * its owner, and goes up the next time the same account signs in here.
     *
     * [onLoggedOut] runs on the main thread once the session is really gone. Navigate to the
     * login screen from there, not before: the session must be cleared before anyone can sign in
     * again, or the late clear would wipe the new session.
     */
    fun logout(onLoggedOut: () -> Unit = {}) {
        _isLoggedIn.value = false
        _loginState.value = null
        _registerState.value = null
        viewModelScope.launch(NonCancellable) {
            withTimeoutOrNull(SIGN_OUT_SYNC_TIMEOUT_MS) { progressSyncManager.pushIfDirty() }
            authRepository.logout()
            syncManager.clearError()
            languageManager.setCurrentUser(null)
            withContext(Dispatchers.Main) { onLoggedOut() }
        }
    }

    /**
     * Reset login state.
     */
    fun resetLoginState() {
        _loginState.value = null
    }

    /**
     * Reset register state.
     */
    fun resetRegisterState() {
        _registerState.value = null
    }

    /**
     * Request forgot password verification code.
     * Returns null on success, error message on failure.
     */
    suspend fun forgotPassword(email: String): String? {
        return try {
            val response = apiService.forgotPassword(mapOf("email" to email.trim()))
            if (response.isSuccessful) null
            else {
                apiErrorMessage(response.errorBody()?.string(), AuthErrors.SERVER)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthErrors.NO_CONNECTION
        }
    }

    /**
     * Confirm forgot password with verification code and new password.
     * Returns null on success, error message on failure.
     */
    suspend fun confirmForgotPassword(email: String, code: String, newPassword: String): String? {
        return try {
            val response = apiService.confirmForgotPassword(
                mapOf("email" to email.trim(), "code" to code.trim(), "newPassword" to newPassword)
            )
            if (response.isSuccessful) null
            else {
                apiErrorMessage(response.errorBody()?.string(), AuthErrors.SERVER)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthErrors.NO_CONNECTION
        }
    }
}
