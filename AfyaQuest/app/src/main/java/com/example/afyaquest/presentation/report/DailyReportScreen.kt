package com.example.afyaquest.presentation.report

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.domain.model.DailyReport
import com.example.afyaquest.presentation.components.HandwritingDialog
import com.example.afyaquest.presentation.components.InputAssistRow
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.Resource
import java.text.SimpleDateFormat
import java.util.*

/**
 * Daily Report screen
 * Form for Community Health Assistants to submit daily activities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(
    navController: NavController,
    viewModel: DailyReportViewModel = hiltViewModel()
) {
    val patientsVisited by viewModel.patientsVisited.collectAsState()
    val vaccinationsGiven by viewModel.vaccinationsGiven.collectAsState()
    val healthEducation by viewModel.healthEducation.collectAsState()
    val challenges by viewModel.challenges.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val submissionState by viewModel.submissionState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val reportHistory by viewModel.reportHistory.collectAsState()
    val historyLoading by viewModel.historyLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val reportSubmittedMsg = stringResource(R.string.report_submitted)
    val submissionFailedMsg = stringResource(R.string.submission_failed)

    // Handle submission state
    LaunchedEffect(submissionState) {
        when (submissionState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    (submissionState as Resource.Success).data ?: reportSubmittedMsg,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetSubmissionState()
                viewModel.selectTab(1) // Switch to History tab
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    (submissionState as Resource.Error).message ?: submissionFailedMsg,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetSubmissionState()
            }
            else -> {}
        }
    }

    val tabs = listOf(stringResource(R.string.new_report), stringResource(R.string.report_history))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_report), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date()),
                        modifier = Modifier.padding(end = 16.dp),
                        fontSize = 14.sp
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ReportFormContent(
                    patientsVisited = patientsVisited,
                    vaccinationsGiven = vaccinationsGiven,
                    healthEducation = healthEducation,
                    challenges = challenges,
                    notes = notes,
                    submissionState = submissionState,
                    healthEducationTopics = viewModel.healthEducationTopics,
                    isFormValid = viewModel.isFormValid(),
                    currentLanguage = viewModel.getCurrentLanguage(),
                    onPatientsVisitedChange = viewModel::setPatientsVisited,
                    onVaccinationsGivenChange = viewModel::setVaccinationsGiven,
                    onHealthEducationChange = viewModel::setHealthEducation,
                    onChallengesChange = viewModel::setChallenges,
                    onNotesChange = viewModel::setNotes,
                    onSubmit = viewModel::submitReport
                )
                1 -> ReportHistoryContent(
                    reports = reportHistory,
                    isLoading = historyLoading
                )
            }
        }
    }
}

/**
 * Returns the BCP-47 speech recognition locale tag for the current app language.
 */
private fun speechLocale(language: String): String = when (language) {
    LanguageManager.LANGUAGE_SWAHILI -> "sw-TZ"
    else -> "en-US"
}

/**
 * Returns the BCP-47 tag for ML Kit Digital Ink model download.
 */
private fun handwritingLocale(language: String): String = when (language) {
    LanguageManager.LANGUAGE_SWAHILI -> "sw-TZ"
    else -> "en-US"
}

/**
 * Creates a speech recognition [Intent] for the given locale.
 */
