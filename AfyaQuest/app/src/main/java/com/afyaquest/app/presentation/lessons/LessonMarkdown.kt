package com.afyaquest.app.presentation.lessons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afyaquest.app.presentation.components.MarkdownStyles
import com.afyaquest.app.presentation.components.MarkdownText

/**
 * Renders a lesson body. Lesson bodies are Markdown that opens with the lesson's own title; the
 * lesson screen already shows the title, so that first heading is dropped.
 */
@Composable
fun LessonMarkdown(content: String, modifier: Modifier = Modifier) {
    MarkdownText(
        content = content,
        modifier = modifier.fillMaxWidth(),
        style = MarkdownStyles.lesson(),
        dropLeadingTitle = true
    )
}
