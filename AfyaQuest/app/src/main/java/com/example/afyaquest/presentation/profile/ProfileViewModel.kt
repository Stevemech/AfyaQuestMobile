package com.example.afyaquest.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.domain.model.Achievement
import com.example.afyaquest.domain.model.AchievementCategory
import com.example.afyaquest.domain.model.WeeklyReflection
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.XpData
import com.example.afyaquest.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile screens
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
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

    // Achievements
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    // Weekly reflections
    private val _weeklyReflections = MutableStateFlow<List<WeeklyReflection>>(emptyList())
    val weeklyReflections: StateFlow<List<WeeklyReflection>> = _weeklyReflections.asStateFlow()

    // Selected tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadAchievements()
        loadWeeklyReflections()
    }

    /**
     * Load achievements
     */
    private fun loadAchievements() {
        // TODO: Fetch from API
        _achievements.value = getSampleAchievements()
    }

    /**
     * Load weekly reflections
     */
    private fun loadWeeklyReflections() {
        // TODO: Fetch from API
        _weeklyReflections.value = getSampleReflections()
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

    /**
     * Get sample achievements
     */
    private fun getSampleAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "1",
                title = "First Steps",
                description = "Complete your first daily question",
                icon = "🎯",
                category = AchievementCategory.LEARNING,
                unlocked = true,
                unlockedDate = "2024-01-15"
            ),
            Achievement(
                id = "2",
                title = "Week Warrior",
                description = "Maintain a 7-day streak",
                icon = "🔥",
                category = AchievementCategory.CONSISTENCY,
                unlocked = true,
                unlockedDate = "2024-01-20"
            ),
            Achievement(
                id = "3",
                title = "Community Champion",
                description = "Submit 10 daily reports",
                icon = "🏆",
                category = AchievementCategory.COMMUNITY,
                unlocked = true,
                unlockedDate = "2024-01-25",
                progress = 10,
                target = 10
            ),
            Achievement(
                id = "4",
                title = "Knowledge Seeker",
                description = "Complete 5 interactive lessons",
                icon = "📚",
                category = AchievementCategory.LEARNING,
                unlocked = false,
                progress = 3,
                target = 5
            ),
            Achievement(
                id = "5",
                title = "Video Expert",
                description = "Watch 10 video modules",
                icon = "🎬",
                category = AchievementCategory.LEARNING,
                unlocked = false,
                progress = 5,
                target = 10
            ),
            Achievement(
                id = "6",
                title = "Perfect Score",
                description = "Get 3/3 correct on daily questions",
                icon = "⭐",
                category = AchievementCategory.EXPERTISE,
                unlocked = true,
                unlockedDate = "2024-01-18"
            ),
            Achievement(
                id = "7",
                title = "Level 5",
                description = "Reach level 5",
                icon = "🎓",
                category = AchievementCategory.MILESTONES,
                unlocked = false,
                progress = 3,
                target = 5
            ),
            Achievement(
                id = "8",
                title = "Helpful Assistant",
                description = "Chat with Fred 20 times",
                icon = "💬",
                category = AchievementCategory.LEARNING,
                unlocked = false,
                progress = 8,
                target = 20
            )
        )
    }

    /**
     * Get sample weekly reflections
     */
    private fun getSampleReflections(): List<WeeklyReflection> {
        return listOf(
            WeeklyReflection(
                id = "1",
                weekStartDate = "2024-01-15",
                weekEndDate = "2024-01-21",
                successStory = "Successfully vaccinated 15 children and conducted 3 health education sessions on malaria prevention.",
                challengesFaced = "Some families were hesitant about vaccinations. Transportation to remote areas was difficult.",
                lessonsLearned = "Building trust with families takes time. Better planning for transportation is needed.",
                goalsNextWeek = "Reach 20 families for vaccinations and improve record-keeping.",
                overallRating = 4,
                submittedDate = "2024-01-21"
            ),
            WeeklyReflection(
                id = "2",
                weekStartDate = "2024-01-08",
                weekEndDate = "2024-01-14",
                successStory = "Organized a successful health awareness event with 30 community members attending.",
                challengesFaced = "Limited medical supplies. Difficulty reaching families in remote areas.",
                lessonsLearned = "Community events are effective for health education. Need better supply management.",
                goalsNextWeek = "Follow up with event attendees and request additional supplies.",
                overallRating = 5,
                submittedDate = "2024-01-14"
            )
        )
    }
}
