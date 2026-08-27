package com.afyaquest.app.presentation.emergencyguide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.afyaquest.app.R
import com.afyaquest.app.data.local.entity.CaseLogEntity
import com.afyaquest.app.domain.triage.localized
import com.afyaquest.app.ui.theme.AfyaQuestTheme
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Read-only list of the current user's past emergency assessments, newest first.
 * Disposition labels/colors are resolved from the (current) tree by stored id.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseHistoryScreen(
    navController: NavController,
    viewModel: CaseHistoryViewModel = hiltViewModel()
) {
    val cases by viewModel.cases.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val engine = viewModel.engine

    AfyaQuestTheme(darkTheme = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.emergency_history), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (cases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.emergency_history_empty),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cases, key = { it.id }) { case ->
                        CaseRow(
                            case = case,
                            label = engine.disposition(case.dispositionId)?.label.localized(language)
                                .ifBlank { case.dispositionId },
                            colors = severityColorsFor(
                                engine.disposition(case.dispositionId)?.color ?: ""
                            ),
                            mechanismLabel = mechanismLabel(case.mechanism)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaseRow(
    case: CaseLogEntity,
    label: String,
    colors: SeverityColors,
    mechanismLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(colors.main)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$mechanismLabel · ${formatDate(case.completedAt.time)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!case.isSynced) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.emergency_not_synced),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun mechanismLabel(mechanism: String?): String = when (mechanism) {
    "trauma" -> stringResource(R.string.emergency_mechanism_trauma)
    "medical" -> stringResource(R.string.emergency_mechanism_medical)
    else -> stringResource(R.string.emergency_mechanism_unknown)
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(epochMillis)
