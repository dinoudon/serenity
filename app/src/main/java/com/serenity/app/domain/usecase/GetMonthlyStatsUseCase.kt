package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import com.serenity.app.domain.model.MonthlyStats
import com.serenity.app.domain.model.WeekAverage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class GetMonthlyStatsUseCase @Inject constructor() {

    operator fun invoke(
        rituals: List<DailyRitual>,
        today: LocalDate = LocalDate.now()
    ): MonthlyStats? {
        if (rituals.isEmpty()) return null

        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStarts = (3 downTo 0).map { thisWeekMonday.minusWeeks(it.toLong()) }

        val weeklyAverages = weekStarts.map { weekStart ->
            val weekEnd = weekStart.plusDays(6)
            val weekRituals = rituals.filter { it.date in weekStart..weekEnd }
            if (weekRituals.isEmpty()) {
                WeekAverage(weekStart = weekStart, averageScore = null, hasData = false)
            } else {
                val avg = roundHalfUp(weekRituals.sumOf { it.wellnessScore }.toDouble() / weekRituals.size)
                WeekAverage(weekStart = weekStart, averageScore = avg, hasData = true)
            }
        }

        val bestDay = rituals
            .maxWithOrNull(compareBy({ it.wellnessScore }, { it.date }))!!
            .let { it.date to it.wellnessScore }

        return MonthlyStats(
            weeklyAverages = weeklyAverages,
            bestDay = bestDay,
            topHabit = resolveTopHabit(rituals)
        )
    }

    private fun roundHalfUp(value: Double): Int = (value + 0.5).toInt()

    private fun resolveTopHabit(rituals: List<DailyRitual>): HabitType {
        val total = rituals.size.toDouble()
        return HabitType.entries
            .maxWithOrNull(
                compareBy<HabitType> { habit ->
                    rituals.count { it.hasHabit(habit) } / total
                }.thenByDescending { -it.ordinal }
            )!!
    }

    private fun DailyRitual.hasHabit(habit: HabitType): Boolean = when (habit) {
        HabitType.MOOD -> mood != null
        HabitType.SLEEP -> sleepHours != null
        HabitType.WATER -> waterGlasses != null
        HabitType.BREATHING -> breathingCompleted == true
        HabitType.GRATITUDE -> gratitudeNote != null && gratitudeNote.isNotBlank()
    }
}