private fun createSpeechIntent(locale: String): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFormContent(
    patientsVisited: String,
    vaccinationsGiven: String,
    healthEducation: String,
    challenges: String,
    notes: String,
    submissionState: Resource<String>?,
    healthEducationTopics: List<String>,
    isFormValid: Boolean,
    currentLanguage: String,
    onPatientsVisitedChange: (String) -> Unit,
    onVaccinationsGivenChange: (String) -> Unit,
    onHealthEducationChange: (String) -> Unit,
    onChallengesChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showEducationDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val speechNotAvailableMsg = stringResource(R.string.speech_not_available)

    val locale = speechLocale(currentLanguage)
    val hwLocale = handwritingLocale(currentLanguage)

    // ── Handwriting dialog state ────────────────────────────────────────
    var handwritingTarget by remember { mutableStateOf<String?>(null) }

    // ── Speech launchers (one per field that supports speech) ───────────
    val patientsVisitedSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            // Extract digits for numeric field
            val digits = spoken.filter { it.isDigit() }
            if (digits.isNotEmpty()) onPatientsVisitedChange(digits)
        }
    }

    val vaccinationsSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            val digits = spoken.filter { it.isDigit() }
            if (digits.isNotEmpty()) onVaccinationsGivenChange(digits)
        }
    }

    val challengesSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            if (spoken.isNotEmpty()) {
                val appended = if (challenges.isNotEmpty()) "$challenges $spoken" else spoken
                onChallengesChange(appended)
            }
        }
    }

    val notesSpeechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            if (spoken.isNotEmpty()) {
                val appended = if (notes.isNotEmpty()) "$notes $spoken" else spoken
                onNotesChange(appended)
            }
        }
    }

    fun launchSpeech(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        try {
            launcher.launch(createSpeechIntent(locale))
        } catch (_: Exception) {
            Toast.makeText(context, speechNotAvailableMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // ── Handwriting dialog ──────────────────────────────────────────────
    if (handwritingTarget != null) {
        HandwritingDialog(
            onDismiss = { handwritingTarget = null },
            onTextRecognized = { recognized ->
                when (handwritingTarget) {
                    "patientsVisited" -> {
                        val digits = recognized.filter { it.isDigit() }
                        if (digits.isNotEmpty()) onPatientsVisitedChange(digits)
                    }
                    "vaccinationsGiven" -> {
                        val digits = recognized.filter { it.isDigit() }
                        if (digits.isNotEmpty()) onVaccinationsGivenChange(digits)
                    }
                    "challenges" -> {
                        val appended = if (challenges.isNotEmpty()) "$challenges $recognized" else recognized
                        onChallengesChange(appended)
                    }
                    "notes" -> {
                        val appended = if (notes.isNotEmpty()) "$notes $recognized" else recognized
                        onNotesChange(appended)
                    }
                }
                handwritingTarget = null
            },
            languageTag = hwLocale
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Intro card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "\uD83D\uDCDD ", fontSize = 24.sp)
                Text(
                    text = stringResource(R.string.daily_report_intro),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Patients Visited field (mic + pen)
        OutlinedTextField(
            value = patientsVisited,
            onValueChange = onPatientsVisitedChange,
            label = { Text(stringResource(R.string.patients_visited_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                InputAssistRow(
                    onMicClick = { launchSpeech(patientsVisitedSpeechLauncher) },
                    onPenClick = { handwritingTarget = "patientsVisited" }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vaccinations Given field (mic + pen)
        OutlinedTextField(
            value = vaccinationsGiven,
            onValueChange = onVaccinationsGivenChange,
            label = { Text(stringResource(R.string.vaccinations_administered_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                InputAssistRow(
                    onMicClick = { launchSpeech(vaccinationsSpeechLauncher) },
                    onPenClick = { handwritingTarget = "vaccinationsGiven" }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Health Education Topics Covered dropdown (no assist)
        ExposedDropdownMenuBox(
            expanded = showEducationDropdown,
            onExpandedChange = { showEducationDropdown = it }
        ) {
            OutlinedTextField(
                value = healthEducation,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.health_education_topics_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEducationDropdown)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = showEducationDropdown,
                onDismissRequest = { showEducationDropdown = false }
            ) {
                healthEducationTopics.forEach { topic ->
                    DropdownMenuItem(
                        text = { Text(topic) },
                        onClick = {
                            onHealthEducationChange(topic)
                            showEducationDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Challenges Faced field (mic + pen)
        OutlinedTextField(
            value = challenges,
            onValueChange = onChallengesChange,
            label = { Text(stringResource(R.string.challenges_faced_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            trailingIcon = {
                InputAssistRow(
                    onMicClick = { launchSpeech(challengesSpeechLauncher) },
                    onPenClick = { handwritingTarget = "challenges" }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Notes field (mic + pen)
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.additional_notes)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            trailingIcon = {
                InputAssistRow(
                    onMicClick = { launchSpeech(notesSpeechLauncher) },
                    onPenClick = { handwritingTarget = "notes" }
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isFormValid && submissionState !is Resource.Loading
        ) {
            if (submissionState is Resource.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.submit_report), fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Required fields note
        Text(
            text = stringResource(R.string.required_fields_note),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReportHistoryContent(
    reports: List<DailyReport>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (reports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_reports_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ReportSummaryCard(reports)
            }
            item {
                Text(
                    text = stringResource(R.string.your_reports),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
            items(reports, key = { it.id }) { report ->
                ReportHistoryCard(report)
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(reports: List<DailyReport>) {
    val totalPatients = reports.sumOf { it.patientsVisited }
    val totalVaccinations = reports.sumOf { it.vaccinationsGiven }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.your_activity_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(
                    value = reports.size.toString(),
                    label = stringResource(R.string.total_reports_count)
                )
                SummaryStatItem(
                    value = totalPatients.toString(),
                    label = stringResource(R.string.patients_visited)
                )
                SummaryStatItem(
                    value = totalVaccinations.toString(),
                    label = stringResource(R.string.vaccines_administered)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReportHistoryCard(report: DailyReport) {
    val formattedDate = remember(report.date) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            val parsed = inputFormat.parse(report.date)
            if (parsed != null) outputFormat.format(parsed) else report.date
        } catch (_: Exception) {
            report.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date row with sync status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (report.isSynced)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (report.isSynced)
                            stringResource(R.string.synced_status)
                        else
                            stringResource(R.string.pending_sync_status),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (report.isSynced)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            HorizontalDivider()

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.patients_visited),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = report.patientsVisited.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.vaccines_administered),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = report.vaccinationsGiven.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (report.healthEducation.isNotBlank()) {
                Text(
                    text = stringResource(R.string.health_education_topics_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.healthEducation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (report.challenges.isNotBlank()) {
                Text(
                    text = stringResource(R.string.challenges_faced_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.challenges,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (report.notes.isNotBlank()) {
                Text(
                    text = stringResource(R.string.additional_notes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = report.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
