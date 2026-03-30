package com.serenity.app.domain.model

import java.time.Instant

enum class AchievementCategory { STREAK, SCORE, HABIT }

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: AchievementCategory,
    val xpReward: Int,
    val unlockedAt: Instant? // null = locked
)

data class UserProgress(
    val totalXP: Int,
    val level: Int,
    val levelName: String,
    val levelEmoji: String,
    val xpIntoCurrentLevel: Int,
    val xpRequiredForNextLevel: Int?, // null at max level (6)
    val achievements: List<Achievement>
)
