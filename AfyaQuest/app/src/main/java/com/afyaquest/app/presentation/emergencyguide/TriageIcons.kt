package com.afyaquest.app.presentation.emergencyguide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Maps a triage node's `icon` token to a visual cue (icon-first design, after
 * safe+natal's pictographic checklist). These are interim Material glyphs; custom
 * field-tested pictographs can replace them without touching the tree.
 */
fun triageIcon(token: String?): ImageVector = when (token) {
    "shield" -> Icons.Filled.Shield
    "gloves" -> Icons.Filled.PanTool
    "question" -> Icons.Filled.HelpOutline
    "person" -> Icons.Filled.Person
    "airway" -> Icons.Filled.Air
    "lungs" -> Icons.Filled.Masks
    "blood" -> Icons.Filled.Bloodtype
    "eye" -> Icons.Filled.Visibility
    "head" -> Icons.Filled.Face
    "chest" -> Icons.Filled.Favorite
    "abdomen" -> Icons.Filled.Sick
    "limb" -> Icons.Filled.Accessibility
    "spine" -> Icons.Filled.AirlineSeatFlat
    "history" -> Icons.Filled.MedicalServices
    "clock" -> Icons.Filled.Schedule
    "complaint" -> Icons.Filled.ReportProblem
    "pulse" -> Icons.Filled.MonitorHeart
    else -> Icons.Filled.MedicalServices
}

fun dispositionIcon(level: String): ImageVector = when (level) {
    "immediate", "early_exit" -> Icons.Filled.Warning
    "priority" -> Icons.Filled.PriorityHigh
    else -> Icons.Filled.CheckCircle
}

/** Circular icon badge shown above a triage question. Decorative — the question text carries meaning. */
@Composable
fun TriageStepIcon(token: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = triageIcon(token),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
    }
}
