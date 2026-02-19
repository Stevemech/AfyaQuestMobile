package com.example.afyaquest.presentation.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
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

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEducationDropdown by remember { mutableStateOf(false) }

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
                navController.popBackStack()
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
                    // Show current date
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
                    Text(text = "📝 ", fontSize = 24.sp)
                    Text(
                        text = stringResource(R.string.daily_report_intro),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Patients Visited field
            OutlinedTextField(
                value = patientsVisited,
                onValueChange = { viewModel.setPatientsVisited(it) },
                label = { Text(stringResource(R.string.patients_visited_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Vaccinations Given field
            OutlinedTextField(
                value = vaccinationsGiven,
                onValueChange = { viewModel.setVaccinationsGiven(it) },
                label = { Text(stringResource(R.string.vaccinations_administered_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Health Education Topics Covered dropdown
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
                    viewModel.healthEducationTopics.forEach { topic ->
                        DropdownMenuItem(
                            text = { Text(topic) },
                            onClick = {
                                viewModel.setHealthEducation(topic)
                                showEducationDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Challenges Faced field
            OutlinedTextField(
                value = challenges,
                onValueChange = { viewModel.setChallenges(it) },
                label = { Text(stringResource(R.string.challenges_faced_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text(stringResource(R.string.additional_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = { viewModel.submitReport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = viewModel.isFormValid() && submissionState !is Resource.Loading
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
}
