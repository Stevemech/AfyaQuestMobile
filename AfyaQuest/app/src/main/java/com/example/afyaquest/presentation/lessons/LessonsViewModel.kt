package com.example.afyaquest.presentation.lessons

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.Difficulty
import com.example.afyaquest.domain.model.Lesson
import com.example.afyaquest.domain.model.LessonCategory
import com.example.afyaquest.util.XpManager
import com.example.afyaquest.util.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Interactive Lessons
 */
@HiltViewModel
class LessonsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val xpManager: XpManager
    // TODO: Inject LessonsRepository when backend is ready
) : ViewModel() {

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _selectedCategory = MutableStateFlow<LessonCategory?>(null)
    val selectedCategory: StateFlow<LessonCategory?> = _selectedCategory.asStateFlow()

    private val _completedLessons = MutableStateFlow<Set<String>>(emptySet())
    val completedLessons: StateFlow<Set<String>> = _completedLessons.asStateFlow()

    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

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

    init {
        loadLessons()
    }

    /**
     * Load lessons
     * In production, this would fetch from API
     */
    private fun loadLessons() {
        _lessons.value = listOf(
            Lesson(
                id = "1",
                title = context.getString(R.string.lesson_1_title),
                description = context.getString(R.string.lesson_1_description),
                category = LessonCategory.HYGIENE,
                difficulty = Difficulty.EASY,
                content = context.getString(R.string.lesson_1_content),
                estimatedMinutes = 5,
                points = 50
            ),
            Lesson(
                id = "2",
                title = context.getString(R.string.lesson_2_title),
                description = context.getString(R.string.lesson_2_description),
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_2_content),
                estimatedMinutes = 10,
                points = 75
            ),
            Lesson(
                id = "3",
                title = context.getString(R.string.lesson_3_title),
                description = context.getString(R.string.lesson_3_description),
                category = LessonCategory.MATERNAL_HEALTH,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_3_content),
                estimatedMinutes = 15,
                points = 75
            ),
            Lesson(
                id = "4",
                title = context.getString(R.string.lesson_4_title),
                description = context.getString(R.string.lesson_4_description),
                category = LessonCategory.CHILD_CARE,
                difficulty = Difficulty.EASY,
                content = context.getString(R.string.lesson_4_content),
                estimatedMinutes = 8,
                points = 50
            ),
            Lesson(
                id = "5",
                title = context.getString(R.string.lesson_5_title),
                description = context.getString(R.string.lesson_5_description),
                category = LessonCategory.DISEASE_PREVENTION,
                difficulty = Difficulty.MEDIUM,
                content = context.getString(R.string.lesson_5_content),
                estimatedMinutes = 12,
                points = 75
            ),
            Lesson(
                id = "6",
                title = context.getString(R.string.lesson_6_title),
                description = context.getString(R.string.lesson_6_description),
                category = LessonCategory.FIRST_AID,
                difficulty = Difficulty.HARD,
                content = context.getString(R.string.lesson_6_content),
                estimatedMinutes = 20,
                points = 100
            )
        )
    }

    /**
     * Get filtered lessons
     */
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

    /**
     * Set category filter
     */
    fun setCategory(category: LessonCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Select a lesson to view
     */
    fun selectLesson(lesson: Lesson?) {
        _selectedLesson.value = lesson
    }

    /**
     * Mark lesson as completed and award XP
     */
    fun completeLesson(lessonId: String) {
        viewModelScope.launch {
            _completedLessons.value = _completedLessons.value + lessonId

            val lesson = _lessons.value.find { it.id == lessonId }
            if (lesson != null) {
                xpManager.addXP(
                    XpRewards.MODULE_COMPLETED,
                    "Completed lesson: ${lesson.title}"
                )
            }
        }
    }

    /**
     * Get stats
     */
    fun getCompletedCount(): Int = _completedLessons.value.size
    fun getTotalLessons(): Int = _lessons.value.size

    /**
     * Get category display name
     */
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
