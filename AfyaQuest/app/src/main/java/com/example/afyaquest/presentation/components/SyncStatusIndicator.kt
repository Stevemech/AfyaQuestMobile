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
    modifier: Modifier = Modifier,
    onSyncClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = !isConnected || unsyncedCount > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
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
                    Icon(
                        imageVector = when {
                            !isConnected -> Icons.Default.CloudOff
                            unsyncedCount > 0 -> Icons.Default.CloudSync
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null,
                        tint = if (isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )

                    Column {
                        Text(
                            text = if (isConnected) {
                                if (unsyncedCount > 0) "Syncing..." else "All synced"
                            } else {
                                "Offline"
                            },
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = if (isConnected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )

                        if (unsyncedCount > 0) {
                            Text(
                                text = "$unsyncedCount item${if (unsyncedCount > 1) "s" else ""} pending",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!isConnected) {
                            Text(
                                text = "Changes will sync when online",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isConnected && unsyncedCount > 0 && onSyncClick != null) {
                    TextButton(onClick = onSyncClick) {
                        Text("Sync Now")
                    }
                }
            }
        }
    }
}
