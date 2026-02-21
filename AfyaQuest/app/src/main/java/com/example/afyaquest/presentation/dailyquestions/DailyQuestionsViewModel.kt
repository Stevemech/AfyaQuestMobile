package com.example.afyaquest.presentation.dailyquestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.repository.QuestionsRepository
import com.example.afyaquest.domain.model.Difficulty
import com.example.afyaquest.domain.model.Question
import com.example.afyaquest.domain.model.QuizAnswer
import com.example.afyaquest.domain.model.QuizSubmissionRequest
import com.example.afyaquest.domain.model.QuizSubmissionResponse
import com.example.afyaquest.util.Resource
import com.example.afyaquest.util.XpManager
import com.example.afyaquest.util.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Daily Questions screen
 */
@HiltViewModel
class DailyQuestionsViewModel @Inject constructor(
    private val questionsRepository: QuestionsRepository,
    private val xpManager: XpManager
) : ViewModel() {

    private val _questionsState = MutableStateFlow<Resource<List<Question>>?>(null)
    val questionsState: StateFlow<Resource<List<Question>>?> = _questionsState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer.asStateFlow()

    private val _showExplanation = MutableStateFlow(false)
    val showExplanation: StateFlow<Boolean> = _showExplanation.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    private val _answeredQuestions = MutableStateFlow<Set<String>>(emptySet())

    private val _quizSubmissionState = MutableStateFlow<Resource<QuizSubmissionResponse>?>(null)
    val quizSubmissionState: StateFlow<Resource<QuizSubmissionResponse>?> = _quizSubmissionState.asStateFlow()

    // Lives from XpManager
    val lives: StateFlow<Int> = xpManager.getXpDataFlow()
        .map { it.lives }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10
        )

    init {
        loadDailyQuestions()
    }

    /**
     * Load daily questions from API
     */
    private fun loadDailyQuestions() {
        viewModelScope.launch {
            questionsRepository.getDailyQuestions().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _questionsState.value = Resource.Success(resource.data?.questions ?: emptyList())
                    }
                    is Resource.Error -> {
                        _questionsState.value = Resource.Error(resource.message ?: "Failed to load questions")
                    }
                    is Resource.Loading -> {
                        _questionsState.value = Resource.Loading()
                    }
                }
            }
        }
    }

    /**
     * Handle answer selection
     */
    fun selectAnswer(answerIndex: Int, question: Question) {
        if (_answeredQuestions.value.contains(question.id)) return

        _selectedAnswer.value = answerIndex
        _showExplanation.value = true

        val isCorrect = answerIndex == question.correctAnswerIndex

        if (isCorrect) {
            // Update score
            _score.value += question.points
            _correctAnswers.value += 1

            // Award XP based on question difficulty
            val xpReward = when (question.difficulty) {
                Difficulty.EASY -> XpRewards.EASY_QUESTION
                Difficulty.MEDIUM -> XpRewards.MEDIUM_QUESTION
                Difficulty.HARD -> XpRewards.HARD_QUESTION
            }
            viewModelScope.launch {
                xpManager.addXP(
                    xpReward,
                    "Correct answer: ${question.question.take(50)}..."
                )
                // Add 1 life for correct answer (capped at MAX_LIVES)
                xpManager.addLives(1, "Correct answer!")
            }
        } else {
            // Remove 1 life for wrong answer
            viewModelScope.launch {
                xpManager.removeLives(1, "Wrong answer")
            }
        }

        // Mark question as answered
        _answeredQuestions.value = _answeredQuestions.value + question.id
    }

    /**
     * Move to next question
     */
    fun nextQuestion() {
        _currentQuestionIndex.value += 1
        _selectedAnswer.value = null
        _showExplanation.value = false
    }

    /**
     * Check if current question is the last one
     */
    fun isLastQuestion(): Boolean {
        val questions = (_questionsState.value as? Resource.Success)?.data ?: return false
        return _currentQuestionIndex.value == questions.size - 1
    }

    /**
     * Get current question
     */
    fun getCurrentQuestion(): Question? {
        val questions = (_questionsState.value as? Resource.Success)?.data ?: return null
        return questions.getOrNull(_currentQuestionIndex.value)
    }

    /**
     * Finish quiz and submit results
     */
    fun finishQuiz() {
        viewModelScope.launch {
            val questions = (_questionsState.value as? Resource.Success)?.data ?: return@launch
            val totalQuestions = questions.size
            val correctCount = _correctAnswers.value
            val incorrectCount = totalQuestions - correctCount

            // Award bonus XP for completing all questions
            if (_answeredQuestions.value.size == totalQuestions) {
                xpManager.addXP(
                    XpRewards.DAILY_QUESTION_BONUS,
                    "Completed all daily questions!"
                )
            }

            // Build answers list
            val answers = questions.mapIndexed { index, question ->
                val selectedIdx = if (_answeredQuestions.value.contains(question.id)) {
                    _selectedAnswer.value ?: -1
                } else {
                    -1
                }
                QuizAnswer(
                    questionId = question.id,
                    selectedAnswer = selectedIdx,
                    isCorrect = selectedIdx == question.correctAnswerIndex
                )
            }

            // Submit to backend
            val request = QuizSubmissionRequest(
                videoId = "daily-${System.currentTimeMillis()}", // Use timestamp as unique ID
                totalQuestions = totalQuestions,
                correctAnswers = correctCount,
                incorrectAnswers = incorrectCount,
                answers = answers
            )

            questionsRepository.submitQuiz(request).collect { resource ->
                _quizSubmissionState.value = resource
            }
        }
    }

    /**
     * Reset quiz submission state
     */
    fun resetQuizSubmissionState() {
        _quizSubmissionState.value = null
    }

    /**
     * Get total questions count
     */
    fun getTotalQuestions(): Int {
        return (_questionsState.value as? Resource.Success)?.data?.size ?: 0
    }
}
