package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class GetMonthlyStatsUseCaseTest {

    private val useCase = GetMonthlyStatsUseCase()

    private fun ritual(
        date: LocalDate,
        score: Int,
        mood: Int? = 3,
        sleepHours: Float? = 7f,
        waterGlasses: Int? = 6,
        breathingCompleted: Boolean? = true,
        gratitudeNote: String? = "grateful"
    ) = DailyRitual(
        date = date,
        mood = mood,
        sleepHours = sleepHours,
        waterGlasses = waterGlasses,
        breathingCompleted = breathingCompleted,
        gratitudeNote = gratitudeNote,
        wellnessScore = score,
        createdAt = Instant.EPOCH
    )

    private val today = LocalDate.of(2026, 3, 20)
    private val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @Test
    fun `returns null for empty list`() {
        assertNull(useCase(emptyList(), today))
    }

    @Test
    fun `produces 4 week averages`() {
        assertEquals(4, useCase(listOf(ritual(today, score = 70)), today)!!.weeklyAverages.size)
    }

    @Test
    fun `week starts are correct Mondays`() {
        val stats = useCase(listOf(ritual(today, score = 70)), today)!!
        val expected = (3 downTo 0).map { thisWeekMonday.minusWeeks(it.toLong()) }
        assertEquals(expected, stats.weeklyAverages.map { it.weekStart })
    }

    @Test
    fun `week with no rituals has hasData false and null averageScore`() {
        val stats = useCase(listOf(ritual(today, score = 70)), today)!!
        val oldestWeek = stats.weeklyAverages.first()
        assertFalse(oldestWeek.hasData)
        assertNull(oldestWeek.averageScore)
    }

    @Test
    fun `week with rituals has correct average`() {
        val rituals = listOf(
            ritual(thisWeekMonday, score = 60),
            ritual(thisWeekMonday.plusDays(1), score = 80)
        )
        val latestWeek = useCase(rituals, today)!!.weeklyAverages.last()
        assertTrue(latestWeek.hasData)
        assertEquals(70, latestWeek.averageScore)
    }

    @Test
    fun `weekly average rounds half up`() {
        val rituals = listOf(
            ritual(thisWeekMonday, score = 60),
            ritual(thisWeekMonday.plusDays(1), score = 61)
        )
        assertEquals(61, useCase(rituals, today)!!.weeklyAverages.last().averageScore)
    }

    @Test
    fun `bestDay is single best day across all 28 days`() {
        val week1Mon = thisWeekMonday.minusWeeks(3)
        val rituals = listOf(
            ritual(today, score = 70),
            ritual(week1Mon, score = 95)
        )
        val stats = useCase(rituals, today)!!
        assertEquals(week1Mon, stats.bestDay.first)
        assertEquals(95, stats.bestDay.second)
    }

    @Test
    fun `topHabit tie-break respects mood priority`() {
        assertEquals(HabitType.MOOD, useCase(listOf(ritual(today, score = 70)), today)!!.topHabit)
    }
}
