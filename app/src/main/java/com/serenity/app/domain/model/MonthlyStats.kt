package com.serenity.app.domain.model

import java.time.LocalDate

data class WeekAverage(
    val weekStart: LocalDate,
    val averageScore: Int?,
    val hasData: Boolean
)

data class MonthlyStats(
    val weeklyAverages: List<WeekAverage>,
    val bestDay: Pair<LocalDate, Int>,
    val topHabit: HabitType
)
