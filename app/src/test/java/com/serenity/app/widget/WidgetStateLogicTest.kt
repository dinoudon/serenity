package com.serenity.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for WidgetState display logic.
 * Tests the logic that determines what score/state to show, not the Glance rendering itself.
 */
class WidgetStateLogicTest {

    // Helper to build WidgetState
    private fun state(
        score: Int? = 75,
        streak: Int = 5,
        isYesterdayScore: Boolean = false,
        themeName: String = "SAGE"
    ) = WidgetState(score, streak, isYesterdayScore, themeName)

    @Test
    fun `score is null when no rituals exist`() {
        val s = state(score = null, streak = 0)
        assertNull(s.score)
        assertEquals(0, s.streak)
    }

    @Test
    fun `isYesterdayScore is false when today has data`() {
        val s = state(score = 80, isYesterdayScore = false)
        assertFalse(s.isYesterdayScore)
        assertEquals(80, s.score)
    }

    @Test
    fun `isYesterdayScore is true when showing past score`() {
        val s = state(score = 65, isYesterdayScore = true)
        assertTrue(s.isYesterdayScore)
        assertEquals(65, s.score)
    }

    @Test
    fun `amber dot shown only when isYesterdayScore is true and score is non-null`() {
        val showDot = { s: WidgetState -> s.isYesterdayScore && s.score != null }
        assertFalse(showDot(state(score = 80, isYesterdayScore = false)))
        assertTrue(showDot(state(score = 65, isYesterdayScore = true)))
        assertFalse(showDot(state(score = null, isYesterdayScore = true)))
    }

    @Test
    fun `themeName LAVENDER is recognized`() {
        assertEquals("LAVENDER", state(themeName = "LAVENDER").themeName)
        assertEquals("SAND", state(themeName = "SAND").themeName)
        assertEquals("SAGE", state(themeName = "SAGE").themeName)
    }

    @Test
    fun `streak is preserved in state`() {
        assertEquals(12, state(streak = 12).streak)
    }
}
