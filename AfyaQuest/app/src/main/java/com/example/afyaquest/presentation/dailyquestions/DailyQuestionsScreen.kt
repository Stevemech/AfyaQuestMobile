package com.example.afyaquest.presentation.dailyquestions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.Difficulty
import com.example.afyaquest.domain.model.Question
import com.example.afyaquest.util.Resource

/**
 * Daily Questions screen
 * Displays 3 daily health questions with XP rewards and lives system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionsScreen(
    navController: NavController,
    viewModel: DailyQuestionsViewModel = hiltViewModel()
) {
    val questionsState by viewModel.questionsState.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val showExplanation by viewModel.showExplanation.collectAsState()
    val score by viewModel.score.collectAsState()
    val correctAnswers by viewModel.correctAnswers.collectAsState()
    val lives by viewModel.lives.collectAsState()
    val quizSubmissionState by viewModel.quizSubmissionState.collectAsState()

    val scrollState = rememberScrollState()

    // Handle quiz submission completion
    LaunchedEffect(quizSubmissionState) {
        when (quizSubmissionState) {
            is Resource.Success -> {
                // Quiz submitted successfully, navigate back to dashboard
                navController.popBackStack()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_questions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Score display
                    Text(
                        text = "💯 $score",
                        modifier = Modifier.padding(end = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                    // Lives display
                    Text(
                        text = "❤️ $lives",
                        modifier = Modifier.padding(end = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        when (questionsState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.failed_to_load_questions),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (questionsState as Resource.Error).message ?: stringResource(R.string.unknown_error),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text(stringResource(R.string.go_back))
                        }
                    }
                }
            }
            is Resource.Success -> {
                val questions = (questionsState as Resource.Success).data ?: emptyList()

                if (questions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_questions_available),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text(stringResource(R.string.go_back))
                            }
                        }
                    }
                } else {
                    val currentQuestion = viewModel.getCurrentQuestion()
                    val isLastQuestion = viewModel.isLastQuestion()
                    val totalQuestions = viewModel.getTotalQuestions()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        // Progress indicator
                        QuestionProgress(
                            current = currentQuestionIndex + 1,
                            total = totalQuestions
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Question card
                        if (currentQuestion != null) {
                            QuestionCard(
                                question = currentQuestion,
                                selectedAnswer = selectedAnswer,
                                showExplanation = showExplanation,
                                onAnswerSelect = { answerIndex ->
                                    viewModel.selectAnswer(answerIndex, currentQuestion)
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Action buttons
                            if (showExplanation) {
                                if (!isLastQuestion) {
                                    Button(
                                        onClick = { viewModel.nextQuestion() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.next_question), fontSize = 16.sp)
                                    }
                                } else {
                                    // Show summary before finish button
                                    QuizSummaryCard(
                                        correctAnswers = correctAnswers,
                                        totalQuestions = totalQuestions,
                                        lives = lives
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { viewModel.finishQuiz() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.finish_quiz), fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            null -> {
                // Initial state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun QuestionProgress(
    current: Int,
    total: Int
) {
    Column {
        Text(
            text = stringResource(R.string.question_progress, current, total),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun QuestionCard(
    question: Question,
    selectedAnswer: Int?,
    showExplanation: Boolean,
    onAnswerSelect: (Int) -> Unit
) {
    val difficultyLabel = when (question.difficulty) {
        Difficulty.EASY -> stringResource(R.string.difficulty_easy)
        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
        Difficulty.HARD -> stringResource(R.string.difficulty_hard)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Question metadata badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Difficulty badge
                val difficultyColor = when (question.difficulty) {
                    Difficulty.EASY -> Color(0xFF4CAF50)
                    Difficulty.MEDIUM -> Color(0xFFFF9800)
                    Difficulty.HARD -> Color(0xFFF44336)
                }
                Badge(
                    containerColor = difficultyColor,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = difficultyLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Points badge
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.points_label, question.points),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Category badge
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = question.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Question text
            Text(
                text = question.question,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Options
            question.options.forEachIndexed { index, option ->
                OptionButton(
                    option = option,
                    optionIndex = index,
                    isSelected = index == selectedAnswer,
                    isCorrect = index == question.correctAnswerIndex,
                    showResult = showExplanation,
                    onClick = { onAnswerSelect(index) },
                    enabled = !showExplanation
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Explanation
            if (showExplanation) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.explanation),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionButton(
    option: String,
    optionIndex: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val backgroundColor = when {
        showResult && isCorrect -> Color(0xFF4CAF50)
        showResult && isSelected && !isCorrect -> Color(0xFFF44336)
        isSelected && !showResult -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        showResult && isCorrect -> Color(0xFF4CAF50)
        showResult && isSelected && !isCorrect -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.outline
    }

    val textColor = when {
        showResult && (isCorrect || (isSelected && !isCorrect)) -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = textColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = textColor
        ),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Option label (A, B, C, D)
                Text(
                    text = ('A' + optionIndex).toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .background(
                            textColor.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Option text
                Text(
                    text = option,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Start
                )
            }

            // Result icon
            if (showResult) {
                if (isCorrect) {
                    Text("✓", fontSize = 20.sp, color = Color.White)
                } else if (isSelected) {
                    Text("✗", fontSize = 20.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun QuizSummaryCard(
    correctAnswers: Int,
    totalQuestions: Int,
    lives: Int
) {
    val summaryText = stringResource(R.string.quiz_score_summary, correctAnswers, totalQuestions)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.quiz_complete),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = summaryText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.lives_gained), fontSize = 12.sp)
                    Text("${correctAnswers * 2} ❤️", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.lives_lost), fontSize = 12.sp)
                    Text("${totalQuestions - correctAnswers} 💔", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.current_lives), fontSize = 12.sp)
                    Text("$lives ❤️", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
