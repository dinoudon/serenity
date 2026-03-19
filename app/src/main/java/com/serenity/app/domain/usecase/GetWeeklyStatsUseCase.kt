package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import com.serenity.app.domain.model.WeeklyStats
import javax.inject.Inject

class GetWeeklyStatsUseCase @Inject constructor() {

    operator fun invoke(rituals: List<DailyRitual>): WeeklyStats? {
        if (rituals.isEmpty()) return null

        val averageScore = roundHalfUp(rituals.sumOf { it.wellnessScore }.toDouble() / rituals.size)

        val bestDay = rituals
            .maxWithOrNull(compareBy({ it.wellnessScore }, { it.date }))!!
            .let { it.date to it.wellnessScore }

        return WeeklyStats(
            averageScore = averageScore,
            bestDay = bestDay,
            topHabit = resolveTopHabit(rituals)
        )
    }

    private fun roundHalfUp(value: Double): Int = (value + 0.5).toInt()

    internal fun resolveTopHabit(rituals: List<DailyRitual>): HabitType {
        val total = rituals.size.toDouble()
        return HabitType.entries
            .maxWithOrNull(
                compareBy<HabitType> { habit ->
                    rituals.count { it.hasHabit(habit) } / total
                }.thenByDescending { -it.ordinal }
            )!!
    }

    internal fun DailyRitual.hasHabit(habit: HabitType): Boolean = when (habit) {
        HabitType.MOOD -> mood != null
        HabitType.SLEEP -> sleepHours != null
        HabitType.WATER -> waterGlasses != null
        HabitType.BREATHING -> breathingCompleted == true
        HabitType.GRATITUDE -> gratitudeNote != null && gratitudeNote.isNotBlank()
    }
}
