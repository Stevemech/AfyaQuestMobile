package com.afyaquest.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afyaquest.app.R

/**
 * Sync status indicator showing connection state, pending items, and a force sync button.
 * Always visible so the user can manually trigger a sync at any time.
 */
@Composable
fun SyncStatusIndicator(
    isConnected: Boolean,
    unsyncedCount: Int,
    isSyncing: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    onSyncClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = when {
            !isConnected || errorMessage != null -> MaterialTheme.colorScheme.errorContainer
            isSyncing -> MaterialTheme.colorScheme.primaryContainer
            unsyncedCount > 0 -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = when {
                            !isConnected -> Icons.Default.CloudOff
                            errorMessage != null -> Icons.Default.CloudOff
                            unsyncedCount > 0 -> Icons.Default.CloudSync
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            !isConnected || errorMessage != null -> MaterialTheme.colorScheme.error
                            unsyncedCount > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Column {
                    Text(
                        text = when {
                            isSyncing -> stringResource(R.string.syncing)
                            !isConnected -> stringResource(R.string.offline)
                            errorMessage != null -> errorMessage
                            unsyncedCount > 0 -> stringResource(R.string.items_pending_count, unsyncedCount)
                            else -> stringResource(R.string.all_synced)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !isConnected || errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                            unsyncedCount > 0 || isSyncing -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (!isConnected) {
                        Text(
                            text = stringResource(R.string.changes_will_sync),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Always show sync button when connected and not currently syncing
            if (isConnected && !isSyncing && onSyncClick != null) {
                FilledTonalButton(
                    onClick = onSyncClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = stringResource(R.string.sync),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            errorMessage != null -> stringResource(R.string.retry)
                            unsyncedCount > 0 -> stringResource(R.string.sync_now)
                            else -> stringResource(R.string.sync)
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
