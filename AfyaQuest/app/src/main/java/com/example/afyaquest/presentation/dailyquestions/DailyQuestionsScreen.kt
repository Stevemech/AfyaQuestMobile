package com.example.afyaquest.presentation.dailyquestions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    val quizFinished by viewModel.quizFinished.collectAsState()

    val scrollState = rememberScrollState()

    // Handle quiz completion — navigate back to dashboard
    LaunchedEffect(quizFinished) {
        if (quizFinished) {
            navController.popBackStack()
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
                            val translatedQuestion = translateDailyQuestion(currentQuestion)
                            QuestionCard(
                                question = translatedQuestion,
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
                    Difficulty.EASY -> Color(0xFF438894)
                    Difficulty.MEDIUM -> Color(0xFFEFA03F)
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
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

@Composable
fun translateDailyQuestion(question: Question): Question {
    val qRes = dailyQuestionResources[question.id] ?: return question

    val translatedQ = stringResource(qRes.question)
    val translatedOptions = listOf(
        stringResource(qRes.optionA),
        stringResource(qRes.optionB),
        stringResource(qRes.optionC),
        stringResource(qRes.optionD)
    )
    val translatedExp = stringResource(qRes.explanation)
    val translatedCat = categoryResources[question.category]?.let { stringResource(it) } ?: question.category

    return question.copy(
        question = translatedQ,
        options = translatedOptions,
        explanation = translatedExp,
        category = translatedCat
    )
}

private data class DQRes(
    val question: Int,
    val optionA: Int,
    val optionB: Int,
    val optionC: Int,
    val optionD: Int,
    val explanation: Int
)

private val categoryResources = mapOf(
    "Hygiene" to R.string.cat_hygiene,
    "Nutrition" to R.string.cat_nutrition,
    "Maternal Health" to R.string.cat_maternal_health,
    "Immunization" to R.string.cat_immunization,
    "Disease Prevention" to R.string.cat_disease_prevention,
    "First Aid" to R.string.cat_first_aid,
    "Child Care" to R.string.cat_child_care,
    "Community Health" to R.string.cat_community_health,
    "Reproductive Health" to R.string.cat_reproductive_health,
    "Emergency" to R.string.cat_emergency
)

private val dailyQuestionResources = mapOf(
    "local_hyg_1" to DQRes(R.string.dq_local_hyg_1, R.string.dq_local_hyg_1_a, R.string.dq_local_hyg_1_b, R.string.dq_local_hyg_1_c, R.string.dq_local_hyg_1_d, R.string.dq_local_hyg_1_exp),
    "local_hyg_2" to DQRes(R.string.dq_local_hyg_2, R.string.dq_local_hyg_2_a, R.string.dq_local_hyg_2_b, R.string.dq_local_hyg_2_c, R.string.dq_local_hyg_2_d, R.string.dq_local_hyg_2_exp),
    "local_hyg_3" to DQRes(R.string.dq_local_hyg_3, R.string.dq_local_hyg_3_a, R.string.dq_local_hyg_3_b, R.string.dq_local_hyg_3_c, R.string.dq_local_hyg_3_d, R.string.dq_local_hyg_3_exp),
    "local_hyg_4" to DQRes(R.string.dq_local_hyg_4, R.string.dq_local_hyg_4_a, R.string.dq_local_hyg_4_b, R.string.dq_local_hyg_4_c, R.string.dq_local_hyg_4_d, R.string.dq_local_hyg_4_exp),
    "local_hyg_5" to DQRes(R.string.dq_local_hyg_5, R.string.dq_local_hyg_5_a, R.string.dq_local_hyg_5_b, R.string.dq_local_hyg_5_c, R.string.dq_local_hyg_5_d, R.string.dq_local_hyg_5_exp),
    "local_nut_1" to DQRes(R.string.dq_local_nut_1, R.string.dq_local_nut_1_a, R.string.dq_local_nut_1_b, R.string.dq_local_nut_1_c, R.string.dq_local_nut_1_d, R.string.dq_local_nut_1_exp),
    "local_nut_2" to DQRes(R.string.dq_local_nut_2, R.string.dq_local_nut_2_a, R.string.dq_local_nut_2_b, R.string.dq_local_nut_2_c, R.string.dq_local_nut_2_d, R.string.dq_local_nut_2_exp),
    "local_nut_3" to DQRes(R.string.dq_local_nut_3, R.string.dq_local_nut_3_a, R.string.dq_local_nut_3_b, R.string.dq_local_nut_3_c, R.string.dq_local_nut_3_d, R.string.dq_local_nut_3_exp),
    "local_nut_4" to DQRes(R.string.dq_local_nut_4, R.string.dq_local_nut_4_a, R.string.dq_local_nut_4_b, R.string.dq_local_nut_4_c, R.string.dq_local_nut_4_d, R.string.dq_local_nut_4_exp),
    "local_nut_5" to DQRes(R.string.dq_local_nut_5, R.string.dq_local_nut_5_a, R.string.dq_local_nut_5_b, R.string.dq_local_nut_5_c, R.string.dq_local_nut_5_d, R.string.dq_local_nut_5_exp),
    "local_mat_1" to DQRes(R.string.dq_local_mat_1, R.string.dq_local_mat_1_a, R.string.dq_local_mat_1_b, R.string.dq_local_mat_1_c, R.string.dq_local_mat_1_d, R.string.dq_local_mat_1_exp),
    "local_mat_2" to DQRes(R.string.dq_local_mat_2, R.string.dq_local_mat_2_a, R.string.dq_local_mat_2_b, R.string.dq_local_mat_2_c, R.string.dq_local_mat_2_d, R.string.dq_local_mat_2_exp),
    "local_mat_3" to DQRes(R.string.dq_local_mat_3, R.string.dq_local_mat_3_a, R.string.dq_local_mat_3_b, R.string.dq_local_mat_3_c, R.string.dq_local_mat_3_d, R.string.dq_local_mat_3_exp),
    "local_mat_4" to DQRes(R.string.dq_local_mat_4, R.string.dq_local_mat_4_a, R.string.dq_local_mat_4_b, R.string.dq_local_mat_4_c, R.string.dq_local_mat_4_d, R.string.dq_local_mat_4_exp),
    "local_mat_5" to DQRes(R.string.dq_local_mat_5, R.string.dq_local_mat_5_a, R.string.dq_local_mat_5_b, R.string.dq_local_mat_5_c, R.string.dq_local_mat_5_d, R.string.dq_local_mat_5_exp),
    "local_imm_1" to DQRes(R.string.dq_local_imm_1, R.string.dq_local_imm_1_a, R.string.dq_local_imm_1_b, R.string.dq_local_imm_1_c, R.string.dq_local_imm_1_d, R.string.dq_local_imm_1_exp),
    "local_imm_2" to DQRes(R.string.dq_local_imm_2, R.string.dq_local_imm_2_a, R.string.dq_local_imm_2_b, R.string.dq_local_imm_2_c, R.string.dq_local_imm_2_d, R.string.dq_local_imm_2_exp),
    "local_imm_3" to DQRes(R.string.dq_local_imm_3, R.string.dq_local_imm_3_a, R.string.dq_local_imm_3_b, R.string.dq_local_imm_3_c, R.string.dq_local_imm_3_d, R.string.dq_local_imm_3_exp),
    "local_imm_4" to DQRes(R.string.dq_local_imm_4, R.string.dq_local_imm_4_a, R.string.dq_local_imm_4_b, R.string.dq_local_imm_4_c, R.string.dq_local_imm_4_d, R.string.dq_local_imm_4_exp),
    "local_imm_5" to DQRes(R.string.dq_local_imm_5, R.string.dq_local_imm_5_a, R.string.dq_local_imm_5_b, R.string.dq_local_imm_5_c, R.string.dq_local_imm_5_d, R.string.dq_local_imm_5_exp),
    "local_dis_1" to DQRes(R.string.dq_local_dis_1, R.string.dq_local_dis_1_a, R.string.dq_local_dis_1_b, R.string.dq_local_dis_1_c, R.string.dq_local_dis_1_d, R.string.dq_local_dis_1_exp),
    "local_dis_2" to DQRes(R.string.dq_local_dis_2, R.string.dq_local_dis_2_a, R.string.dq_local_dis_2_b, R.string.dq_local_dis_2_c, R.string.dq_local_dis_2_d, R.string.dq_local_dis_2_exp),
    "local_dis_3" to DQRes(R.string.dq_local_dis_3, R.string.dq_local_dis_3_a, R.string.dq_local_dis_3_b, R.string.dq_local_dis_3_c, R.string.dq_local_dis_3_d, R.string.dq_local_dis_3_exp),
    "local_dis_4" to DQRes(R.string.dq_local_dis_4, R.string.dq_local_dis_4_a, R.string.dq_local_dis_4_b, R.string.dq_local_dis_4_c, R.string.dq_local_dis_4_d, R.string.dq_local_dis_4_exp),
    "local_dis_5" to DQRes(R.string.dq_local_dis_5, R.string.dq_local_dis_5_a, R.string.dq_local_dis_5_b, R.string.dq_local_dis_5_c, R.string.dq_local_dis_5_d, R.string.dq_local_dis_5_exp),
    "local_fa_1" to DQRes(R.string.dq_local_fa_1, R.string.dq_local_fa_1_a, R.string.dq_local_fa_1_b, R.string.dq_local_fa_1_c, R.string.dq_local_fa_1_d, R.string.dq_local_fa_1_exp),
    "local_fa_2" to DQRes(R.string.dq_local_fa_2, R.string.dq_local_fa_2_a, R.string.dq_local_fa_2_b, R.string.dq_local_fa_2_c, R.string.dq_local_fa_2_d, R.string.dq_local_fa_2_exp),
    "local_fa_3" to DQRes(R.string.dq_local_fa_3, R.string.dq_local_fa_3_a, R.string.dq_local_fa_3_b, R.string.dq_local_fa_3_c, R.string.dq_local_fa_3_d, R.string.dq_local_fa_3_exp),
    "local_fa_4" to DQRes(R.string.dq_local_fa_4, R.string.dq_local_fa_4_a, R.string.dq_local_fa_4_b, R.string.dq_local_fa_4_c, R.string.dq_local_fa_4_d, R.string.dq_local_fa_4_exp),
    "local_fa_5" to DQRes(R.string.dq_local_fa_5, R.string.dq_local_fa_5_a, R.string.dq_local_fa_5_b, R.string.dq_local_fa_5_c, R.string.dq_local_fa_5_d, R.string.dq_local_fa_5_exp),
    "local_cc_1" to DQRes(R.string.dq_local_cc_1, R.string.dq_local_cc_1_a, R.string.dq_local_cc_1_b, R.string.dq_local_cc_1_c, R.string.dq_local_cc_1_d, R.string.dq_local_cc_1_exp),
    "local_cc_2" to DQRes(R.string.dq_local_cc_2, R.string.dq_local_cc_2_a, R.string.dq_local_cc_2_b, R.string.dq_local_cc_2_c, R.string.dq_local_cc_2_d, R.string.dq_local_cc_2_exp),
    "local_cc_3" to DQRes(R.string.dq_local_cc_3, R.string.dq_local_cc_3_a, R.string.dq_local_cc_3_b, R.string.dq_local_cc_3_c, R.string.dq_local_cc_3_d, R.string.dq_local_cc_3_exp),
    "local_cc_4" to DQRes(R.string.dq_local_cc_4, R.string.dq_local_cc_4_a, R.string.dq_local_cc_4_b, R.string.dq_local_cc_4_c, R.string.dq_local_cc_4_d, R.string.dq_local_cc_4_exp),
    "local_cc_5" to DQRes(R.string.dq_local_cc_5, R.string.dq_local_cc_5_a, R.string.dq_local_cc_5_b, R.string.dq_local_cc_5_c, R.string.dq_local_cc_5_d, R.string.dq_local_cc_5_exp),
    "local_ch_1" to DQRes(R.string.dq_local_ch_1, R.string.dq_local_ch_1_a, R.string.dq_local_ch_1_b, R.string.dq_local_ch_1_c, R.string.dq_local_ch_1_d, R.string.dq_local_ch_1_exp),
    "local_ch_2" to DQRes(R.string.dq_local_ch_2, R.string.dq_local_ch_2_a, R.string.dq_local_ch_2_b, R.string.dq_local_ch_2_c, R.string.dq_local_ch_2_d, R.string.dq_local_ch_2_exp),
    "local_ch_3" to DQRes(R.string.dq_local_ch_3, R.string.dq_local_ch_3_a, R.string.dq_local_ch_3_b, R.string.dq_local_ch_3_c, R.string.dq_local_ch_3_d, R.string.dq_local_ch_3_exp),
    "local_ch_4" to DQRes(R.string.dq_local_ch_4, R.string.dq_local_ch_4_a, R.string.dq_local_ch_4_b, R.string.dq_local_ch_4_c, R.string.dq_local_ch_4_d, R.string.dq_local_ch_4_exp),
    "local_ch_5" to DQRes(R.string.dq_local_ch_5, R.string.dq_local_ch_5_a, R.string.dq_local_ch_5_b, R.string.dq_local_ch_5_c, R.string.dq_local_ch_5_d, R.string.dq_local_ch_5_exp),
    "local_rh_1" to DQRes(R.string.dq_local_rh_1, R.string.dq_local_rh_1_a, R.string.dq_local_rh_1_b, R.string.dq_local_rh_1_c, R.string.dq_local_rh_1_d, R.string.dq_local_rh_1_exp),
    "local_rh_2" to DQRes(R.string.dq_local_rh_2, R.string.dq_local_rh_2_a, R.string.dq_local_rh_2_b, R.string.dq_local_rh_2_c, R.string.dq_local_rh_2_d, R.string.dq_local_rh_2_exp),
    "local_rh_3" to DQRes(R.string.dq_local_rh_3, R.string.dq_local_rh_3_a, R.string.dq_local_rh_3_b, R.string.dq_local_rh_3_c, R.string.dq_local_rh_3_d, R.string.dq_local_rh_3_exp),
    "local_em_1" to DQRes(R.string.dq_local_em_1, R.string.dq_local_em_1_a, R.string.dq_local_em_1_b, R.string.dq_local_em_1_c, R.string.dq_local_em_1_d, R.string.dq_local_em_1_exp),
    "local_em_2" to DQRes(R.string.dq_local_em_2, R.string.dq_local_em_2_a, R.string.dq_local_em_2_b, R.string.dq_local_em_2_c, R.string.dq_local_em_2_d, R.string.dq_local_em_2_exp),
    "local_em_3" to DQRes(R.string.dq_local_em_3, R.string.dq_local_em_3_a, R.string.dq_local_em_3_b, R.string.dq_local_em_3_c, R.string.dq_local_em_3_d, R.string.dq_local_em_3_exp),
)
