package com.example.afyaquest.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.data.remote.dto.OrganizationDto
import com.example.afyaquest.data.repository.AuthRepository
import com.example.afyaquest.domain.model.User
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val languageManager: LanguageManager,
    private val apiService: ApiService
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
        fetchOrganizations()
    }

    /**
     * Check if user is already logged in.
     */
    private fun checkLoginStatus() {
        _isLoggedIn.value = authRepository.isLoggedIn()
    }

    /**
     * Fetch available organizations from the API.
     */
    private fun fetchOrganizations() {
        viewModelScope.launch {
            try {
                val response = apiService.getOrganizations()
                if (response.isSuccessful) {
                    response.body()?.let {
                        _organizations.value = it.organizations
                    }
                }
            } catch (_: Exception) {
                // Silently fail — organizations list stays empty
            }
        }
    }

    /**
     * Login user.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email.trim(), password)
                .collect { resource ->
                    _loginState.value = resource
                    if (resource is Resource.Success) {
                        _isLoggedIn.value = true
                        // Carry over the login-screen language choice into the user session.
                        resource.data?.let { user ->
                            languageManager.setCurrentUser(user.id, inheritScreenLanguage = true)
                        }
                    }
                }
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
        viewModelScope.launch {
            authRepository.register(email.trim(), password, name.trim(), phone?.trim(), organization = organization)
                .collect { resource ->
                    _registerState.value = resource
                }
        }
    }

    /**
     * Logout user.
     */
    fun logout() {
        authRepository.logout()
        languageManager.setCurrentUser(null)
        _isLoggedIn.value = false
        _loginState.value = null
        _registerState.value = null
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
}
