package com.serenity.app.data.repository

import com.serenity.app.data.local.AchievementDao
import com.serenity.app.data.local.AchievementUnlockEntity
import com.serenity.app.data.local.UserProgressEntity
import com.serenity.app.domain.repository.AchievementRepository
import java.time.Instant
import javax.inject.Inject

class AchievementRepositoryImpl @Inject constructor(
    private val dao: AchievementDao
) : AchievementRepository {

    override suspend fun getUnlockedIds(): Set<String> =
        dao.getAllUnlocks().map { it.achievementId }.toSet()

    override suspend fun recordUnlock(achievementId: String, unlockedAt: Instant) {
        dao.insertUnlock(AchievementUnlockEntity(achievementId, unlockedAt.toEpochMilli()))
    }

    override suspend fun getTotalXp(): Int =
        dao.getUserProgress()?.totalXp ?: 0

    override suspend fun addXp(amount: Int) {
        val current = dao.getUserProgress()?.totalXp ?: 0
        dao.upsertUserProgress(UserProgressEntity(id = 1, totalXp = current + amount))
    }
}
