package com.afyaquest.app.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lesson bodies and Fred's answers are Markdown; these pin down the subset that is rendered. */
class MarkdownTextTest {

    @Test
    fun `headings, bullets, numbered steps and paragraphs become blocks`() {
        val blocks = parseMarkdown(
            "## Why It Matters\n" +
                "Handwashing prevents disease.\n" +
                "\n" +
                "### 1. Proteins\n" +
                "- Beans, lentils\n" +
                "* Eggs\n" +
                "1. **Wet hands** with clean water\n" +
                "2) Apply soap"
        )

        assertEquals(
            listOf(
                MarkdownBlock.Heading(2, "Why It Matters"),
                MarkdownBlock.Paragraph("Handwashing prevents disease."),
                MarkdownBlock.Heading(3, "1. Proteins"),
                MarkdownBlock.Bullet("Beans, lentils"),
                MarkdownBlock.Bullet("Eggs"),
                MarkdownBlock.Numbered("1", "**Wet hands** with clean water"),
                MarkdownBlock.Numbered("2", "Apply soap")
            ),
            blocks
        )
    }

    @Test
    fun `the opening title heading is dropped because the screen already shows the title`() {
        val blocks = parseMarkdown("# Prenatal Care Essentials\n\n## Regular Check-ups\n- Once in first 3 months", dropLeadingTitle = true)

        assertEquals(MarkdownBlock.Heading(2, "Regular Check-ups"), blocks.first())
        assertEquals(2, blocks.size)
    }

    @Test
    fun `a level one heading later in the lesson is kept`() {
        val blocks = parseMarkdown("Intro line\n# Part Two")

        assertEquals(listOf(MarkdownBlock.Paragraph("Intro line"), MarkdownBlock.Heading(1, "Part Two")), blocks)
    }

    @Test
    fun `text that only looks like markup stays a paragraph`() {
        val blocks = parseMarkdown("#hashtag\n-5 degrees\n100-120 per minute\n30:2")

        assertTrue(blocks.all { it is MarkdownBlock.Paragraph })
        assertEquals(4, blocks.size)
    }

    @Test
    fun `the opening title is kept unless the caller asks to drop it`() {
        val text = "# Danger signs\nGo to the clinic."
        assertEquals(MarkdownBlock.Heading(1, "Danger signs"), parseMarkdown(text).first())
        assertEquals(MarkdownBlock.Paragraph("Go to the clinic."), parseMarkdown(text, dropLeadingTitle = true).first())
    }

    @Test
    fun `bullet characters used by chat answers are recognised`() {
        assertEquals(
            listOf(MarkdownBlock.Bullet("Sunken eyes"), MarkdownBlock.Bullet("No tears")),
            parseMarkdown("• Sunken eyes\n* No tears")
        )
    }

    @Test
    fun `a table is flattened into rows a phone can show`() {
        val blocks = parseMarkdown(
            "| Situation | What to do |\n|-----------|:----------:|\n| **Bleeding** | Go to hospital |\n| Fever | Give fluids |"
        )

        assertEquals(
            listOf(
                MarkdownBlock.Bullet("Situation - What to do"),
                MarkdownBlock.Bullet("**Bleeding** - Go to hospital"),
                MarkdownBlock.Bullet("Fever - Give fluids")
            ),
            blocks
        )
    }

    @Test
    fun `blank and whitespace-only content gives no blocks`() {
        assertTrue(parseMarkdown("").isEmpty())
        assertTrue(parseMarkdown("  \n\n \n").isEmpty())
    }

    @Test
    fun `bold markers split a line into runs`() {
        assertEquals(
            listOf("Wash for at least " to false, "20 seconds" to true, " every time" to false),
            parseInlineBold("Wash for at least **20 seconds** every time")
        )
        assertEquals(listOf("Wet hands" to true, " with water" to false), parseInlineBold("**Wet hands** with water"))
        assertEquals(listOf("a" to true, " and " to false, "b" to true), parseInlineBold("**a** and **b**"))
    }

    @Test
    fun `plain text and unmatched markers are left as written`() {
        assertEquals(listOf("No markup here" to false), parseInlineBold("No markup here"))
        assertEquals(listOf("Rating: ** out of five" to false), parseInlineBold("Rating: ** out of five"))
        assertEquals(listOf("done" to true, " then ** stray" to false), parseInlineBold("**done** then ** stray"))
        assertTrue(parseInlineBold("").isEmpty())
    }

    @Test
    fun `every bundled lesson renders without leftover heading or bold markup`() {
        // Lesson bodies live in res/values*/strings*.xml; read them straight from the source tree
        // (tests run with the module directory as working directory).
        val lessonBody = Regex("<string name=\"lesson_\\d+_content\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        val perLanguage = mutableMapOf<String, Int>()
        listOf("values", "values-es", "values-b+cak").forEach { folder ->
            java.io.File("src/main/res/$folder").listFiles { file -> file.name.startsWith("strings") }!!.forEach { file ->
                lessonBody.findAll(file.readText()).forEach { match ->
                    val body = match.groupValues[1]
                        .replace("\\n", "\n").replace("\\'", "'").replace("\\\"", "\"")
                        .replace("&amp;", "&")
                    val blocks = parseMarkdown(body, dropLeadingTitle = true)
                    assertTrue("$folder: lesson has no content", blocks.isNotEmpty())
                    blocks.forEach { block ->
                        val text = when (block) {
                            is MarkdownBlock.Heading -> block.text
                            is MarkdownBlock.Bullet -> block.text
                            is MarkdownBlock.Numbered -> block.text
                            is MarkdownBlock.Paragraph -> block.text
                        }
                        assertTrue("$folder: heading markup left in '$text'", !text.startsWith("#"))
                        val shown = parseInlineBold(text).joinToString("") { it.first }
                        assertTrue("$folder: bold markup left in '$text'", !shown.contains("**"))
                    }
                    perLanguage[folder] = (perLanguage[folder] ?: 0) + 1
                }
            }
        }
        // 10 lessons in English and Spanish; Kaqchikel has the first 6 (the rest fall back).
        assertEquals(mapOf("values" to 10, "values-es" to 10, "values-b+cak" to 6), perLanguage)
    }
}
