package com.serenity.app.domain.model

import java.time.LocalTime

data class UserPreferences(
    val name: String = "",
    val ritualTime: LocalTime = LocalTime.of(8, 0),
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SAGE
)

enum class AppTheme { SAGE, LAVENDER, SAND }
