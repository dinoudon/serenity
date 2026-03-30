package com.serenity.app.domain.usecase

import com.serenity.app.domain.repository.AchievementRepository
import com.serenity.app.domain.repository.RitualRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CheckAndUnlockAchievementsUseCase @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val ritualRepository: RitualRepository
) {

    suspend operator fun invoke(wellnessScore: Int) {
        val alreadyUnlocked = achievementRepository.getUnlockedIds()

        val streak = ritualRepository.getCurrentStreak()
        val perfectCount = ritualRepository.countPerfectScores()
        val sleepCount = ritualRepository.countNonNullSleep()
        val waterCount = ritualRepository.countNonNullWater()
        val gratitudeCount = ritualRepository.countNonNullGratitude()
        val breathingCount = ritualRepository.countBreathingCompleted()

        // 7-day average for score_avg_80
        val today = LocalDate.now()
        val weekRituals = ritualRepository.getRitualsInRange(today.minusDays(6), today).first()
        val weekAvg: Int? = if (weekRituals.isNotEmpty()) weekRituals.sumOf { it.wellnessScore } / weekRituals.size else null

        val triggers = mapOf(
            "streak_7"           to (streak >= 7),
            "streak_30"          to (streak >= 30),
            "streak_100"         to (streak >= 100),
            "streak_365"         to (streak >= 365),
            "score_perfect_1"    to (perfectCount >= 1),
            "score_perfect_5"    to (perfectCount >= 5),
            "score_avg_80"       to (weekAvg != null && weekAvg >= 80),
            "habit_sleep_10"     to (sleepCount >= 10),
            "habit_water_20"     to (waterCount >= 20),
            "habit_gratitude_30" to (gratitudeCount >= 30),
            "habit_breathing_50" to (breathingCount >= 50),
        )

        val now = Instant.now()
        var xpEarned = 10 // base XP per ritual

        // XP bonus for score
        xpEarned += when {
            wellnessScore == 100 -> 15
            wellnessScore >= 80  -> 5
            else -> 0
        }

        // Unlock new achievements and accumulate their XP
        for ((id, triggered) in triggers) {
            if (triggered && id !in alreadyUnlocked) {
                achievementRepository.recordUnlock(id, now)
                xpEarned += AchievementCatalogue.byId[id]?.xpReward ?: 0
            }
        }

        // Streak milestone bonus XP (awarded once per milestone — gated on first-time unlock)
        val streakMilestones = mapOf("streak_7" to 7, "streak_30" to 30, "streak_100" to 100, "streak_365" to 365)
        for ((id, threshold) in streakMilestones) {
            if (streak == threshold && id !in alreadyUnlocked) {
                xpEarned += 20
            }
        }

        achievementRepository.addXp(xpEarned)
    }
}
