package com.example.afyaquest.presentation.modulequiz

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
import com.example.afyaquest.domain.model.ModuleQuizQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleQuizScreen(
    navController: NavController,
    viewModel: ModuleQuizViewModel = hiltViewModel()
) {
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val selectedAnswer by viewModel.selectedAnswer.collectAsState()
    val showExplanation by viewModel.showExplanation.collectAsState()
    val correctAnswers by viewModel.correctAnswers.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(isFinished) {
        if (isFinished) navController.popBackStack()
    }

    val questions = viewModel.questions
    val totalQuestions = viewModel.getTotalQuestions()

    if (questions.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(translatedModuleTitle(viewModel.moduleId), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { paddingValues ->
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
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(translatedModuleTitle(viewModel.moduleId), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Text(
                        text = "💯 $correctAnswers/$totalQuestions",
                        modifier = Modifier.padding(end = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        val currentQuestion = viewModel.getCurrentQuestion()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Progress bar
            ModuleQuizProgress(
                current = currentQuestionIndex + 1,
                total = totalQuestions
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (currentQuestion != null) {
                val translatedQuestion = translateQuestion(currentQuestion)
                // Question card
                ModuleQuizQuestionCard(
                    question = translatedQuestion,
                    selectedAnswer = selectedAnswer,
                    showExplanation = showExplanation,
                    onAnswerSelect = { viewModel.selectAnswer(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                if (showExplanation) {
                    if (!viewModel.isLastQuestion()) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.next_question), fontSize = 16.sp)
                        }
                    } else {
                        ModuleQuizSummaryCard(
                            correctAnswers = correctAnswers,
                            totalQuestions = totalQuestions
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

@Composable
fun ModuleQuizProgress(
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
fun ModuleQuizQuestionCard(
    question: ModuleQuizQuestion,
    selectedAnswer: Int?,
    showExplanation: Boolean,
    onAnswerSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                ModuleQuizOptionButton(
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
                    Column(modifier = Modifier.padding(16.dp)) {
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
fun ModuleQuizOptionButton(
    option: String,
    optionIndex: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val backgroundColor = when {
        showResult && isCorrect -> Color(0xFF438894)
        showResult && isSelected && !isCorrect -> Color(0xFFF44336)
        isSelected && !showResult -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        showResult && isCorrect -> Color(0xFF438894)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

                Text(
                    text = option,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Start
                )
            }

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
fun ModuleQuizSummaryCard(
    correctAnswers: Int,
    totalQuestions: Int
) {
    val percentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
    val emoji = when {
        percentage >= 80 -> "🎉"
        percentage >= 60 -> "👍"
        else -> "📚"
    }

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
                text = "$emoji ${stringResource(R.string.quiz_complete).removePrefix("\uD83C\uDF89 ")}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.quiz_score_summary, correctAnswers, totalQuestions),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.score_label),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$correctAnswers/$totalQuestions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.percentage_label),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun translatedModuleTitle(moduleId: String): String {
    return when (moduleId) {
        "video-8" -> stringResource(R.string.quiz_title_8)
        "video-9" -> stringResource(R.string.quiz_title_9)
        "video-10" -> stringResource(R.string.quiz_title_10)
        else -> stringResource(R.string.take_quiz)
    }
}

@Composable
fun translateQuestion(question: ModuleQuizQuestion): ModuleQuizQuestion {
    val translatedQ = when (question.id) {
        "8_q1" -> stringResource(R.string.quiz_8_q1)
        "8_q2" -> stringResource(R.string.quiz_8_q2)
        "8_q3" -> stringResource(R.string.quiz_8_q3)
        "8_q4" -> stringResource(R.string.quiz_8_q4)
        "8_q5" -> stringResource(R.string.quiz_8_q5)
        "8_q6" -> stringResource(R.string.quiz_8_q6)
        "8_q7" -> stringResource(R.string.quiz_8_q7)
        "8_q8" -> stringResource(R.string.quiz_8_q8)
        "8_q9" -> stringResource(R.string.quiz_8_q9)
        "8_q10" -> stringResource(R.string.quiz_8_q10)
        "9_q1" -> stringResource(R.string.quiz_9_q1)
        "9_q2" -> stringResource(R.string.quiz_9_q2)
        "9_q3" -> stringResource(R.string.quiz_9_q3)
        "9_q4" -> stringResource(R.string.quiz_9_q4)
        "9_q5" -> stringResource(R.string.quiz_9_q5)
        "9_q6" -> stringResource(R.string.quiz_9_q6)
        "9_q7" -> stringResource(R.string.quiz_9_q7)
        "9_q8" -> stringResource(R.string.quiz_9_q8)
        "9_q9" -> stringResource(R.string.quiz_9_q9)
        "9_q10" -> stringResource(R.string.quiz_9_q10)
        "10_q1" -> stringResource(R.string.quiz_10_q1)
        "10_q2" -> stringResource(R.string.quiz_10_q2)
        "10_q3" -> stringResource(R.string.quiz_10_q3)
        "10_q4" -> stringResource(R.string.quiz_10_q4)
        "10_q5" -> stringResource(R.string.quiz_10_q5)
        "10_q6" -> stringResource(R.string.quiz_10_q6)
        "10_q7" -> stringResource(R.string.quiz_10_q7)
        "10_q8" -> stringResource(R.string.quiz_10_q8)
        "10_q9" -> stringResource(R.string.quiz_10_q9)
        "10_q10" -> stringResource(R.string.quiz_10_q10)
        else -> question.question
    }

    val translatedOptions = when (question.id) {
        "8_q1" -> listOf(stringResource(R.string.quiz_8_q1_a), stringResource(R.string.quiz_8_q1_b), stringResource(R.string.quiz_8_q1_c), stringResource(R.string.quiz_8_q1_d))
        "8_q2" -> listOf(stringResource(R.string.quiz_8_q2_a), stringResource(R.string.quiz_8_q2_b), stringResource(R.string.quiz_8_q2_c), stringResource(R.string.quiz_8_q2_d))
        "8_q3" -> listOf(stringResource(R.string.quiz_8_q3_a), stringResource(R.string.quiz_8_q3_b), stringResource(R.string.quiz_8_q3_c), stringResource(R.string.quiz_8_q3_d))
        "8_q4" -> listOf(stringResource(R.string.quiz_8_q4_a), stringResource(R.string.quiz_8_q4_b), stringResource(R.string.quiz_8_q4_c), stringResource(R.string.quiz_8_q4_d))
        "8_q5" -> listOf(stringResource(R.string.quiz_8_q5_a), stringResource(R.string.quiz_8_q5_b), stringResource(R.string.quiz_8_q5_c), stringResource(R.string.quiz_8_q5_d))
        "8_q6" -> listOf(stringResource(R.string.quiz_8_q6_a), stringResource(R.string.quiz_8_q6_b), stringResource(R.string.quiz_8_q6_c), stringResource(R.string.quiz_8_q6_d))
        "8_q7" -> listOf(stringResource(R.string.quiz_8_q7_a), stringResource(R.string.quiz_8_q7_b), stringResource(R.string.quiz_8_q7_c), stringResource(R.string.quiz_8_q7_d))
        "8_q8" -> listOf(stringResource(R.string.quiz_8_q8_a), stringResource(R.string.quiz_8_q8_b), stringResource(R.string.quiz_8_q8_c), stringResource(R.string.quiz_8_q8_d))
        "8_q9" -> listOf(stringResource(R.string.quiz_8_q9_a), stringResource(R.string.quiz_8_q9_b), stringResource(R.string.quiz_8_q9_c), stringResource(R.string.quiz_8_q9_d))
        "8_q10" -> listOf(stringResource(R.string.quiz_8_q10_a), stringResource(R.string.quiz_8_q10_b), stringResource(R.string.quiz_8_q10_c), stringResource(R.string.quiz_8_q10_d))
        "9_q1" -> listOf(stringResource(R.string.quiz_9_q1_a), stringResource(R.string.quiz_9_q1_b), stringResource(R.string.quiz_9_q1_c), stringResource(R.string.quiz_9_q1_d))
        "9_q2" -> listOf(stringResource(R.string.quiz_9_q2_a), stringResource(R.string.quiz_9_q2_b), stringResource(R.string.quiz_9_q2_c), stringResource(R.string.quiz_9_q2_d))
        "9_q3" -> listOf(stringResource(R.string.quiz_9_q3_a), stringResource(R.string.quiz_9_q3_b), stringResource(R.string.quiz_9_q3_c), stringResource(R.string.quiz_9_q3_d))
        "9_q4" -> listOf(stringResource(R.string.quiz_9_q4_a), stringResource(R.string.quiz_9_q4_b), stringResource(R.string.quiz_9_q4_c), stringResource(R.string.quiz_9_q4_d))
        "9_q5" -> listOf(stringResource(R.string.quiz_9_q5_a), stringResource(R.string.quiz_9_q5_b), stringResource(R.string.quiz_9_q5_c), stringResource(R.string.quiz_9_q5_d))
        "9_q6" -> listOf(stringResource(R.string.quiz_9_q6_a), stringResource(R.string.quiz_9_q6_b), stringResource(R.string.quiz_9_q6_c), stringResource(R.string.quiz_9_q6_d))
        "9_q7" -> listOf(stringResource(R.string.quiz_9_q7_a), stringResource(R.string.quiz_9_q7_b), stringResource(R.string.quiz_9_q7_c), stringResource(R.string.quiz_9_q7_d))
        "9_q8" -> listOf(stringResource(R.string.quiz_9_q8_a), stringResource(R.string.quiz_9_q8_b), stringResource(R.string.quiz_9_q8_c), stringResource(R.string.quiz_9_q8_d))
        "9_q9" -> listOf(stringResource(R.string.quiz_9_q9_a), stringResource(R.string.quiz_9_q9_b), stringResource(R.string.quiz_9_q9_c), stringResource(R.string.quiz_9_q9_d))
        "9_q10" -> listOf(stringResource(R.string.quiz_9_q10_a), stringResource(R.string.quiz_9_q10_b), stringResource(R.string.quiz_9_q10_c), stringResource(R.string.quiz_9_q10_d))
        "10_q1" -> listOf(stringResource(R.string.quiz_10_q1_a), stringResource(R.string.quiz_10_q1_b), stringResource(R.string.quiz_10_q1_c), stringResource(R.string.quiz_10_q1_d))
        "10_q2" -> listOf(stringResource(R.string.quiz_10_q2_a), stringResource(R.string.quiz_10_q2_b), stringResource(R.string.quiz_10_q2_c), stringResource(R.string.quiz_10_q2_d))
        "10_q3" -> listOf(stringResource(R.string.quiz_10_q3_a), stringResource(R.string.quiz_10_q3_b), stringResource(R.string.quiz_10_q3_c), stringResource(R.string.quiz_10_q3_d))
        "10_q4" -> listOf(stringResource(R.string.quiz_10_q4_a), stringResource(R.string.quiz_10_q4_b), stringResource(R.string.quiz_10_q4_c), stringResource(R.string.quiz_10_q4_d))
        "10_q5" -> listOf(stringResource(R.string.quiz_10_q5_a), stringResource(R.string.quiz_10_q5_b), stringResource(R.string.quiz_10_q5_c), stringResource(R.string.quiz_10_q5_d))
        "10_q6" -> listOf(stringResource(R.string.quiz_10_q6_a), stringResource(R.string.quiz_10_q6_b))
        "10_q7" -> listOf(stringResource(R.string.quiz_10_q7_a), stringResource(R.string.quiz_10_q7_b), stringResource(R.string.quiz_10_q7_c), stringResource(R.string.quiz_10_q7_d))
        "10_q8" -> listOf(stringResource(R.string.quiz_10_q8_a), stringResource(R.string.quiz_10_q8_b), stringResource(R.string.quiz_10_q8_c), stringResource(R.string.quiz_10_q8_d))
        "10_q9" -> listOf(stringResource(R.string.quiz_10_q9_a), stringResource(R.string.quiz_10_q9_b), stringResource(R.string.quiz_10_q9_c), stringResource(R.string.quiz_10_q9_d))
        "10_q10" -> listOf(stringResource(R.string.quiz_10_q10_a), stringResource(R.string.quiz_10_q10_b), stringResource(R.string.quiz_10_q10_c), stringResource(R.string.quiz_10_q10_d))
        else -> question.options
    }

    val translatedExp = when (question.id) {
        "8_q1" -> stringResource(R.string.quiz_8_q1_exp)
        "8_q2" -> stringResource(R.string.quiz_8_q2_exp)
        "8_q3" -> stringResource(R.string.quiz_8_q3_exp)
        "8_q4" -> stringResource(R.string.quiz_8_q4_exp)
        "8_q5" -> stringResource(R.string.quiz_8_q5_exp)
        "8_q6" -> stringResource(R.string.quiz_8_q6_exp)
        "8_q7" -> stringResource(R.string.quiz_8_q7_exp)
        "8_q8" -> stringResource(R.string.quiz_8_q8_exp)
        "8_q9" -> stringResource(R.string.quiz_8_q9_exp)
        "8_q10" -> stringResource(R.string.quiz_8_q10_exp)
        "9_q1" -> stringResource(R.string.quiz_9_q1_exp)
        "9_q2" -> stringResource(R.string.quiz_9_q2_exp)
        "9_q3" -> stringResource(R.string.quiz_9_q3_exp)
        "9_q4" -> stringResource(R.string.quiz_9_q4_exp)
        "9_q5" -> stringResource(R.string.quiz_9_q5_exp)
        "9_q6" -> stringResource(R.string.quiz_9_q6_exp)
        "9_q7" -> stringResource(R.string.quiz_9_q7_exp)
        "9_q8" -> stringResource(R.string.quiz_9_q8_exp)
        "9_q9" -> stringResource(R.string.quiz_9_q9_exp)
        "9_q10" -> stringResource(R.string.quiz_9_q10_exp)
        "10_q1" -> stringResource(R.string.quiz_10_q1_exp)
        "10_q2" -> stringResource(R.string.quiz_10_q2_exp)
        "10_q3" -> stringResource(R.string.quiz_10_q3_exp)
        "10_q4" -> stringResource(R.string.quiz_10_q4_exp)
        "10_q5" -> stringResource(R.string.quiz_10_q5_exp)
        "10_q6" -> stringResource(R.string.quiz_10_q6_exp)
        "10_q7" -> stringResource(R.string.quiz_10_q7_exp)
        "10_q8" -> stringResource(R.string.quiz_10_q8_exp)
        "10_q9" -> stringResource(R.string.quiz_10_q9_exp)
        "10_q10" -> stringResource(R.string.quiz_10_q10_exp)
        else -> question.explanation
    }

    return question.copy(
        question = translatedQ,
        options = translatedOptions,
        explanation = translatedExp
    )
}
