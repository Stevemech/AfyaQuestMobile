package com.example.afyaquest.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.data.repository.AuthRepository
import com.example.afyaquest.domain.model.Achievement
import com.example.afyaquest.domain.model.User
import com.example.afyaquest.domain.model.WeeklyReflection
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.ProgressDataStore
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.TokenManager
import com.example.afyaquest.util.XpData
import com.example.afyaquest.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val xpManager: XpManager,
    private val languageManager: LanguageManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    val xpData: StateFlow<XpData> = xpManager.getXpDataFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XpData())

    val currentLanguage: StateFlow<String> = languageManager.getCurrentLanguageFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageManager.LANGUAGE_ENGLISH)

    val quickStats: StateFlow<QuickStats> = combine(
        progressDataStore.getWatchedVideos(),
        progressDataStore.getCompletedLessons(),
        progressDataStore.getCompletedQuizzes()
    ) { videos, lessons, quizzes ->
        QuickStats(
            lessonsCompleted = lessons.size,
            videosWatched = videos.size,
            quizzesCompleted = quizzes.size,
            reportsSubmitted = 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickStats())

    // User profile from API
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _weeklyReflections = MutableStateFlow<List<WeeklyReflection>>(emptyList())
    val weeklyReflections: StateFlow<List<WeeklyReflection>> = _weeklyReflections.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _userStatus = MutableStateFlow("active")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    init {
        loadUserProfile()
        loadAchievements()
    }

    /**
     * Load user profile (name, email, phone, org) from API
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { result ->
                if (result is Resource.Success) {
                    _userProfile.value = result.data
                    Log.d("ProfileVM", "Profile loaded: ${result.data?.name}")
                }
            }
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getIdToken() ?: return@launch
                val response = apiService.getUserProgress("Bearer $token")
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    @Suppress("UNCHECKED_CAST")
                    val achievementsList = body["achievements"] as? List<Map<String, Any>>
                    if (achievementsList != null) {
                        _achievements.value = achievementsList.mapNotNull { map ->
                            try {
                                Achievement(
                                    id = map["id"]?.toString() ?: return@mapNotNull null,
                                    title = map["title"]?.toString() ?: "",
                                    description = map["description"]?.toString() ?: "",
                                    icon = map["icon"]?.toString() ?: "",
                                    category = try {
                                        val catStr = map["category"]?.toString()?.uppercase() ?: "LEARNING"
                                        com.example.afyaquest.domain.model.AchievementCategory.valueOf(catStr)
                                    } catch (e: Exception) {
                                        com.example.afyaquest.domain.model.AchievementCategory.LEARNING
                                    },
                                    unlocked = map["unlocked"] as? Boolean ?: false,
                                    unlockedDate = map["unlockedDate"]?.toString(),
                                    progress = (map["progress"] as? Number)?.toInt() ?: 0,
                                    target = (map["target"] as? Number)?.toInt() ?: 0
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Offline — achievements remain empty
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun submitReflection(reflection: WeeklyReflection) {
        viewModelScope.launch {
            _weeklyReflections.value = listOf(reflection) + _weeklyReflections.value
        }
    }

    suspend fun changeLanguage(languageCode: String) {
        languageManager.setLanguage(languageCode)
    }

    /**
     * Change password via API. Returns null on success, error message on failure.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): String? {
        return try {
            val token = tokenManager.getIdToken() ?: return "Not authenticated"
            val response = apiService.changePassword(
                "Bearer $token",
                mapOf("currentPassword" to currentPassword, "newPassword" to newPassword)
            )
            if (response.isSuccessful) null
            else response.errorBody()?.string() ?: "Failed to change password"
        } catch (e: Exception) {
            e.localizedMessage ?: "Network error"
        }
    }
}

data class QuickStats(
    val lessonsCompleted: Int = 0,
    val videosWatched: Int = 0,
    val quizzesCompleted: Int = 0,
    val reportsSubmitted: Int = 0
)
