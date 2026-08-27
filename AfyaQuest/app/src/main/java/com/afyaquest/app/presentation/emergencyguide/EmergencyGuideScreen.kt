package com.afyaquest.app.presentation.emergencyguide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.ui.theme.AfyaQuestTheme
import com.afyaquest.app.ui.theme.SeverityCrit
import com.afyaquest.app.ui.theme.SeverityCritContainer
import com.afyaquest.app.ui.theme.SeverityCritOnContainer
import com.afyaquest.app.ui.theme.SeverityOk
import com.afyaquest.app.ui.theme.SeverityOkContainer
import com.afyaquest.app.ui.theme.SeverityOkOnContainer
import com.afyaquest.app.ui.theme.SeverityWarn
import com.afyaquest.app.ui.theme.SeverityWarnContainer
import com.afyaquest.app.ui.theme.SeverityWarnOnContainer
import com.afyaquest.app.util.LanguageManager

/**
 * Intro / home screen for the Emergency Response Guide: brand mark, a draft
 * disclaimer, an EN/ES toggle, and one unambiguous "Start assessment" button.
 * Forced light theme for sun legibility in the field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyGuideScreen(
    navController: NavController,
    viewModel: EmergencyTriageViewModel = hiltViewModel()
) {
    val language by viewModel.currentLanguage.collectAsState()

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
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.CaseHistory.route) }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = stringResource(R.string.emergency_history)
                            )
                        }
                        EmergencyLanguageToggle(current = language, onSelect = viewModel::setLanguage)
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✚", fontSize = 48.sp, color = Color.White)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.emergency_start_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.emergency_start_subtitle),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.weight(1f))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeverityWarnContainer)
                ) {
                    Text(
                        text = stringResource(R.string.emergency_disclaimer),
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp,
                        color = SeverityWarnOnContainer,
                        lineHeight = 18.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate(Screen.TriageFlow.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.emergency_start_button),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Dashboard entry point. Lives here so the dashboard only needs one call + import. */
@Composable
fun EmergencyEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✚", fontSize = 30.sp, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.emergency_response),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.emergency_response_subtitle),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

/** A simple EN | ES segmented toggle for the top bar. */
@Composable
fun EmergencyLanguageToggle(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.18f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val options = listOf(
            LanguageManager.LANGUAGE_ENGLISH to "EN",
            LanguageManager.LANGUAGE_SPANISH to "ES"
        )
        options.forEach { (code, label) ->
            val active = current == code
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = Color.White.copy(alpha = if (active) 1f else 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(code) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/** Resolved colors for a disposition's severity token. */
data class SeverityColors(val main: Color, val container: Color, val onContainer: Color)

fun severityColorsFor(token: String): SeverityColors = when (token) {
    "crit" -> SeverityColors(SeverityCrit, SeverityCritContainer, SeverityCritOnContainer)
    "warn" -> SeverityColors(SeverityWarn, SeverityWarnContainer, SeverityWarnOnContainer)
    else -> SeverityColors(SeverityOk, SeverityOkContainer, SeverityOkOnContainer)
}
