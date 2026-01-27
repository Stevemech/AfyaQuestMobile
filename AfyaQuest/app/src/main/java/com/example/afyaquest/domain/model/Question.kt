package com.example.afyaquest.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Question model for daily questions
 */
data class Question(
    val id: String,
    val question: String,
    val options: List<String>,
    @SerializedName("correctAnswer")
    val correctAnswerIndex: Int,
    val explanation: String,
    val category: String,
    val difficulty: Difficulty,
    val points: Int
)

enum class Difficulty {
    @SerializedName("easy")
    EASY,

    @SerializedName("medium")
    MEDIUM,

    @SerializedName("hard")
    HARD
}

/**
 * Daily questions response from API
 */
data class DailyQuestionsResponse(
    val date: String,
    val questions: List<Question>
)

/**
 * Quiz submission request
 */
data class QuizSubmissionRequest(
    val videoId: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val answers: List<QuizAnswer>
)

data class QuizAnswer(
    val questionId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean
)

/**
 * Quiz submission response
 */
data class QuizSubmissionResponse(
    val message: String,
    val score: Int,
    val xpEarned: Int,
    val livesGained: Int,
    val livesLost: Int,
    val newTotalXP: Int,
    val newLevel: Int,
    val newLives: Int
)
