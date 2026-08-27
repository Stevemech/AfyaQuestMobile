package com.afyaquest.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afyaquest.app.R

/**
 * Sync status indicator showing connection state, pending items, and a force sync button.
 *
 * When everything is already synced (online, nothing pending, no error, not syncing) it collapses
 * to a compact one-line row so the Home hub stays focused on today's tasks; the full banner is
 * shown whenever the user needs to know something (offline, pending, error, syncing).
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
    val allGood = isConnected && unsyncedCount == 0 && errorMessage == null && !isSyncing
    if (allGood) {
        CompactSyncedRow(modifier = modifier, onSyncClick = onSyncClick)
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = when {
            !isConnected || errorMessage != null -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
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
                            else -> Icons.Default.CloudSync
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            !isConnected || errorMessage != null -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Column {
                    Text(
                        text = when {
                            isSyncing -> stringResource(R.string.syncing)
                            !isConnected -> stringResource(R.string.offline)
                            errorMessage != null -> errorMessage
                            else -> stringResource(R.string.items_pending_count, unsyncedCount)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !isConnected || errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
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

            // Show sync button when connected and not currently syncing
            if (isConnected && !isSyncing && onSyncClick != null) {
                FilledTonalButton(
                    onClick = onSyncClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            errorMessage != null -> stringResource(R.string.retry)
                            else -> stringResource(R.string.sync_now)
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/** One-line "All synced" row with a small manual sync action. */
@Composable
private fun CompactSyncedRow(
    modifier: Modifier = Modifier,
    onSyncClick: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(36.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onSyncClick != null) {
            TextButton(
                onClick = onSyncClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.all_synced),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.all_synced),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
