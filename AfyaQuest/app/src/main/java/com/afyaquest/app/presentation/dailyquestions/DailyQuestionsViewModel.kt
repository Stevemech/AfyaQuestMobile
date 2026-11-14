package com.afyaquest.app.presentation.dailyquestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.data.repository.QuestionsRepository
import com.afyaquest.app.domain.model.Difficulty
import com.afyaquest.app.domain.model.Question
import com.afyaquest.app.util.DateUtils
import com.afyaquest.app.util.DailyAnswer
import com.afyaquest.app.util.DailyQuestionsResult
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.Resource
import com.afyaquest.app.util.XpManager
import com.afyaquest.app.util.XpRewards
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
    private val xpManager: XpManager,
    private val progressDataStore: ProgressDataStore
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

    /** How many questions have been answered in this session (drives the leave guard). */
    val answeredCount: StateFlow<Int> = _answeredQuestions
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    /** XP earned in this session: per-correct-answer rewards plus the completion bonus. */
    private val _xpEarned = MutableStateFlow(0)
    val xpEarned: StateFlow<Int> = _xpEarned.asStateFlow()

    /** Net change in lives since the session started (positive = gained). */
    private val _livesDelta = MutableStateFlow(0)
    val livesDelta: StateFlow<Int> = _livesDelta.asStateFlow()

    /** Lives when the session started; captured lazily so the delta is real, not estimated. */
    private var startLives: Int? = null

    /** The day this session belongs to, fixed when the questions are loaded (see [finishQuiz]). */
    private var sessionDate: String = DateUtils.todayIso()

    /** Set the moment Finish is tapped, so a second tap cannot pay the bonus again. */
    private var finishing = false

    // Lives from XpManager
    val lives: StateFlow<Int> = xpManager.getXpDataFlow()
        .map { it.lives }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10
        )

    /** True once today's daily questions have been finished (persisted, survives restarts). */
    val completedToday: StateFlow<Boolean> = progressDataStore.isDailyQuestionsDoneToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** The last finished session, used for the "Completed today" summary. */
    val lastResult: StateFlow<DailyQuestionsResult?> = progressDataStore.getDailyQuestionsResult()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            startLives = xpManager.getLives()
            // Do not replay the quiz if today's session is already done.
            val doneToday = progressDataStore.isDailyQuestionsDoneToday().first()
            if (!doneToday) {
                loadDailyQuestions()
            }
        }
    }

    /**
     * Load daily questions from local question bank (works offline)
     */
    private fun loadDailyQuestions() {
        viewModelScope.launch {
            questionsRepository.getDailyQuestions().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val questions = resource.data?.questions ?: emptyList()
                        sessionDate = DateUtils.todayIso()
                        resumeSession(questions)
                        _questionsState.value = Resource.Success(questions)
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
     * Pick the session up where it was left: questions answered earlier today stay answered (and
     * paid), and the quiz continues with the first open one.
     */
    private suspend fun resumeSession(questions: List<Question>) {
        val answered = progressDataStore.getDailySessionAnswers(sessionDate)
            .filter { answer -> questions.any { it.id == answer.questionId } }
        if (answered.isEmpty()) return

        val byId = answered.associateBy { it.questionId }
        _answeredQuestions.value = byId.keys
        _correctAnswers.value = answered.count { it.correct }
        _xpEarned.value = answered.sumOf { it.xp }
        _score.value = questions.filter { byId[it.id]?.correct == true }.sumOf { it.points }

        val firstOpen = questions.indexOfFirst { it.id !in byId }
        if (firstOpen >= 0) {
            _currentQuestionIndex.value = firstOpen
        } else {
            // Everything was answered but Finish was never tapped: show the last question with
            // its answer, so the Finish button is there.
            val last = questions.last()
            _currentQuestionIndex.value = questions.lastIndex
            _selectedAnswer.value = byId[last.id]?.selectedIndex
            _showExplanation.value = true
        }
    }

    /**
     * Handle answer selection
     */
    fun selectAnswer(answerIndex: Int, question: Question) {
        if (_answeredQuestions.value.contains(question.id)) return
        // Mark question as answered straight away (a second tap must not count twice).
        _answeredQuestions.value = _answeredQuestions.value + question.id

        _selectedAnswer.value = answerIndex
        _showExplanation.value = true

        val isCorrect = answerIndex == question.correctAnswerIndex
        // Award XP based on question difficulty
        val xpReward = if (!isCorrect) 0 else when (question.difficulty) {
            Difficulty.EASY -> XpRewards.EASY_QUESTION
            Difficulty.MEDIUM -> XpRewards.MEDIUM_QUESTION
            Difficulty.HARD -> XpRewards.HARD_QUESTION
        }
        if (isCorrect) {
            _score.value += question.points
            _correctAnswers.value += 1
            _xpEarned.value += xpReward
        }

        viewModelScope.launch {
            // Written before anything is paid out: if the answer is already on record for today
            // (the quiz was left and reopened) it is not paid a second time.
            val firstTime = progressDataStore.recordDailyAnswer(
                DailyAnswer(question.id, answerIndex, isCorrect, xpReward),
                sessionDate
            )
            if (!firstTime) return@launch

            val base = startLives ?: xpManager.getLives().also { startLives = it }
            val newLives = if (isCorrect) {
                xpManager.addXP(xpReward, "Correct answer: ${question.question.take(50)}...")
                // Add 1 life for correct answer (capped at MAX_LIVES)
                xpManager.addLives(1, "Correct answer!")
            } else {
                // Remove 1 life for wrong answer
                xpManager.removeLives(1, "Wrong answer")
            }
            _livesDelta.value = newLives - base
        }
    }

    /**
     * Move to next question. Only once the current one has been answered: a quick double tap on
     * "Next" used to skip a question, or run past the last one onto a blank page.
     */
    fun nextQuestion() {
        if (!_showExplanation.value) return
        val total = getTotalQuestions()
        if (_currentQuestionIndex.value >= total - 1) return
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
     * Finish quiz - awards bonus XP, records today's result and signals the completion dialog.
     * Works entirely offline; no API call required.
     */
    fun finishQuiz() {
        if (_quizFinished.value || finishing) return
        val questions = (_questionsState.value as? Resource.Success)?.data ?: return
        finishing = true
        viewModelScope.launch {

            // Award bonus XP for completing all questions
            if (_answeredQuestions.value.size == questions.size) {
                xpManager.addXP(
                    XpRewards.DAILY_QUESTION_BONUS,
                    "Completed all daily questions!"
                )
                _xpEarned.value += XpRewards.DAILY_QUESTION_BONUS
            }

            // Real lives delta (accounts for the MAX_LIVES cap)
            val base = startLives ?: xpManager.getLives()
            _livesDelta.value = xpManager.getLives() - base

            // Recorded for the day the questions belong to: a session started just before
            // midnight must not use up the next day's questions.
            progressDataStore.markDailyQuestionsCompleted(
                correctAnswers = _correctAnswers.value,
                totalQuestions = questions.size,
                xpEarned = _xpEarned.value,
                date = sessionDate
            )

            // Signal quiz is finished; the UI shows the completion dialog
            _quizFinished.value = true
        }
    }

    /**
     * Get total questions count
     */
    fun getTotalQuestions(): Int {
        return (_questionsState.value as? Resource.Success)?.data?.size ?: 0
    }
}
