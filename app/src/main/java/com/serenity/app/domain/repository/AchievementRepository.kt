package com.serenity.app.domain.repository

import java.time.Instant

interface AchievementRepository {
    suspend fun getUnlockedIds(): Set<String>
    suspend fun getUnlockedIdsWithTimestamps(): Map<String, Instant>
    suspend fun recordUnlock(achievementId: String, unlockedAt: Instant)
    suspend fun getTotalXp(): Int
    suspend fun addXp(amount: Int)
}
