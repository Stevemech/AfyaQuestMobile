package com.afyaquest.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.data.repository.AuthRepository
import com.afyaquest.app.domain.model.User
import com.afyaquest.app.domain.model.WeeklyReflection
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.XpData
import com.afyaquest.app.util.XpManager
import com.afyaquest.app.util.apiErrorMessage
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
        progressDataStore.getCompletedQuizzes(),
        progressDataStore.getReportDates()
    ) { videos, lessons, quizzes, reportDates ->
        QuickStats(
            lessonsCompleted = lessons.size,
            videosWatched = videos.size,
            quizzesCompleted = quizzes.size,
            reportsSubmitted = reportDates.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickStats())

    /**
     * Achievements, worked out from the account's real progress (see [AchievementRules]). The
     * screen resolves their names, so they follow the app language.
     */
    val achievements: StateFlow<List<AchievementStatus>> = combine(
        progressDataStore.getWatchedVideos(),
        progressDataStore.getCompletedQuizzes(),
        progressDataStore.getCompletedLessons(),
        progressDataStore.getReportDates(),
        xpManager.getXpDataFlow()
    ) { videos, quizzes, lessons, reportDates, xp ->
        AchievementRules.evaluate(
            AchievementInput(
                watchedVideos = videos,
                completedQuizzes = quizzes,
                completedLessons = lessons,
                reportDays = reportDates.size,
                totalXP = xp.totalXP,
                level = xp.level,
                longestStreak = xp.longestStreak
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AchievementRules.evaluate(AchievementInput()))

    // User profile from API
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _weeklyReflections = MutableStateFlow<List<WeeklyReflection>>(emptyList())
    val weeklyReflections: StateFlow<List<WeeklyReflection>> = _weeklyReflections.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /** Clocked in for the field day, as last confirmed by the server (the same value Home shows). */
    val clockedIn: StateFlow<Boolean> = progressDataStore.clockedIn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadUserProfile()
    }

    /**
     * Load user profile (name, email, phone, org) from API
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            // The copy saved on the phone first, so the name and organisation show without a
            // connection; the server's answer replaces it when it arrives.
            val saved = tokenManager.getUserId()?.let { id -> authRepository.getLocalUser(id) }
            if (saved != null && _userProfile.value == null) _userProfile.value = saved

            authRepository.getCurrentUser().collectLatest { result ->
                if (result is Resource.Success) {
                    _userProfile.value = result.data
                }
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
            else apiErrorMessage(response.errorBody()?.string(), "Failed to change password")
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
