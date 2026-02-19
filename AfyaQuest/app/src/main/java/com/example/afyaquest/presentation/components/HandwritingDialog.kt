package com.example.afyaquest.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.afyaquest.R
import com.example.afyaquest.util.HandwritingRecognitionHelper
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen dialog with a finger-drawing canvas and ML Kit Digital Ink recognition.
 *
 * @param onDismiss Called when the user cancels.
 * @param onTextRecognized Called with the recognized text when the user taps Done.
 * @param languageTag BCP-47 tag for the recognition model, e.g. "en-US" or "sw-TZ".
 */
@Composable
fun HandwritingDialog(
    onDismiss: () -> Unit,
    onTextRecognized: (String) -> Unit,
    languageTag: String
) {
    val helper = remember { HandwritingRecognitionHelper() }
    val scope = rememberCoroutineScope()

    var modelReady by remember { mutableStateOf(false) }
    var modelLoading by remember { mutableStateOf(true) }
    var recognizedText by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(false) }

    // Drawing state
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    // Ink builder for ML Kit
    val inkBuilder = remember { Ink.builder() }
    var strokeBuilder by remember { mutableStateOf<Ink.Stroke.Builder?>(null) }

    // Auto-recognize debounce
    var recognizeJob by remember { mutableStateOf<Job?>(null) }

    // Download model on open
    LaunchedEffect(languageTag) {
        modelLoading = true
        modelReady = helper.ensureModelReady(languageTag)
        modelLoading = false
    }

    // Clean up recognizer on dispose
    DisposableEffect(Unit) {
        onDispose { helper.close() }
    }

    fun triggerRecognition() {
        if (!modelReady) return
        recognizeJob?.cancel()
        recognizeJob = scope.launch {
            delay(600) // wait for user to stop drawing
            isRecognizing = true
            try {
                val ink = inkBuilder.build()
                val text = helper.recognize(ink)
                if (text.isNotEmpty()) {
                    recognizedText = text
                }
            } catch (_: Exception) {
                // Silently ignore recognition errors (e.g. empty ink)
            }
            isRecognizing = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.handwriting_input),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            paths.clear()
                            currentPath.clear()
                            recognizedText = ""
                            // Reset ink builder by creating fresh state
                            inkBuilder.build() // consume current
                        }) {
                            Text(stringResource(R.string.clear))
                        }
                        Button(
                            onClick = { onTextRecognized(recognizedText) },
                            enabled = recognizedText.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model status
                if (modelLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.downloading_model),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Canvas area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(modelReady) {
                                if (!modelReady) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath.clear()
                                        currentPath.add(offset)
                                        strokeBuilder = Ink.Stroke
                                            .builder()
                                            .addPoint(
                                                Ink.Point.create(
                                                    offset.x,
                                                    offset.y,
                                                    System.currentTimeMillis()
                                                )
                                            )
                                        recognizeJob?.cancel()
                                    },
                                    onDrag = { change, _ ->
                                        val pos = change.position
                                        currentPath.add(pos)
                                        strokeBuilder?.addPoint(
                                            Ink.Point.create(
                                                pos.x,
                                                pos.y,
                                                System.currentTimeMillis()
                                            )
                                        )
                                    },
                                    onDragEnd = {
                                        paths.add(currentPath.toList())
                                        currentPath.clear()
                                        strokeBuilder?.let { inkBuilder.addStroke(it.build()) }
                                        strokeBuilder = null
                                        triggerRecognition()
                                    },
                                    onDragCancel = {
                                        currentPath.clear()
                                        strokeBuilder = null
                                    }
                                )
                            }
                    ) {
                        val strokeStyle = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                        val inkColor = Color.Black

                        // Draw completed paths
                        for (points in paths) {
                            if (points.size < 2) continue
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            drawPath(path, inkColor, style = strokeStyle)
                        }

                        // Draw current stroke
                        if (currentPath.size >= 2) {
                            val path = Path().apply {
                                moveTo(currentPath[0].x, currentPath[0].y)
                                for (i in 1 until currentPath.size) {
                                    lineTo(currentPath[i].x, currentPath[i].y)
                                }
                            }
                            drawPath(path, inkColor, style = strokeStyle)
                        }
                    }

                    // Placeholder text
                    if (paths.isEmpty() && currentPath.isEmpty()) {
                        Text(
                            text = stringResource(R.string.draw_here),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recognition result
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecognizing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.recognizing),
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = recognizedText.ifEmpty {
                                    stringResource(R.string.draw_here)
                                },
                                fontSize = 14.sp,
                                color = if (recognizedText.isEmpty())
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
