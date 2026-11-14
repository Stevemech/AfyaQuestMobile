package com.afyaquest.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The record of a daily answer has to survive being written to and read from the phone. */
class DailyAnswerTest {

    @Test
    fun `an answer survives a round trip`() {
        val answer = DailyAnswer(questionId = "imm-q3", selectedIndex = 2, correct = true, xp = 20)
        assertEquals(answer, DailyAnswer.decode(answer.encode()))

        val wrong = DailyAnswer(questionId = "imm-q4", selectedIndex = 0, correct = false, xp = 0)
        assertEquals(wrong, DailyAnswer.decode(wrong.encode()))
    }

    @Test
    fun `a question id may contain the separator`() {
        val answer = DailyAnswer(questionId = "set|7|q1", selectedIndex = 1, correct = true, xp = 30)
        assertEquals(answer, DailyAnswer.decode(answer.encode()))
    }

    @Test
    fun `damaged entries are ignored`() {
        assertNull(DailyAnswer.decode(""))
        assertNull(DailyAnswer.decode("imm-q3"))
        assertNull(DailyAnswer.decode("x|1|20|imm-q3"))
        assertNull(DailyAnswer.decode("2|1|lots|imm-q3"))
    }
}
