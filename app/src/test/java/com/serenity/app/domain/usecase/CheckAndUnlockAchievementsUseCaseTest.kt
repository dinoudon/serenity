package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.AchievementRepository
import com.serenity.app.domain.repository.RitualRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CheckAndUnlockAchievementsUseCaseTest {

    private lateinit var achievementRepo: FakeAchievementRepository
    private lateinit var ritualRepo: FakeRitualRepository
    private lateinit var useCase: CheckAndUnlockAchievementsUseCase

    @Before
    fun setup() {
        achievementRepo = FakeAchievementRepository()
        ritualRepo = FakeRitualRepository()
        useCase = CheckAndUnlockAchievementsUseCase(achievementRepo, ritualRepo)
    }

    @Test
    fun `base XP of 10 is always added`() = runTest {
        useCase(wellnessScore = 50)
        assertEquals(10, achievementRepo.totalXp)
    }

    @Test
    fun `score 80 adds 5 bonus XP`() = runTest {
        useCase(wellnessScore = 80)
        assertEquals(15, achievementRepo.totalXp)
    }

    @Test
    fun `score 100 adds 15 bonus XP not 5`() = runTest {
        useCase(wellnessScore = 100)
        assertEquals(25, achievementRepo.totalXp) // 10 base + 15 perfect bonus
    }

    @Test
    fun `streak 7 unlocks streak_7 and adds 20 milestone bonus`() = runTest {
        ritualRepo.streak = 7
        useCase(wellnessScore = 50)
        assertTrue("streak_7" in achievementRepo.unlocked)
        // 10 base + 20 achievement xp + 20 milestone bonus = 50
        assertEquals(50, achievementRepo.totalXp)
    }

    @Test
    fun `already unlocked achievement is not recorded again`() = runTest {
        ritualRepo.streak = 7
        achievementRepo.alreadyUnlocked = mutableSetOf("streak_7")
        useCase(wellnessScore = 50)
        assertTrue("streak_7" !in achievementRepo.unlocked)
    }

    @Test
    fun `milestone bonus XP not re-awarded when streak achievement already unlocked`() = runTest {
        ritualRepo.streak = 7
        achievementRepo.alreadyUnlocked = mutableSetOf("streak_7")
        useCase(wellnessScore = 50)
        // 10 base only — achievement XP and milestone bonus both suppressed
        assertEquals(10, achievementRepo.totalXp)
    }

    @Test
    fun `perfect score 1 unlocks score_perfect_1`() = runTest {
        ritualRepo.perfectScores = 1
        useCase(wellnessScore = 100)
        assertTrue("score_perfect_1" in achievementRepo.unlocked)
    }

    @Test
    fun `habit_sleep_10 unlocks when sleep count reaches 10`() = runTest {
        ritualRepo.sleepCount = 10
        useCase(wellnessScore = 50)
        assertTrue("habit_sleep_10" in achievementRepo.unlocked)
    }

    @Test
    fun `habit_sleep_10 does not unlock when sleep count is 9`() = runTest {
        ritualRepo.sleepCount = 9
        useCase(wellnessScore = 50)
        assertTrue("habit_sleep_10" !in achievementRepo.unlocked)
    }

    @Test
    fun `score_avg_80 unlocks when 7-day average is exactly 80`() = runTest {
        ritualRepo.weekRituals = listOf(
            fakeRitual(LocalDate.now(), 80),
            fakeRitual(LocalDate.now().minusDays(1), 80),
        )
        useCase(wellnessScore = 50)
        assertTrue("score_avg_80" in achievementRepo.unlocked)
    }

    @Test
    fun `score_avg_80 does not unlock when 7-day average is 79`() = runTest {
        ritualRepo.weekRituals = listOf(
            fakeRitual(LocalDate.now(), 79),
            fakeRitual(LocalDate.now().minusDays(1), 79),
        )
        useCase(wellnessScore = 50)
        assertTrue("score_avg_80" !in achievementRepo.unlocked)
    }

    private fun fakeRitual(date: LocalDate, score: Int) = DailyRitual(
        date = date, mood = 3, sleepHours = 7f, waterGlasses = 6,
        breathingCompleted = true, gratitudeNote = "test",
        wellnessScore = score, createdAt = Instant.EPOCH
    )
}

// ---- Fakes ----

class FakeAchievementRepository : AchievementRepository {
    var alreadyUnlocked: MutableSet<String> = mutableSetOf()
    val unlocked: MutableSet<String> = mutableSetOf()
    var totalXp: Int = 0

    override suspend fun getUnlockedIds(): Set<String> = alreadyUnlocked
    override suspend fun getUnlockedIdsWithTimestamps(): Map<String, Instant> =
        alreadyUnlocked.associateWith { Instant.EPOCH }
    override suspend fun recordUnlock(achievementId: String, unlockedAt: Instant) {
        unlocked.add(achievementId)
    }
    override suspend fun getTotalXp(): Int = totalXp
    override suspend fun addXp(amount: Int) { totalXp += amount }
}

class FakeRitualRepository : RitualRepository {
    var streak: Int = 0
    var perfectScores: Int = 0
    var sleepCount: Int = 0
    var waterCount: Int = 0
    var gratitudeCount: Int = 0
    var breathingCount: Int = 0
    var weekRituals: List<DailyRitual> = emptyList()

    override suspend fun saveRitual(ritual: DailyRitual) {}
    override fun getRitualByDate(date: LocalDate): Flow<DailyRitual?> = flowOf(null)
    override fun getRitualsInRange(start: LocalDate, end: LocalDate): Flow<List<DailyRitual>> =
        flowOf(weekRituals)
    override fun getLatestRitual(): Flow<DailyRitual?> = flowOf(null)
    override suspend fun getCurrentStreak(): Int = streak
    override suspend fun countPerfectScores(): Int = perfectScores
    override suspend fun countNonNullSleep(): Int = sleepCount
    override suspend fun countNonNullWater(): Int = waterCount
    override suspend fun countNonNullGratitude(): Int = gratitudeCount
    override suspend fun countBreathingCompleted(): Int = breathingCount
}
