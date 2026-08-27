package com.afyaquest.app.presentation.emergencyguide

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afyaquest.app.R

/**
 * Maps a triage node's `icon` token to a visual cue (icon-first design, after
 * safe+natal's pictographic checklist). Each token now has a full pictographic
 * illustration (`emerg_pic_*`); the Material glyphs remain as a fallback for
 * any token added to the tree before its artwork exists.
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

/** Pictographic illustration for a triage node's `icon` token, or null if none exists. */
@DrawableRes
fun triagePictureRes(token: String?): Int? = when (token) {
    "shield" -> R.drawable.emerg_pic_shield
    "gloves" -> R.drawable.emerg_pic_gloves
    "question" -> R.drawable.emerg_pic_question
    "person" -> R.drawable.emerg_pic_person
    "airway" -> R.drawable.emerg_pic_airway
    "lungs" -> R.drawable.emerg_pic_lungs
    "blood" -> R.drawable.emerg_pic_blood
    "eye" -> R.drawable.emerg_pic_eye
    "head" -> R.drawable.emerg_pic_head
    "chest" -> R.drawable.emerg_pic_chest
    "abdomen" -> R.drawable.emerg_pic_abdomen
    "limb" -> R.drawable.emerg_pic_limb
    "spine" -> R.drawable.emerg_pic_spine
    "history" -> R.drawable.emerg_pic_history
    "clock" -> R.drawable.emerg_pic_clock
    "complaint" -> R.drawable.emerg_pic_complaint
    "pulse" -> R.drawable.emerg_pic_pulse
    else -> null
}

/** Pictographic illustration for a disposition, keyed by its id. */
@DrawableRes
fun dispositionPictureRes(dispositionId: String): Int? = when (dispositionId) {
    "disp_unsafe" -> R.drawable.emerg_pic_danger
    "disp_cpr" -> R.drawable.emerg_pic_cpr
    "disp_immediate" -> R.drawable.emerg_pic_ambulance
    "disp_priority" -> R.drawable.emerg_pic_ambulance_warn
    "disp_stable" -> R.drawable.emerg_pic_check
    else -> null
}

fun dispositionIcon(level: String): ImageVector = when (level) {
    "immediate", "early_exit" -> Icons.Filled.Warning
    "priority" -> Icons.Filled.PriorityHigh
    else -> Icons.Filled.CheckCircle
}

/**
 * Illustration shown with a triage question: the pictograph when one exists,
 * otherwise the circular Material-glyph badge. Decorative — the question text
 * carries meaning.
 */
@Composable
fun TriageStepPicture(token: String?, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val pic = triagePictureRes(token)
    if (pic != null) {
        Image(
            painter = painterResource(pic),
            contentDescription = null,
            modifier = modifier.size(size)
        )
    } else {
        TriageStepIcon(token, modifier)
    }
}

/** Circular icon badge fallback for tokens without artwork. */
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
