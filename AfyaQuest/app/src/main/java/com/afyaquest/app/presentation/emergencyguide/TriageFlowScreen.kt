package com.afyaquest.app.presentation.emergencyguide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

/**
 * Renders the current step of an assessment: one question (large text + lettered
 * A/B/C/D buttons) or, once a disposition is reached, the result screen with the
 * severity-colored guidance. A phase progress bar sits at the top throughout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageFlowScreen(
    navController: NavController,
    viewModel: EmergencyTriageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val engine = viewModel.engine
    val scrollState = rememberScrollState()

    // Hardware back steps to the previous question until we're at the first one.
    BackHandler(enabled = state.path.isNotEmpty()) { viewModel.back() }

    val phaseLabels = listOf(
        stringResource(R.string.emergency_phase_scene),
        stringResource(R.string.emergency_phase_primary),
        stringResource(R.string.emergency_phase_secondary),
        stringResource(R.string.emergency_phase_disposition)
    )

    AfyaQuestTheme(darkTheme = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.emergency_response), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.path.isNotEmpty()) viewModel.back() else navController.popBackStack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        EmergencyLanguageToggle(current = language, onSelect = viewModel::setLanguage)
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                TriagePhaseBar(
                    currentIndex = engine.progressIndex(state),
                    total = engine.phaseCount(),
                    labels = phaseLabels
                )

                Spacer(Modifier.height(28.dp))

                if (state.isDisposition) {
                    val disposition = engine.currentDisposition(state)
                    if (disposition != null) {
                        DispositionView(
                            disposition = disposition,
                            language = language,
                            onRestart = { viewModel.restart() },
                            onDone = { navController.popBackStack() }
                        )
                    }
                } else {
                    val node = engine.currentNode(state)
                    if (node != null) {
                        Text(
                            text = node.text.localized(language),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 30.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        node.options?.forEach { option ->
                            TriageOptionButton(
                                letter = option.letter,
                                label = option.label.localized(language),
                                onClick = { viewModel.answer(option.letter) }
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
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

    Column(Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(headerAlpha),
            colors = CardDefaults.cardColors(containerColor = colors.main)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = disposition.label.localized(language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
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
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.emergency_new_assessment), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.emergency_done), fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
