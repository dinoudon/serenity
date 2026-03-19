package com.serenity.app.domain.model

import java.time.LocalDate

data class WeeklyStats(
    val averageScore: Int,
    val bestDay: Pair<LocalDate, Int>,
    val topHabit: HabitType
)
