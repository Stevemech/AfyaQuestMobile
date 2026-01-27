package com.example.afyaquest.presentation.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.domain.model.Difficulty
import com.example.afyaquest.domain.model.Lesson
import com.example.afyaquest.domain.model.LessonCategory
import com.example.afyaquest.util.XpManager
import com.example.afyaquest.util.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Interactive Lessons
 */
@HiltViewModel
class LessonsViewModel @Inject constructor(
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
                title = "Proper Handwashing Techniques",
                description = "Learn the 7 steps of proper handwashing to prevent disease transmission",
                category = LessonCategory.HYGIENE,
                difficulty = Difficulty.EASY,
                content = createHandwashingContent(),
                estimatedMinutes = 5,
                points = 50
            ),
            Lesson(
                id = "2",
                title = "Balanced Diet for Children",
                description = "Understanding nutritional needs for growing children",
                category = LessonCategory.NUTRITION,
                difficulty = Difficulty.MEDIUM,
                content = createNutritionContent(),
                estimatedMinutes = 10,
                points = 75
            ),
            Lesson(
                id = "3",
                title = "Prenatal Care Essentials",
                description = "Key aspects of maternal health during pregnancy",
                category = LessonCategory.MATERNAL_HEALTH,
                difficulty = Difficulty.MEDIUM,
                content = createPrenatalCareContent(),
                estimatedMinutes = 15,
                points = 75
            ),
            Lesson(
                id = "4",
                title = "Child Vaccination Schedule",
                description = "Understanding when and why children need vaccines",
                category = LessonCategory.CHILD_CARE,
                difficulty = Difficulty.EASY,
                content = createVaccinationContent(),
                estimatedMinutes = 8,
                points = 50
            ),
            Lesson(
                id = "5",
                title = "Malaria Prevention",
                description = "Strategies to prevent malaria in your community",
                category = LessonCategory.DISEASE_PREVENTION,
                difficulty = Difficulty.MEDIUM,
                content = createMalariaContent(),
                estimatedMinutes = 12,
                points = 75
            ),
            Lesson(
                id = "6",
                title = "CPR Basics",
                description = "Life-saving CPR techniques for adults and children",
                category = LessonCategory.FIRST_AID,
                difficulty = Difficulty.HARD,
                content = createCPRContent(),
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
            LessonCategory.HYGIENE -> "Hygiene"
            LessonCategory.NUTRITION -> "Nutrition"
            LessonCategory.MATERNAL_HEALTH -> "Maternal Health"
            LessonCategory.CHILD_CARE -> "Child Care"
            LessonCategory.DISEASE_PREVENTION -> "Disease Prevention"
            LessonCategory.FIRST_AID -> "First Aid"
            LessonCategory.MEDICATION -> "Medication"
            LessonCategory.HEALTH_EDUCATION -> "Health Education"
        }
    }

    // Sample lesson content
    private fun createHandwashingContent() = """
# Proper Handwashing Techniques

## Why Handwashing Matters
Handwashing is one of the most effective ways to prevent the spread of diseases and infections.

## The 7 Steps

1. **Wet hands** with clean, running water
2. **Apply soap** and lather well
3. **Rub palms together**
4. **Rub between fingers**
5. **Clean backs of hands**
6. **Clean under nails**
7. **Rinse thoroughly**

## When to Wash Hands
- Before preparing food
- Before eating
- After using the toilet
- After coughing or sneezing
- After touching animals

## Duration
Wash hands for at least **20 seconds** - about as long as singing "Happy Birthday" twice!
    """.trimIndent()

    private fun createNutritionContent() = """
# Balanced Diet for Children

## Essential Food Groups

### 1. Proteins
- Beans, lentils, eggs, fish, meat
- Builds strong muscles and supports growth

### 2. Carbohydrates
- Ugali, rice, potatoes, bread
- Provides energy for daily activities

### 3. Fruits & Vegetables
- Dark leafy greens, mangoes, bananas, tomatoes
- Rich in vitamins and minerals

### 4. Dairy
- Milk, yogurt (if available)
- Strong bones and teeth

## Meal Planning Tips
- Offer variety each day
- Include colorful fruits and vegetables
- Ensure adequate portions
- Provide clean drinking water
    """.trimIndent()

    private fun createPrenatalCareContent() = """
# Prenatal Care Essentials

## Regular Check-ups
Visit health facility at least 4 times during pregnancy:
- Once in first 3 months
- Once between 4-6 months
- Twice in last 3 months

## Nutrition During Pregnancy
- Increase food intake
- Take iron and folic acid supplements
- Drink plenty of clean water

## Warning Signs
Seek immediate help if:
- Severe headache
- Blurred vision
- Vaginal bleeding
- Severe abdominal pain
- Reduced baby movement

## Malaria Prevention
- Sleep under treated mosquito net
- Take preventive medication as prescribed
    """.trimIndent()

    private fun createVaccinationContent() = """
# Child Vaccination Schedule

## At Birth
- BCG (tuberculosis)
- Polio 0

## 6 Weeks
- Polio 1
- Pentavalent 1
- PCV 1

## 10 Weeks
- Polio 2
- Pentavalent 2
- PCV 2

## 14 Weeks
- Polio 3
- Pentavalent 3
- PCV 3

## 9 Months
- Measles
- Yellow Fever

## Why Vaccines Matter
Vaccines protect children from serious diseases and save lives.
    """.trimIndent()

    private fun createMalariaContent() = """
# Malaria Prevention

## Use of Mosquito Nets
- Sleep under insecticide-treated nets
- Repair holes immediately
- Re-treat nets every 6-12 months

## Environmental Control
- Remove standing water
- Keep surroundings clean
- Clear bush around homes

## Early Detection
Look for symptoms:
- Fever
- Chills
- Headache
- Body aches

## Treatment
- Seek medical help immediately
- Complete full course of medication
- Rest and drink fluids
    """.trimIndent()

    private fun createCPRContent() = """
# CPR Basics

## When to Perform CPR
- Person is unconscious
- Not breathing or gasping
- No pulse

## Steps for Adults

### 1. Call for Help
Call emergency services immediately

### 2. Position
Lay person flat on firm surface

### 3. Hand Placement
Center of chest, between nipples

### 4. Compressions
- Push hard and fast
- At least 2 inches deep
- Rate: 100-120 per minute
- 30 compressions

### 5. Rescue Breaths
- Tilt head back
- Lift chin
- Give 2 breaths

### 6. Repeat
Continue cycle: 30 compressions, 2 breaths

## For Children
- Use one hand for compressions
- Less depth: about 2 inches
- Same ratio: 30:2
    """.trimIndent()
}
