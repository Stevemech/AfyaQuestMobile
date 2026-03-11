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
                ModuleQuizQuestionCard(
                    question = currentQuestion,
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
    return when {
        moduleId.startsWith("mod") -> {
            val title = moduleId
                .substringAfter("-")
                .replace("-", " ")
                .replaceFirstChar { it.uppercase() }
            "$title Quiz"
        }
        else -> stringResource(R.string.take_quiz)
    }
}
