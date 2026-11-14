package com.afyaquest.app.presentation.lessons

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.R
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.domain.model.Difficulty
import com.afyaquest.app.domain.model.Lesson
import com.afyaquest.app.domain.model.LessonCategory
import com.afyaquest.app.util.LanguageManager
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import com.afyaquest.app.util.XpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Interactive Lessons.
 * Persists completed lesson state via DataStore and syncs to AWS.
 */
@HiltViewModel
class LessonsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val xpManager: XpManager,
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _selectedCategory = MutableStateFlow<LessonCategory?>(null)
    val selectedCategory: StateFlow<LessonCategory?> = _selectedCategory.asStateFlow()

    private val _completedLessons = MutableStateFlow<Set<String>>(emptySet())
    val completedLessons: StateFlow<Set<String>> = _completedLessons.asStateFlow()

    val categories = listOf(
        LessonCategory.HYGIENE,
        LessonCategory.NUTRITION,
        LessonCategory.MATERNAL_HEALTH,
        LessonCategory.CHILD_CARE,
        LessonCategory.DISEASE_PREVENTION,
        LessonCategory.FIRST_AID,
        LessonCategory.MEDICATION,
        LessonCategory.HEALTH_EDUCATION
    )

    companion object {
        /** Number of built-in lessons (referenced by the Learn hub and Dashboard). */
        const val TOTAL_LESSONS = 10

        /** Ids of the built-in lessons, in list order ("lesson-1" .. "lesson-10"). */
        val LESSON_IDS: List<String> = (1..TOTAL_LESSONS).map { "lesson-$it" }

        /** Title resource of a built-in lesson, or null for an unknown id. */
        @StringRes
        fun lessonTitleRes(lessonId: String?): Int? = when (lessonId) {
            "lesson-1" -> R.string.lesson_1_title
            "lesson-2" -> R.string.lesson_2_title
            "lesson-3" -> R.string.lesson_3_title
            "lesson-4" -> R.string.lesson_4_title
            "lesson-5" -> R.string.lesson_5_title
            "lesson-6" -> R.string.lesson_6_title
            "lesson-7" -> R.string.lesson_7_title
            "lesson-8" -> R.string.lesson_8_title
            "lesson-9" -> R.string.lesson_9_title
            "lesson-10" -> R.string.lesson_10_title
            else -> null
        }
    }

    init {
        loadLessons(languageManager.getCurrentLanguage())
        // Lesson text is resolved here, not in a composable, so it has to be re-read when the
        // user switches language; otherwise the list keeps the language it was first opened in.
        viewModelScope.launch {
            languageManager.getCurrentLanguageFlow().collect { language -> loadLessons(language) }
        }
        loadSavedProgress()
    }

    /** A context whose resources are in [language], independent of when the app locale is applied. */
    private fun localizedContext(language: String): Context {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale(language)) }
        return context.createConfigurationContext(config)
    }

    private fun loadLessons(language: String) {
        val context = localizedContext(language)
        _lessons.value = listOf(
            Lesson(
                id = "lesson-1",
                title = context.getString(R.string.lesson_1_title),
                description = context.getString(R.string.lesson_1_description),
                category = LessonCategory.HYGIENE,
                difficulty = Difficulty.EASY,
                content = context.getString(R.string.lesson_1_content),
                estimatedMinutes = 5,
                points = 50
            ),
            Lesson(
                id = "lesson-2",
                title = context.getString(R.string.lesson_2_title),
                description = context.getString(R.string.lesson_2_description),
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_2_content),
                estimatedMinutes = 10,
                points = 75
            ),
            Lesson(
                id = "lesson-3",
                title = context.getString(R.string.lesson_3_title),
                description = context.getString(R.string.lesson_3_description),
                category = LessonCategory.MATERNAL_HEALTH,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_3_content),
                estimatedMinutes = 15,
                points = 75
            ),
            Lesson(
                id = "lesson-4",
                title = context.getString(R.string.lesson_4_title),
                description = context.getString(R.string.lesson_4_description),
                category = LessonCategory.CHILD_CARE,
                difficulty = Difficulty.EASY,
                content = context.getString(R.string.lesson_4_content),
                estimatedMinutes = 8,
                points = 50
            ),
            Lesson(
                id = "lesson-5",
                title = context.getString(R.string.lesson_5_title),
                description = context.getString(R.string.lesson_5_description),
                category = LessonCategory.DISEASE_PREVENTION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_5_content),
                estimatedMinutes = 12,
                points = 75
            ),
            Lesson(
                id = "lesson-6",
                title = context.getString(R.string.lesson_6_title),
                description = context.getString(R.string.lesson_6_description),
                category = LessonCategory.FIRST_AID,
                difficulty = Difficulty.HARD,
                content = context.getString(R.string.lesson_6_content),
                estimatedMinutes = 20,
                points = 100
            ),
            // Lessons 7-10: nutrition, written from the education team's lecture decks
            Lesson(
                id = "lesson-7",
                title = context.getString(R.string.lesson_7_title),
                description = context.getString(R.string.lesson_7_description),
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_7_content),
                estimatedMinutes = 10,
                points = 75
            ),
            Lesson(
                id = "lesson-8",
                title = context.getString(R.string.lesson_8_title),
                description = context.getString(R.string.lesson_8_description),
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.EASY,
                content = context.getString(R.string.lesson_8_content),
                estimatedMinutes = 8,
                points = 50
            ),
            Lesson(
                id = "lesson-9",
                title = context.getString(R.string.lesson_9_title),
                description = context.getString(R.string.lesson_9_description),
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_9_content),
                estimatedMinutes = 8,
                points = 75
            ),
            Lesson(
                id = "lesson-10",
                title = context.getString(R.string.lesson_10_title),
                description = context.getString(R.string.lesson_10_description),
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_10_content),
                estimatedMinutes = 10,
                points = 75
            )
        )
    }

    /**
     * Load saved progress from DataStore so it persists across navigation
     */
    private fun loadSavedProgress() {
        viewModelScope.launch {
            progressDataStore.getCompletedLessons().collect { completed ->
                _completedLessons.value = completed
            }
        }
    }

    fun getFilteredLessons(): List<Lesson> {
        val category = _selectedCategory.value
        return if (category == null) {
            _lessons.value
        } else {
            _lessons.value.filter { it.category == category }
        }.map { lesson ->
            lesson.copy(completed = _completedLessons.value.contains(lesson.id))
        }
    }

    fun setCategory(category: LessonCategory?) {
        _selectedCategory.value = category
    }

    /** A lesson by id, with its persisted completion state applied. */
    fun lessonById(id: String): Lesson? =
        _lessons.value.find { it.id == id }?.let { lesson ->
            lesson.copy(completed = _completedLessons.value.contains(lesson.id))
        }

    /**
     * The next lesson the user has not completed yet.
     * With [afterId] the search starts after that lesson and wraps around, so the "Next lesson"
     * action after completing lesson 3 suggests lesson 4 first, then any earlier one still open.
     * With [within] (the lesson ids of a focus topic, in the topic's own order) only those lessons
     * are considered, so "Next lesson" stays inside the topic the lesson was opened from.
     */
    fun nextIncompleteLesson(afterId: String? = null, within: List<String>? = null): Lesson? {
        val all = if (within == null) {
            _lessons.value
        } else {
            within.mapNotNull { id -> _lessons.value.find { it.id == id } }
        }
        if (all.isEmpty()) return null
        val startIndex = all.indexOfFirst { it.id == afterId }
        val ordered = if (startIndex < 0) all else all.drop(startIndex + 1) + all.take(startIndex)
        return ordered
            .firstOrNull { !_completedLessons.value.contains(it.id) }
            ?.copy(completed = false)
    }

    /**
     * Mark lesson as completed — persists locally, awards XP, and syncs to AWS
     */
    fun completeLesson(lessonId: String) {
        if (_completedLessons.value.contains(lessonId)) return
        viewModelScope.launch {
            _completedLessons.value = _completedLessons.value + lessonId
            progressDataStore.markLessonCompleted(lessonId)

            val lesson = _lessons.value.find { it.id == lessonId }
            if (lesson != null) {
                // Award exactly what the "Mark as complete (+N XP)" button promises
                xpManager.addXP(
                    lesson.points,
                    "Completed lesson: ${lesson.title}"
                )
            }

            // Sync to AWS
            syncLessonProgress(lessonId)
        }
    }

    /**
     * Sync lesson completion to the backend
     */
    private suspend fun syncLessonProgress(lessonId: String) {
        try {
            val token = tokenManager.getIdToken() ?: return
            val body = mapOf<String, Any>(
                "lessonId" to lessonId,
                "completed" to true,
                // XP is awarded on the device and uploaded with the account state.
                "skipXp" to true
            )
            apiService.updateLessonProgress("Bearer $token", body)

            // Also update assignment status
            val progressBody = mapOf<String, Any>(
                "type" to "lesson_complete",
                "itemId" to lessonId
            )
            apiService.updateUserProgress("Bearer $token", progressBody)
        } catch (e: Exception) {
            Log.d("LessonsVM", "Progress sync failed (will retry): ${e.message}")
        }
    }

    fun getCompletedCount(): Int = _completedLessons.value.size
    fun getTotalLessons(): Int = _lessons.value.size

    fun getCategoryDisplayName(category: LessonCategory): String {
        return when (category) {
            LessonCategory.HYGIENE -> context.getString(R.string.category_hygiene)
            LessonCategory.NUTRITION -> context.getString(R.string.category_nutrition)
            LessonCategory.MATERNAL_HEALTH -> context.getString(R.string.category_maternal_health)
            LessonCategory.CHILD_CARE -> context.getString(R.string.category_child_care)
            LessonCategory.DISEASE_PREVENTION -> context.getString(R.string.category_disease_prevention)
            LessonCategory.FIRST_AID -> context.getString(R.string.category_first_aid)
            LessonCategory.MEDICATION -> context.getString(R.string.category_medication)
            LessonCategory.HEALTH_EDUCATION -> context.getString(R.string.category_health_education)
        }
    }
}
