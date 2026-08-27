package com.afyaquest.app.presentation.modulequiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.model.ModuleQuizQuestion
import com.afyaquest.app.presentation.components.CompletionDialog
import com.afyaquest.app.presentation.components.ConfirmLeaveDialog
import com.afyaquest.app.presentation.components.EmptyState
import com.afyaquest.app.ui.theme.AfyaError
import com.afyaquest.app.ui.theme.AfyaSuccess

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
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val completionXp by viewModel.completionXp.collectAsState()

    val scrollState = rememberScrollState()
    var showLeaveDialog by remember { mutableStateOf(false) }

    val questions = viewModel.questions
    val totalQuestions = viewModel.getTotalQuestions()
    val screenTitle = stringResource(R.string.quiz_title_format, viewModel.getModuleTitle())

    // Leaving mid-quiz throws away answers, so confirm first. Nothing to lose before the first
    // answer, and once finished the results dialog owns the exit.
    val hasUnsavedProgress = !isFinished && (currentQuestionIndex > 0 || selectedAnswer != null)
    val onBack: () -> Unit = {
        if (hasUnsavedProgress) {
            showLeaveDialog = true
        } else {
            navController.popBackStack()
        }
    }
    BackHandler(enabled = hasUnsavedProgress) { showLeaveDialog = true }

    // Each new question starts at the top of the page.
    LaunchedEffect(currentQuestionIndex) { scrollState.scrollTo(0) }

    if (questions.isEmpty()) {
        Scaffold(
            topBar = { ModuleQuizTopBar(title = screenTitle, onBack = onBack) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = stringResource(R.string.no_questions_available),
                    icon = Icons.Outlined.Quiz,
                    actionText = stringResource(R.string.go_back),
                    onAction = { navController.popBackStack() }
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            ModuleQuizTopBar(
                title = screenTitle,
                onBack = onBack,
                correctAnswers = correctAnswers
            )
        },
        bottomBar = {
            if (showExplanation) {
                ModuleQuizActionBar(
                    isLastQuestion = viewModel.isLastQuestion(),
                    isSubmitting = isSubmitting,
                    enabled = !isSubmitting && !isFinished,
                    onNext = { viewModel.nextQuestion() },
                    onFinish = { viewModel.finishQuiz() }
                )
            }
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

                if (showExplanation && viewModel.isLastQuestion()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ModuleQuizSummaryCard(
                        correctAnswers = correctAnswers,
                        totalQuestions = totalQuestions
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isFinished) {
        val percentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        CompletionDialog(
            title = if (completionXp != null) {
                stringResource(R.string.quiz_complete_title)
            } else {
                stringResource(R.string.quiz_complete_retake_title)
            },
            message = stringResource(R.string.quiz_result_format, correctAnswers, totalQuestions, percentage),
            primaryText = stringResource(R.string.quiz_back_to_module),
            onPrimary = { navController.popBackStack() },
            xpEarned = completionXp,
            secondaryText = stringResource(R.string.quiz_try_again),
            onSecondary = { viewModel.restart() }
        )
    }

    if (showLeaveDialog) {
        ConfirmLeaveDialog(
            title = stringResource(R.string.quiz_leave_title),
            message = stringResource(R.string.quiz_leave_message),
            onStay = { showLeaveDialog = false },
            onLeave = {
                showLeaveDialog = false
                navController.popBackStack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleQuizTopBar(
    title: String,
    onBack: () -> Unit,
    correctAnswers: Int? = null
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            if (correctAnswers != null) {
                Row(
                    modifier = Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.quiz_correct_count_desc),
                        tint = AfyaSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = correctAnswers.toString(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

/** Always-visible Next / Finish button, pinned under the scrolling content. */
@Composable
private fun ModuleQuizActionBar(
    isLastQuestion: Boolean,
    isSubmitting: Boolean,
    enabled: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = if (isLastQuestion) onFinish else onNext,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.quiz_saving_desc), fontSize = 16.sp)
                } else {
                    Text(
                        text = if (isLastQuestion) {
                            stringResource(R.string.finish_quiz)
                        } else {
                            stringResource(R.string.next_question)
                        },
                        fontSize = 16.sp
                    )
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
    val fraction = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column {
        Text(
            text = stringResource(R.string.question_progress, current, total),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
            Text(
                text = question.question,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

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
        showResult && isCorrect -> AfyaSuccess
        showResult && isSelected && !isCorrect -> AfyaError
        isSelected && !showResult -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        showResult && isCorrect -> AfyaSuccess
        showResult && isSelected && !isCorrect -> AfyaError
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
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.correct),
                        tint = Color.White
                    )
                } else if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.incorrect),
                        tint = Color.White
                    )
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
    val icon = when {
        percentage >= 80 -> Icons.Filled.EmojiEvents
        percentage >= 60 -> Icons.Filled.ThumbUp
        else -> Icons.AutoMirrored.Filled.MenuBook
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.quiz_score_summary, correctAnswers, totalQuestions),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryStat(
                    label = stringResource(R.string.quiz_score_label),
                    value = "$correctAnswers/$totalQuestions"
                )
                SummaryStat(
                    label = stringResource(R.string.quiz_percentage_label),
                    value = "$percentage%"
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
