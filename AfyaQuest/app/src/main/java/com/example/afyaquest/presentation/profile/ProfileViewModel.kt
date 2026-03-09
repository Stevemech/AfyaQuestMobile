package com.example.afyaquest.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.domain.model.Achievement
import com.example.afyaquest.domain.model.WeeklyReflection
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.ProgressDataStore
import com.example.afyaquest.util.TokenManager
import com.example.afyaquest.util.XpData
import com.example.afyaquest.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile screens
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val xpManager: XpManager,
    private val languageManager: LanguageManager
) : ViewModel() {

    // XP data
    val xpData: StateFlow<XpData> = xpManager.getXpDataFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = XpData()
        )

    // Current language
    val currentLanguage: StateFlow<String> = languageManager.getCurrentLanguageFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LanguageManager.LANGUAGE_ENGLISH
        )

    // Quick stats computed from local progress data
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickStats()
    )

    // Achievements
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    // Weekly reflections (empty - no sample data)
    private val _weeklyReflections = MutableStateFlow<List<WeeklyReflection>>(emptyList())
    val weeklyReflections: StateFlow<List<WeeklyReflection>> = _weeklyReflections.asStateFlow()

    // Selected tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // User status based on local tracking
    private val _userStatus = MutableStateFlow("active")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    init {
        loadAchievements()
    }

    /**
     * Load achievements from API if online, otherwise empty list
     */
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
                // Offline or error - achievements remain empty
            }
        }
    }

    /**
     * Set selected tab
     */
    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * Submit weekly reflection
     */
    fun submitReflection(reflection: WeeklyReflection) {
        viewModelScope.launch {
            // TODO: Submit to API
            _weeklyReflections.value = listOf(reflection) + _weeklyReflections.value
        }
    }

    /**
     * Change language
     */
    suspend fun changeLanguage(languageCode: String) {
        languageManager.setLanguage(languageCode)
    }
}

data class QuickStats(
    val lessonsCompleted: Int = 0,
    val videosWatched: Int = 0,
    val quizzesCompleted: Int = 0,
    val reportsSubmitted: Int = 0
)
