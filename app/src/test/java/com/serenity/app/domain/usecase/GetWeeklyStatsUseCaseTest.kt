package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GetWeeklyStatsUseCaseTest {

    private val useCase = GetWeeklyStatsUseCase()

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

    @Test
    fun `returns null for empty list`() {
        assertNull(useCase(emptyList()))
    }

    @Test
    fun `average score rounds half up`() {
        // 64 + 65 = 129 / 2 = 64.5 → rounds up to 65
        val rituals = listOf(
            ritual(today, score = 64),
            ritual(today.minusDays(1), score = 65)
        )
        assertEquals(65, useCase(rituals)!!.averageScore)
    }

    @Test
    fun `best day is highest score`() {
        val rituals = listOf(
            ritual(today, score = 70),
            ritual(today.minusDays(1), score = 82),
            ritual(today.minusDays(2), score = 55)
        )
        val stats = useCase(rituals)!!
        assertEquals(today.minusDays(1), stats.bestDay.first)
        assertEquals(82, stats.bestDay.second)
    }

    @Test
    fun `best day tie resolved by most recent date`() {
        val rituals = listOf(
            ritual(today, score = 80),
            ritual(today.minusDays(1), score = 80)
        )
        assertEquals(today, useCase(rituals)!!.bestDay.first)
    }

    @Test
    fun `top habit mood wins when all completion rates equal`() {
        val rituals = listOf(ritual(today, score = 70))
        assertEquals(HabitType.MOOD, useCase(rituals)!!.topHabit)
    }

    @Test
    fun `top habit picks highest completion rate`() {
        val rituals = listOf(
            ritual(today, score = 70, mood = null, sleepHours = null,
                waterGlasses = null, breathingCompleted = true, gratitudeNote = null),
            ritual(today.minusDays(1), score = 60, mood = null, sleepHours = null,
                waterGlasses = null, breathingCompleted = true, gratitudeNote = null)
        )
        assertEquals(HabitType.BREATHING, useCase(rituals)!!.topHabit)
    }

    @Test
    fun `top habit tie-break mood over sleep`() {
        val rituals = listOf(
            ritual(today, score = 70, mood = 4, sleepHours = 7f,
                waterGlasses = null, breathingCompleted = null, gratitudeNote = null)
        )
        assertEquals(HabitType.MOOD, useCase(rituals)!!.topHabit)
    }

    @Test
    fun `gratitude blank note counts as skipped`() {
        val rituals = listOf(
            ritual(today, score = 70, mood = null, sleepHours = null,
                waterGlasses = null, breathingCompleted = null, gratitudeNote = "  ")
        )
        assertEquals(HabitType.MOOD, useCase(rituals)!!.topHabit)
    }
}
