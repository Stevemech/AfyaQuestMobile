package com.example.afyaquest.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.afyaquest.R

/**
 * Trailing-icon row providing mic and optional pen buttons for text field input assist.
 *
 * Place this as the `trailingIcon` of an [OutlinedTextField].
 */
@Composable
fun InputAssistRow(
    onMicClick: () -> Unit,
    onPenClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        IconButton(onClick = onMicClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.speech_to_text),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (onPenClick != null) {
            IconButton(onClick = onPenClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.handwriting_input),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
