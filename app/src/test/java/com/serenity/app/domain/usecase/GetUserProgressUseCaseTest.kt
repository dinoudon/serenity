package com.serenity.app.domain.usecase

import com.serenity.app.domain.repository.AchievementRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GetUserProgressUseCaseTest {

    private fun useCase(xp: Int, unlockedIds: Set<String> = emptySet()) =
        GetUserProgressUseCase(object : AchievementRepository {
            override suspend fun getUnlockedIds() = unlockedIds
            override suspend fun getUnlockedIdsWithTimestamps(): Map<String, Instant> =
                unlockedIds.associateWith { Instant.EPOCH }
            override suspend fun recordUnlock(id: String, at: Instant) {}
            override suspend fun getTotalXp() = xp
            override suspend fun addXp(amount: Int) {}
        })

    @Test
    fun `level 1 at 0 XP`() = runTest {
        val p = useCase(0)()
        assertEquals(1, p.level)
        assertEquals("Seedling", p.levelName)
        assertEquals("🌱", p.levelEmoji)
        assertEquals(0, p.xpIntoCurrentLevel)
        assertEquals(200, p.xpRequiredForNextLevel)
    }

    @Test
    fun `level 2 starts at 200 XP`() = runTest {
        val p = useCase(200)()
        assertEquals(2, p.level)
        assertEquals("Sprout", p.levelName)
        assertEquals(0, p.xpIntoCurrentLevel)
        assertEquals(300, p.xpRequiredForNextLevel) // 500 - 200
    }

    @Test
    fun `level 3 mid-progress`() = runTest {
        val p = useCase(650)()
        assertEquals(3, p.level)
        assertEquals(150, p.xpIntoCurrentLevel) // 650 - 500
        assertEquals(350, p.xpRequiredForNextLevel) // 1000 - 650
    }

    @Test
    fun `level 6 max level has null xpRequiredForNextLevel`() = runTest {
        val p = useCase(4000)()
        assertEquals(6, p.level)
        assertEquals("Serenity Sage", p.levelName)
        assertNull(p.xpRequiredForNextLevel)
    }

    @Test
    fun `unlocked achievements have non-null unlockedAt`() = runTest {
        val p = useCase(100, setOf("streak_7"))()
        val achievement = p.achievements.first { it.id == "streak_7" }
        assertTrue(achievement.unlockedAt != null)
    }

    @Test
    fun `locked achievements have null unlockedAt`() = runTest {
        val p = useCase(0)()
        val achievement = p.achievements.first { it.id == "streak_365" }
        assertNull(achievement.unlockedAt)
    }

    @Test
    fun `all 11 achievements present`() = runTest {
        val p = useCase(0)()
        assertEquals(11, p.achievements.size)
    }
}
