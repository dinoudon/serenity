package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.AchievementCategory

object AchievementCatalogue {

    val all: List<Achievement> = listOf(
        Achievement("streak_7",    "Week Warrior",     "Complete rituals 7 days in a row",      "🔥", AchievementCategory.STREAK, 20, null),
        Achievement("streak_30",   "Monthly Mover",    "Complete rituals 30 days in a row",     "🏅", AchievementCategory.STREAK, 20, null),
        Achievement("streak_100",  "Century Sage",     "Complete rituals 100 days in a row",    "🏆", AchievementCategory.STREAK, 20, null),
        Achievement("streak_365",  "Year of Serenity", "Complete rituals 365 days in a row",    "💎", AchievementCategory.STREAK, 20, null),
        Achievement("score_perfect_1", "First Perfection", "Achieve a wellness score of 100",   "⭐", AchievementCategory.SCORE,  15, null),
        Achievement("score_perfect_5", "Five Star Life",   "Achieve 5 perfect wellness scores", "✨", AchievementCategory.SCORE,  15, null),
        Achievement("score_avg_80",    "Consistently Well", "Average score ≥ 80 over 7 days",  "🌟", AchievementCategory.SCORE,  15, null),
        Achievement("habit_sleep_10",      "Sleep Seeker",    "Log sleep 10 times",        "💤", AchievementCategory.HABIT, 10, null),
        Achievement("habit_water_20",      "Hydration Hero",  "Log water intake 20 times", "💧", AchievementCategory.HABIT, 10, null),
        Achievement("habit_gratitude_30",  "Grateful Heart",  "Write gratitude 30 times",  "📝", AchievementCategory.HABIT, 10, null),
        Achievement("habit_breathing_50",  "Breath Master",   "Complete breathing 50 times","🫁", AchievementCategory.HABIT, 10, null),
    )

    val byId: Map<String, Achievement> = all.associateBy { it.id }
}
