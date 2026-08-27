package com.afyaquest.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afyaquest.app.R
import com.afyaquest.app.ui.theme.AfyaSuccess

/*
 * Shared "guidance" components: the one visual language used across every workflow so a CHV
 * always recognises progress, completion, the next step, and how to leave safely.
 *
 *  - ProgressSummary   "Label            2/3"  + thin progress bar
 *  - StepIndicator     "Step 2 of 5"           + progress bar (quizzes, forms)
 *  - DoneBadge         small green check badge for cards / list rows
 *  - NextStepCard      highlighted "Continue / Next up" card with one clear action
 *  - HintRow           inline helper text with an icon ("Watch the video to unlock the quiz")
 *  - CompletionDialog  celebration + XP earned + primary/secondary next step
 *  - ConfirmLeaveDialog "Leave? You will lose progress" Stay/Leave
 *  - EmptyState / ErrorState / LoadingState for lists that load from the network
 */

@Composable
fun ProgressSummary(
    label: String,
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val safeTotal = total.coerceAtLeast(0)
    val fraction = if (safeTotal == 0) 0f else (done.toFloat() / safeTotal).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$done/$safeTotal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (safeTotal > 0 && done >= safeTotal) AfyaSuccess else color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (safeTotal > 0 && done >= safeTotal) AfyaSuccess else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun StepIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (total <= 0) 0f else (current.toFloat() / total).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.step_of_format, current, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
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
fun DoneBadge(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = stringResource(R.string.completed)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AfyaSuccess),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.7f)
        )
    }
}

@Composable
fun NextStepCard(
    title: String,
    subtitle: String?,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.PlayArrow,
    eyebrow: String = stringResource(R.string.next_up)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun HintRow(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

/** Small pill that shows XP earned ("+25 XP"). */
@Composable
fun XpChip(xp: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = stringResource(R.string.xp_earned_format, xp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Shown when a workflow finishes (quiz, daily questions, report, lesson).
 * Always offers one clear primary next step; the secondary action is optional.
 */
@Composable
fun CompletionDialog(
    title: String,
    message: String?,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    xpEarned: Int? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    onDismiss: () -> Unit = onPrimary,
    icon: ImageVector = Icons.Filled.EmojiEvents
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (xpEarned != null && xpEarned > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    XpChip(xp = xpEarned)
                }
            }
        },
        confirmButton = {
            Button(onClick = onPrimary) { Text(primaryText) }
        },
        dismissButton = if (secondaryText != null && onSecondary != null) {
            { TextButton(onClick = onSecondary) { Text(secondaryText) } }
        } else null
    )
}

/** "Leave this screen? You will lose progress" with Stay as the safe default. */
@Composable
fun ConfirmLeaveDialog(
    title: String,
    message: String,
    onStay: () -> Unit,
    onLeave: () -> Unit,
    stayText: String = stringResource(R.string.stay),
    leaveText: String = stringResource(R.string.leave)
) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onStay) { Text(stayText) }
        },
        dismissButton = {
            TextButton(onClick = onLeave) { Text(leaveText) }
        }
    )
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Info,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onAction) { Text(actionText) }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        title = message,
        modifier = modifier,
        icon = Icons.Outlined.CloudOff,
        actionText = if (onRetry != null) stringResource(R.string.retry) else null,
        onAction = onRetry
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
