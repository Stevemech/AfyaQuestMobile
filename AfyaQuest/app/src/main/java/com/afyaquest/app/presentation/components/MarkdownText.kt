package com.afyaquest.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * A small Markdown renderer for the two places the app shows authored text: lesson bodies and
 * Fred's chat answers. It covers the subset that actually occurs there - "#"/"##"/"###" headings,
 * "- " / "* " / "•" bullets, "1." numbered steps, **bold**, and (as a fallback) pipe tables, which
 * are flattened into lines because a phone bubble cannot show columns.
 *
 * The parser is plain Kotlin and unit-tested; [MarkdownText] turns its blocks into Compose text.
 */

/** One line-level element. Text still contains inline `**bold**` markers. */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Numbered(val number: String, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^[-*•]\\s+(.*)$")
private val NUMBERED = Regex("^(\\d+)[.)]\\s+(.*)$")
private val TABLE_ROW = Regex("^\\|(.*)\\|$")
private val TABLE_RULE = Regex("^\\|?[\\s:|-]+\\|?$")

/**
 * Splits [content] into blocks. Blank lines only separate blocks.
 *
 * With [dropLeadingTitle] a level-1 heading that opens the text is dropped; lesson bodies start
 * with their own title, which the lesson screen already shows.
 */
fun parseMarkdown(content: String, dropLeadingTitle: Boolean = false): List<MarkdownBlock> {
    val blocks = content.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line -> parseLine(line) }
    val first = blocks.firstOrNull()
    return if (dropLeadingTitle && first is MarkdownBlock.Heading && first.level == 1) blocks.drop(1) else blocks
}

private fun parseLine(line: String): MarkdownBlock? {
    HEADING.matchEntire(line)?.let { match ->
        return MarkdownBlock.Heading(level = match.groupValues[1].length, text = match.groupValues[2].trim())
    }
    BULLET.matchEntire(line)?.let { match ->
        return MarkdownBlock.Bullet(text = match.groupValues[1].trim())
    }
    NUMBERED.matchEntire(line)?.let { match ->
        return MarkdownBlock.Numbered(number = match.groupValues[1], text = match.groupValues[2].trim())
    }
    if (line.startsWith("|")) {
        // "|---|---|" only draws the rule under a table header: nothing to show.
        if (TABLE_RULE.matches(line)) return null
        TABLE_ROW.matchEntire(line)?.let { match ->
            val cells = match.groupValues[1].split("|").map { it.trim() }.filter { it.isNotEmpty() }
            return if (cells.isEmpty()) null else MarkdownBlock.Bullet(text = cells.joinToString(" - "))
        }
    }
    return MarkdownBlock.Paragraph(text = line)
}

/**
 * Splits text on `**bold**` markers into (text, isBold) runs. An unmatched `**` is kept as
 * written rather than swallowing the rest of the line.
 */
fun parseInlineBold(text: String): List<Pair<String, Boolean>> {
    val runs = mutableListOf<Pair<String, Boolean>>()
    var index = 0
    while (index < text.length) {
        val open = text.indexOf("**", index)
        val close = if (open >= 0) text.indexOf("**", open + 2) else -1
        if (open < 0 || close < 0) {
            runs += text.substring(index) to false
            break
        }
        if (open > index) runs += text.substring(index, open) to false
        if (close > open + 2) runs += text.substring(open + 2, close) to true
        index = close + 2
    }
    return runs
}

private fun styledText(text: String): AnnotatedString = buildAnnotatedString {
    parseInlineBold(text).forEach { (run, bold) ->
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(run) } else append(run)
    }
}

/** Sizes and colours for one use of [MarkdownText]. */
data class MarkdownStyle(
    val bodySize: TextUnit,
    val bodyLineHeight: TextUnit,
    val headingSize: TextUnit,
    val subheadingSize: TextUnit,
    val textColor: Color,
    val headingColor: Color,
    val markerColor: Color,
    /** Space above a section heading that is not the first block. */
    val headingSpacing: Dp,
    val paragraphSpacing: Dp
)

object MarkdownStyles {
    /** Long-form reading: lesson bodies. */
    @Composable
    fun lesson() = MarkdownStyle(
        bodySize = 16.sp,
        bodyLineHeight = 24.sp,
        headingSize = 20.sp,
        subheadingSize = 17.sp,
        textColor = MaterialTheme.colorScheme.onSurface,
        headingColor = MaterialTheme.colorScheme.primary,
        markerColor = MaterialTheme.colorScheme.primary,
        headingSpacing = 20.dp,
        paragraphSpacing = 4.dp
    )

    /** Compact: inside a chat bubble, everything in the bubble's own text colour. */
    @Composable
    fun chatBubble(textColor: Color) = MarkdownStyle(
        bodySize = 15.sp,
        bodyLineHeight = 20.sp,
        headingSize = 16.sp,
        subheadingSize = 15.sp,
        textColor = textColor,
        headingColor = textColor,
        markerColor = textColor,
        headingSpacing = 8.dp,
        paragraphSpacing = 3.dp
    )
}

/** Renders [content] (the Markdown subset described above). */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    style: MarkdownStyle = MarkdownStyles.lesson(),
    dropLeadingTitle: Boolean = false
) {
    val blocks = remember(content, dropLeadingTitle) { parseMarkdown(content, dropLeadingTitle) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val major = block.level <= 2
                    // Section headings get air above them, except when they open the text.
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(if (major) style.headingSpacing else style.headingSpacing * 0.7f))
                    }
                    Text(
                        text = styledText(block.text),
                        modifier = Modifier.semantics { heading() },
                        fontSize = if (major) style.headingSize else style.subheadingSize,
                        lineHeight = (if (major) style.headingSize else style.subheadingSize) * 1.3f,
                        fontWeight = FontWeight.Bold,
                        color = if (major) style.headingColor else style.textColor
                    )
                    Spacer(modifier = Modifier.height(style.paragraphSpacing + 2.dp))
                }

                is MarkdownBlock.Bullet -> ListRow(marker = "•", text = block.text, style = style)

                is MarkdownBlock.Numbered -> ListRow(marker = block.number + ".", text = block.text, style = style)

                is MarkdownBlock.Paragraph -> Text(
                    text = styledText(block.text),
                    modifier = Modifier.padding(vertical = style.paragraphSpacing),
                    fontSize = style.bodySize,
                    lineHeight = style.bodyLineHeight,
                    color = style.textColor
                )
            }
        }
    }
}

/** A bullet or numbered step: marker in a fixed gutter so wrapped lines stay aligned. */
@Composable
private fun ListRow(marker: String, text: String, style: MarkdownStyle) {
    Row(modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp)) {
        Text(
            text = marker,
            modifier = Modifier.widthIn(min = 24.dp),
            fontSize = style.bodySize,
            lineHeight = style.bodyLineHeight,
            fontWeight = FontWeight.Bold,
            color = style.markerColor
        )
        Text(
            text = styledText(text),
            modifier = Modifier.weight(1f, fill = false),
            fontSize = style.bodySize,
            lineHeight = style.bodyLineHeight,
            color = style.textColor
        )
    }
}
