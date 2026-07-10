package com.afyaquest.app.presentation.emergencyguide

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.domain.triage.Disposition
import com.afyaquest.app.domain.triage.localized
import com.afyaquest.app.ui.theme.AfyaQuestTheme
import com.afyaquest.app.util.LanguageManager
import kotlinx.coroutines.delay

/**
 * Conversational assessment flow, styled after the MedPull kiosk guided intake:
 * minimal chrome (a thin progress line instead of an app bar), an assistant badge,
 * a brief typing indicator before each step, one big centered question with its
 * pictograph, and large tappable answer cards. The assistant reads each question
 * aloud (device TTS, EN/ES) unless voice is muted.
 */
@Composable
fun TriageFlowScreen(
    navController: NavController,
    viewModel: EmergencyTriageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val engine = viewModel.engine

    // Hardware back steps to the previous question until we're at the first one.
    BackHandler(enabled = state.path.isNotEmpty()) { viewModel.back() }

    // Keep the screen awake during an assessment; stop audio when leaving.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewModel.stopSpeaking()
        }
    }

    // The assistant "types" briefly, then the step appears and is spoken.
    var typing by remember { mutableStateOf(true) }
    LaunchedEffect(state.currentId) {
        typing = true
        delay(TYPING_MILLIS)
        typing = false
        viewModel.speakCurrentQuestion()
    }

    val phaseLabels = listOf(
        stringResource(R.string.emergency_phase_scene),
        stringResource(R.string.emergency_phase_primary),
        stringResource(R.string.emergency_phase_secondary),
        stringResource(R.string.emergency_phase_disposition)
    )
    val phaseIndex = engine.progressIndex(state)

    AfyaQuestTheme(darkTheme = false) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                val progress by animateFloatAsState(
                    targetValue = phaseIndex.toFloat() / engine.phaseCount(),
                    label = "phaseProgress"
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Thin top row: faint back arrow, phase label, voice + language.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (state.path.isNotEmpty()) viewModel.back()
                            else navController.popBackStack()
                        },
                        modifier = Modifier.alpha(0.5f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                    Text(
                        text = phaseLabels.getOrElse(phaseIndex - 1) { "" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::toggleVoice, modifier = Modifier.alpha(0.7f)) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp
                            else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = stringResource(
                                if (voiceEnabled) R.string.emergency_voice_on
                                else R.string.emergency_voice_off
                            )
                        )
                    }
                    SurfaceLanguageToggle(current = language, onSelect = viewModel::setLanguage)
                }

                AssistantBadge(
                    typing = typing,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                AnimatedContent(
                    targetState = state.currentId to typing,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 10 })
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "step"
                ) { (currentId, isTyping) ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        val disposition = engine.disposition(currentId)
                        val node = engine.node(currentId)
                        when {
                            isTyping -> {
                                Spacer(Modifier.height(120.dp))
                                TypingIndicator(Modifier.align(Alignment.CenterHorizontally))
                            }
                            disposition != null -> DispositionView(
                                disposition = disposition,
                                language = language,
                                onRestart = { viewModel.restart() },
                                onDone = { navController.popBackStack() }
                            )
                            node != null -> {
                                Spacer(Modifier.height(12.dp))
                                TriageStepPicture(
                                    token = node.icon,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(20.dp))
                                QuestionText(
                                    text = node.text.localized(language),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { heading() }
                                )
                                Spacer(Modifier.height(28.dp))
                                node.options?.forEach { option ->
                                    AnswerCard(
                                        label = option.label.localized(language),
                                        onClick = { viewModel.answer(option.letter) }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TYPING_MILLIS = 650L

/** "AfyaQuest Assistant" pill — the conversational presence above each step. */
@Composable
private fun AssistantBadge(typing: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(vertical = 6.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sparkleAlpha = if (typing) {
                rememberInfiniteTransition(label = "sparkle").animateFloat(
                    initialValue = 1f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "sparkleAlpha"
                ).value
            } else 1f
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .alpha(sparkleAlpha)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.emergency_assistant),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Three softly pulsing dots while the assistant "types" the next step. */
@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 450, delayMillis = i * 140),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/** Big centered question, auto-shrinking so long steps never crowd the answers. */
@Composable
private fun QuestionText(text: String, modifier: Modifier = Modifier) {
    val size = when {
        text.length < 60 -> 28.sp
        text.length < 120 -> 24.sp
        else -> 21.sp
    }
    Text(
        text = text,
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        lineHeight = size * 1.32f,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/** Large tappable answer card (kiosk-style: min 76dp, rounded 16dp, auto-advance). */
@Composable
private fun AnswerCard(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** EN | ES toggle recolored for a light surface (the flow has no app bar). */
@Composable
private fun SurfaceLanguageToggle(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val options = listOf(
            LanguageManager.LANGUAGE_ENGLISH to "EN",
            LanguageManager.LANGUAGE_SPANISH to "ES"
        )
        options.forEach { (code, label) ->
            // Any legacy/unknown saved language renders as English (the fallback).
            val active = current == code ||
                (code == LanguageManager.LANGUAGE_ENGLISH &&
                    options.none { it.first == current })
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(code) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DispositionView(
    disposition: Disposition,
    language: String,
    onRestart: () -> Unit,
    onDone: () -> Unit
) {
    val colors = severityColorsFor(disposition.color)

    val headerAlpha = if (disposition.pulse) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulseAlpha"
        ).value
    } else {
        1f
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        dispositionPictureRes(disposition.id)?.let { pic ->
            Image(
                painter = painterResource(pic),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(headerAlpha),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.main)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = dispositionIcon(disposition.level),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = disposition.label.localized(language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.container)
        ) {
            Text(
                text = disposition.instructions.localized(language),
                modifier = Modifier.padding(20.dp),
                fontSize = 17.sp,
                lineHeight = 26.sp,
                color = colors.onContainer
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.emergency_new_assessment), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.emergency_done), fontSize = 16.sp)
        }
        Spacer(Modifier.height(20.dp))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
