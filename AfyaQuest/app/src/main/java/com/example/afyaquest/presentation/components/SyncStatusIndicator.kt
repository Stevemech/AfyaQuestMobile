package com.example.afyaquest.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sync status indicator showing connection and pending sync items
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
    AnimatedVisibility(
        visible = !isConnected || unsyncedCount > 0 || isSyncing || errorMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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
                            tint = when {
                                !isConnected || errorMessage != null -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Column {
                        Text(
                            text = when {
                                isSyncing -> "Syncing..."
                                !isConnected -> "Offline"
                                errorMessage != null -> errorMessage
                                unsyncedCount > 0 -> "$unsyncedCount item${if (unsyncedCount > 1) "s" else ""} pending"
                                else -> "All synced"
                            },
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = when {
                                !isConnected || errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )

                        if (!isConnected) {
                            Text(
                                text = "Changes will sync when online",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isConnected && (unsyncedCount > 0 || errorMessage != null) && !isSyncing && onSyncClick != null) {
                    TextButton(onClick = onSyncClick) {
                        Text(if (errorMessage != null) "Retry" else "Sync Now")
                    }
                }
            }
        }
    }
}
