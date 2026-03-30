package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.UserProgress
import com.serenity.app.domain.repository.AchievementRepository
import javax.inject.Inject

class GetUserProgressUseCase @Inject constructor(
    private val achievementRepository: AchievementRepository
) {

    suspend operator fun invoke(): UserProgress {
        val totalXp = achievementRepository.getTotalXp()
        val unlockedTimestamps = achievementRepository.getUnlockedIdsWithTimestamps()

        val level = levelFor(totalXp)
        val levelThreshold = LEVELS[level - 1].second
        val nextThreshold = LEVELS.getOrNull(level)?.second

        val achievements = AchievementCatalogue.all.map { template ->
            template.copy(unlockedAt = unlockedTimestamps[template.id])
        }

        return UserProgress(
            totalXP = totalXp,
            level = level,
            levelName = LEVELS[level - 1].first,
            levelEmoji = LEVEL_EMOJIS[level - 1],
            xpIntoCurrentLevel = totalXp - levelThreshold,
            xpRequiredForNextLevel = nextThreshold?.let { it - totalXp },
            achievements = achievements
        )
    }

    private fun levelFor(xp: Int): Int {
        for (i in LEVELS.indices.reversed()) {
            if (xp >= LEVELS[i].second) return i + 1
        }
        return 1
    }

    companion object {
        // Pair(name, xpRequired)
        val LEVELS = listOf(
            "Seedling"      to 0,
            "Sprout"        to 200,
            "Blossom"       to 500,
            "Grove Keeper"  to 1000,
            "Summit Walker" to 2000,
            "Serenity Sage" to 4000,
        )
        val LEVEL_EMOJIS = listOf("🌱", "🌿", "🌸", "🌳", "🏔️", "🌙")
    }
}
