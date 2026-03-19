package com.serenity.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class DailyRitual(
    val id: Long = 0,
    val date: LocalDate,
    val mood: Int?,
    val sleepHours: Float?,
    val waterGlasses: Int?,
    val breathingCompleted: Boolean?,
    val gratitudeNote: String?,
    val wellnessScore: Int,
    val createdAt: Instant
)
