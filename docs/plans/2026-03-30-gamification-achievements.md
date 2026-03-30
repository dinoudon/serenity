# Gamification & Achievements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add XP progression, level system, and achievement badges to Serenity via a new Profile screen accessible through a bottom navigation bar.

**Architecture:** New domain models (`Achievement`, `UserProgress`) + two new use cases (`CheckAndUnlockAchievementsUseCase`, `GetUserProgressUseCase`) + new Room tables (`achievement_unlock`, `user_progress`). `CompleteRitualUseCase` calls achievement checking after saving. `MainActivity` gains a 4-item `NavigationBar`; `ProfileScreen` is the new destination.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Room (migration v1→v2), Hilt, JUnit 4, Coroutines Test

---

## File Map

**Create:**
- `domain/model/Achievement.kt` — `Achievement`, `AchievementCategory`, `UserProgress` models
- `domain/repository/AchievementRepository.kt` — interface
- `domain/usecase/AchievementCatalogue.kt` — hardcoded list of all 11 achievements
- `domain/usecase/CheckAndUnlockAchievementsUseCase.kt` — checks triggers, writes unlocks + XP
- `domain/usecase/GetUserProgressUseCase.kt` — reads DB, computes level, hydrates achievements
- `data/local/AchievementUnlockEntity.kt` — Room entity for unlock records
- `data/local/UserProgressEntity.kt` — Room entity (single-row, stores totalXP)
- `data/local/AchievementDao.kt` — DAO for both entities
- `data/repository/AchievementRepositoryImpl.kt` — impl
- `ui/profile/ProfileViewModel.kt`
- `ui/profile/ProfileScreen.kt`
- `test/.../CheckAndUnlockAchievementsUseCaseTest.kt`
- `test/.../GetUserProgressUseCaseTest.kt`

**Modify:**
- `data/local/RitualDao.kt` — add 5 count queries
- `data/local/SerenityDatabase.kt` — add entities, bump to v2, add migration
- `di/DatabaseModule.kt` — provide `AchievementDao`
- `di/RepositoryModule.kt` — bind `AchievementRepository`
- `domain/usecase/CompleteRitualUseCase.kt` — call `CheckAndUnlockAchievementsUseCase`
- `ui/navigation/Routes.kt` — add `Profile` route
- `ui/navigation/SerenityNavGraph.kt` — add bottom nav + Profile destination, remove back-nav from History/Settings
- `ui/home/HomeScreen.kt` — remove History/Settings buttons, add level pill below score ring
- `ui/home/HomeViewModel.kt` — add `levelName`/`levelEmoji` to `HomeUiState` via `GetUserProgressUseCase`

---

## Task 1: Domain Models

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/model/Achievement.kt`

- [ ] **Step 1: Create the file**

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/model/Achievement.kt
git commit -m "feat: add Achievement and UserProgress domain models"
```

---

## Task 2: Achievement Catalogue

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/usecase/AchievementCatalogue.kt`

- [ ] **Step 1: Create the catalogue object**

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/AchievementCatalogue.kt
git commit -m "feat: add hardcoded achievement catalogue"
```

---

## Task 3: Data Layer — Entities + DAO

**Files:**
- Create: `app/src/main/java/com/serenity/app/data/local/AchievementUnlockEntity.kt`
- Create: `app/src/main/java/com/serenity/app/data/local/UserProgressEntity.kt`
- Create: `app/src/main/java/com/serenity/app/data/local/AchievementDao.kt`

- [ ] **Step 1: Create AchievementUnlockEntity**

```kotlin
package com.serenity.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_unlock")
data class AchievementUnlockEntity(
    @PrimaryKey
    val achievementId: String,
    val unlockedAt: Long // epoch millis
)
```

- [ ] **Step 2: Create UserProgressEntity**

```kotlin
package com.serenity.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: Int = 1, // single-row table
    val totalXp: Int = 0
)
```

- [ ] **Step 3: Create AchievementDao**

```kotlin
package com.serenity.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(entity: AchievementUnlockEntity)

    @Query("SELECT * FROM achievement_unlock")
    suspend fun getAllUnlocks(): List<AchievementUnlockEntity>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgress(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProgress(entity: UserProgressEntity)
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/serenity/app/data/local/AchievementUnlockEntity.kt \
        app/src/main/java/com/serenity/app/data/local/UserProgressEntity.kt \
        app/src/main/java/com/serenity/app/data/local/AchievementDao.kt
git commit -m "feat: add AchievementUnlockEntity, UserProgressEntity, AchievementDao"
```

---

## Task 4: Add Count Queries to RitualDao + DB Migration

**Files:**
- Modify: `app/src/main/java/com/serenity/app/data/local/RitualDao.kt`
- Modify: `app/src/main/java/com/serenity/app/data/local/SerenityDatabase.kt`

- [ ] **Step 1: Add 5 count queries to RitualDao**

Open `RitualDao.kt` and add these queries at the bottom of the interface (before the closing brace):

```kotlin
    @Query("SELECT COUNT(*) FROM daily_ritual WHERE sleepHours IS NOT NULL")
    suspend fun countNonNullSleep(): Int

    @Query("SELECT COUNT(*) FROM daily_ritual WHERE waterGlasses IS NOT NULL")
    suspend fun countNonNullWater(): Int

    @Query("SELECT COUNT(*) FROM daily_ritual WHERE gratitudeNote IS NOT NULL AND gratitudeNote != ''")
    suspend fun countNonNullGratitude(): Int

    @Query("SELECT COUNT(*) FROM daily_ritual WHERE breathingCompleted = 1")
    suspend fun countBreathingCompleted(): Int

    @Query("SELECT COUNT(*) FROM daily_ritual WHERE wellnessScore = 100")
    suspend fun countPerfectScores(): Int
```

- [ ] **Step 2: Update SerenityDatabase — bump version, add entities, add migration**

Replace the entire content of `SerenityDatabase.kt`:

```kotlin
package com.serenity.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RitualEntity::class, AchievementUnlockEntity::class, UserProgressEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SerenityDatabase : RoomDatabase() {
    abstract fun ritualDao(): RitualDao
    abstract fun achievementDao(): AchievementDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `achievement_unlock` (
                `achievementId` TEXT NOT NULL PRIMARY KEY,
                `unlockedAt` INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `user_progress` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `totalXp` INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/data/local/RitualDao.kt \
        app/src/main/java/com/serenity/app/data/local/SerenityDatabase.kt
git commit -m "feat: add habit count queries to RitualDao and Room migration v1→v2"
```

---

## Task 5: Repository Interface + Impl

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/repository/AchievementRepository.kt`
- Create: `app/src/main/java/com/serenity/app/data/repository/AchievementRepositoryImpl.kt`

- [ ] **Step 1: Create the interface**

```kotlin
package com.serenity.app.domain.repository

import java.time.Instant

interface AchievementRepository {
    suspend fun getUnlockedIds(): Set<String>
    suspend fun recordUnlock(achievementId: String, unlockedAt: Instant)
    suspend fun getTotalXp(): Int
    suspend fun addXp(amount: Int)
}
```

- [ ] **Step 2: Create the impl**

```kotlin
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
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/repository/AchievementRepository.kt \
        app/src/main/java/com/serenity/app/data/repository/AchievementRepositoryImpl.kt
git commit -m "feat: add AchievementRepository interface and impl"
```

---

## Task 6: Wire DI

**Files:**
- Modify: `app/src/main/java/com/serenity/app/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/serenity/app/di/RepositoryModule.kt`

- [ ] **Step 1: Add AchievementDao provider and migration to DatabaseModule**

Replace the `provideDatabase` function and add a new `provideAchievementDao` function. The full file becomes:

```kotlin
package com.serenity.app.di

import android.content.Context
import androidx.room.Room
import com.serenity.app.data.local.AchievementDao
import com.serenity.app.data.local.MIGRATION_1_2
import com.serenity.app.data.local.RitualDao
import com.serenity.app.data.local.SerenityDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SerenityDatabase {
        return Room.databaseBuilder(
            context,
            SerenityDatabase::class.java,
            "serenity_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideRitualDao(database: SerenityDatabase): RitualDao =
        database.ritualDao()

    @Provides
    fun provideAchievementDao(database: SerenityDatabase): AchievementDao =
        database.achievementDao()
}
```

- [ ] **Step 2: Add AchievementRepository binding to RepositoryModule**

Add one binding at the bottom of the abstract class (before the closing brace):

```kotlin
    @Binds
    abstract fun bindAchievementRepository(impl: com.serenity.app.data.repository.AchievementRepositoryImpl): com.serenity.app.domain.repository.AchievementRepository
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/di/DatabaseModule.kt \
        app/src/main/java/com/serenity/app/di/RepositoryModule.kt
git commit -m "feat: wire AchievementDao and AchievementRepository in DI modules"
```

---

## Task 7: CheckAndUnlockAchievementsUseCase

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/usecase/CheckAndUnlockAchievementsUseCase.kt`

- [ ] **Step 1: Create the use case**

```kotlin
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
            "score_avg_80"       to (weekAvg != null && weekAvg!! >= 80),
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

        // Streak milestone bonus XP (awarded once per milestone, same as unlock)
        val streakMilestones = mapOf("streak_7" to 7, "streak_30" to 30, "streak_100" to 100, "streak_365" to 365)
        for ((id, threshold) in streakMilestones) {
            if (streak == threshold) { // exact hit = today crossed the milestone
                xpEarned += 20
            }
        }

        achievementRepository.addXp(xpEarned)
    }
}
```

- [ ] **Step 2: Add the new count methods to RitualRepository interface**

Open `app/src/main/java/com/serenity/app/domain/repository/RitualRepository.kt` and add these to the interface:

```kotlin
    suspend fun countPerfectScores(): Int
    suspend fun countNonNullSleep(): Int
    suspend fun countNonNullWater(): Int
    suspend fun countNonNullGratitude(): Int
    suspend fun countBreathingCompleted(): Int
```

- [ ] **Step 3: Implement the new methods in RitualRepositoryImpl**

Open `app/src/main/java/com/serenity/app/data/repository/RitualRepositoryImpl.kt` and add the implementations (delegating straight to `ritualDao`):

```kotlin
    override suspend fun countPerfectScores(): Int = ritualDao.countPerfectScores()
    override suspend fun countNonNullSleep(): Int = ritualDao.countNonNullSleep()
    override suspend fun countNonNullWater(): Int = ritualDao.countNonNullWater()
    override suspend fun countNonNullGratitude(): Int = ritualDao.countNonNullGratitude()
    override suspend fun countBreathingCompleted(): Int = ritualDao.countBreathingCompleted()
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/CheckAndUnlockAchievementsUseCase.kt \
        app/src/main/java/com/serenity/app/domain/repository/RitualRepository.kt \
        app/src/main/java/com/serenity/app/data/repository/RitualRepositoryImpl.kt
git commit -m "feat: add CheckAndUnlockAchievementsUseCase and count queries on RitualRepository"
```

---

## Task 8: Write Tests for CheckAndUnlockAchievementsUseCase

**Files:**
- Create: `app/src/test/java/com/serenity/app/domain/usecase/CheckAndUnlockAchievementsUseCaseTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.AchievementRepository
import com.serenity.app.domain.repository.RitualRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
```

- [ ] **Step 2: Run tests — expect FAIL (use case not yet hooked up)**

```bash
cd app && ../gradlew test --tests "com.serenity.app.domain.usecase.CheckAndUnlockAchievementsUseCaseTest" 2>&1 | tail -20
```

Expected: tests compile and run (fakes satisfy interface). If compile errors appear, fix them before continuing.

- [ ] **Step 3: Run tests — expect PASS**

```bash
cd app && ../gradlew test --tests "com.serenity.app.domain.usecase.CheckAndUnlockAchievementsUseCaseTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/serenity/app/domain/usecase/CheckAndUnlockAchievementsUseCaseTest.kt
git commit -m "test: add CheckAndUnlockAchievementsUseCase unit tests with fakes"
```

---

## Task 9: GetUserProgressUseCase + Tests

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/usecase/GetUserProgressUseCase.kt`
- Create: `app/src/test/java/com/serenity/app/domain/usecase/GetUserProgressUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.repository.AchievementRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class GetUserProgressUseCaseTest {

    private fun useCase(xp: Int, unlockedIds: Set<String> = emptySet()) =
        GetUserProgressUseCase(object : AchievementRepository {
            override suspend fun getUnlockedIds() = unlockedIds
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
```

Add `import org.junit.Assert.assertTrue` at the top of the imports.

- [ ] **Step 2: Run test — expect FAIL**

```bash
cd app && ../gradlew test --tests "com.serenity.app.domain.usecase.GetUserProgressUseCaseTest" 2>&1 | tail -10
```

Expected: compile error — `GetUserProgressUseCase` not found.

- [ ] **Step 3: Implement GetUserProgressUseCase**

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.UserProgress
import com.serenity.app.domain.repository.AchievementRepository
import java.time.Instant
import javax.inject.Inject

class GetUserProgressUseCase @Inject constructor(
    private val achievementRepository: AchievementRepository
) {

    suspend operator fun invoke(): UserProgress {
        val totalXp = achievementRepository.getTotalXp()
        val unlockedIds = achievementRepository.getUnlockedIds()

        val level = levelFor(totalXp)
        val levelThreshold = LEVELS[level - 1].second
        val nextThreshold = LEVELS.getOrNull(level)?.second

        val achievements = AchievementCatalogue.all.map { template ->
            val unlockedAt: Instant? = if (template.id in unlockedIds) Instant.now() else null
            template.copy(unlockedAt = unlockedAt)
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
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd app && ../gradlew test --tests "com.serenity.app.domain.usecase.GetUserProgressUseCaseTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/GetUserProgressUseCase.kt \
        app/src/test/java/com/serenity/app/domain/usecase/GetUserProgressUseCaseTest.kt
git commit -m "feat: add GetUserProgressUseCase with level computation and tests"
```

---

## Task 10: Hook CheckAndUnlockAchievementsUseCase into CompleteRitualUseCase

**Files:**
- Modify: `app/src/main/java/com/serenity/app/domain/usecase/CompleteRitualUseCase.kt`

- [ ] **Step 1: Update CompleteRitualUseCase**

Replace the full file content:

```kotlin
package com.serenity.app.domain.usecase

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import com.serenity.app.widget.SerenityWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CompleteRitualUseCase @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val calculateWellnessScoreUseCase: CalculateWellnessScoreUseCase,
    private val checkAndUnlockAchievementsUseCase: CheckAndUnlockAchievementsUseCase,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(
        mood: Int?,
        sleepHours: Float?,
        waterGlasses: Int?,
        breathingCompleted: Boolean?,
        gratitudeNote: String?
    ) {
        val score = calculateWellnessScoreUseCase(
            mood = mood,
            sleepHours = sleepHours,
            waterGlasses = waterGlasses,
            breathingCompleted = breathingCompleted,
            gratitudeNote = gratitudeNote
        )

        val ritual = DailyRitual(
            date = LocalDate.now(),
            mood = mood,
            sleepHours = sleepHours,
            waterGlasses = waterGlasses,
            breathingCompleted = breathingCompleted,
            gratitudeNote = gratitudeNote,
            wellnessScore = score,
            createdAt = Instant.now()
        )

        ritualRepository.saveRitual(ritual)

        // Fire-and-forget: achievement failure must not block ritual save
        runCatching {
            checkAndUnlockAchievementsUseCase(wellnessScore = score)
        }

        // Trigger widget refresh
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SerenityWidgetReceiver::class.java)
            )
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        component = ComponentName(context, SerenityWidgetReceiver::class.java)
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/CompleteRitualUseCase.kt
git commit -m "feat: call CheckAndUnlockAchievementsUseCase from CompleteRitualUseCase"
```

---

## Task 11: ProfileViewModel

**Files:**
- Create: `app/src/main/java/com/serenity/app/ui/profile/ProfileViewModel.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.serenity.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.UserProgress
import com.serenity.app.domain.usecase.GetUserProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val progress: UserProgress) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProgressUseCase: GetUserProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    fun loadProgress() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            runCatching { getUserProgressUseCase() }
                .onSuccess { _uiState.value = ProfileUiState.Success(it) }
                .onFailure { _uiState.value = ProfileUiState.Error(it.message ?: "Unknown error") }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/profile/ProfileViewModel.kt
git commit -m "feat: add ProfileViewModel with Loading/Success/Error states"
```

---

## Task 12: ProfileScreen

**Files:**
- Create: `app/src/main/java/com/serenity/app/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.serenity.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.AchievementCategory
import com.serenity.app.domain.model.UserProgress

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message)
        }
        is ProfileUiState.Success -> ProfileScreenContent(progress = state.progress)
    }
}

@Composable
internal fun ProfileScreenContent(progress: UserProgress) {
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        LevelHeader(progress = progress)
        QuickStatsRow(progress = progress)
        Spacer(Modifier.height(16.dp))
        AchievementCategory.entries.forEach { category ->
            BadgeSection(
                category = category,
                achievements = progress.achievements.filter { it.category == category },
                onBadgeTapped = { selectedAchievement = it }
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    selectedAchievement?.let { achievement ->
        BadgeDetailSheet(
            achievement = achievement,
            onDismiss = { selectedAchievement = null }
        )
    }
}

@Composable
private fun LevelHeader(progress: UserProgress) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(primary, secondary)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(progress.levelEmoji, fontSize = 48.sp)
            Text(
                text = progress.levelName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Level ${progress.level}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (progress.xpRequiredForNextLevel == null) 1f
                    else progress.xpIntoCurrentLevel.toFloat() / (progress.xpIntoCurrentLevel + progress.xpRequiredForNextLevel)
                },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(4.dp))
            val xpText = if (progress.xpRequiredForNextLevel == null) {
                "${progress.totalXP} XP · Max Level"
            } else {
                "${progress.xpIntoCurrentLevel} / ${progress.xpIntoCurrentLevel + progress.xpRequiredForNextLevel} XP"
            }
            Text(
                text = xpText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun QuickStatsRow(progress: UserProgress) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCell(label = "TOTAL XP", value = "${progress.totalXP}")
        StatCell(label = "LEVEL", value = "${progress.level}")
        StatCell(
            label = "BADGES",
            value = "${progress.achievements.count { it.unlockedAt != null }} / ${progress.achievements.size}"
        )
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BadgeSection(
    category: AchievementCategory,
    achievements: List<Achievement>,
    onBadgeTapped: (Achievement) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { achievement ->
                BadgeItem(achievement = achievement, onTap = { onBadgeTapped(achievement) })
            }
        }
    }
}

@Composable
private fun BadgeItem(achievement: Achievement, onTap: () -> Unit) {
    val isUnlocked = achievement.unlockedAt != null
    Column(
        modifier = Modifier
            .width(72.dp)
            .alpha(if (isUnlocked) 1f else 0.3f)
            .clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(achievement.emoji, fontSize = 28.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeDetailSheet(achievement: Achievement, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(achievement.emoji, fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(achievement.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(achievement.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("+${achievement.xpReward} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (achievement.unlockedAt != null) {
                Text("Unlocked!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Keep going to unlock this", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/profile/ProfileScreen.kt
git commit -m "feat: add ProfileScreen with LevelHeader, QuickStatsRow, BadgeSection, and BadgeDetailSheet"
```

---

## Task 13: Navigation — Add Profile Route + Bottom Nav

**Files:**
- Modify: `app/src/main/java/com/serenity/app/ui/navigation/Routes.kt`
- Modify: `app/src/main/java/com/serenity/app/ui/navigation/SerenityNavGraph.kt`
- Modify: `app/src/main/java/com/serenity/app/ui/MainActivity.kt`

- [ ] **Step 1: Add Profile to Routes.kt**

Replace the file:

```kotlin
package com.serenity.app.ui.navigation

sealed class Routes(val route: String) {
    data object Onboarding : Routes("onboarding")
    data object Home       : Routes("home")
    data object Ritual     : Routes("ritual")
    data object History    : Routes("history")
    data object Settings   : Routes("settings")
    data object Profile    : Routes("profile")
}
```

- [ ] **Step 2: Update SerenityNavGraph.kt to add bottom nav + Profile destination**

Replace the file:

```kotlin
package com.serenity.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.serenity.app.ui.history.HistoryScreen
import com.serenity.app.ui.home.HomeScreen
import com.serenity.app.ui.onboarding.OnboardingScreen
import com.serenity.app.ui.profile.ProfileScreen
import com.serenity.app.ui.ritual.RitualScreen
import com.serenity.app.ui.settings.SettingsScreen

private data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    NavItem(Routes.Home.route,     "Home",    Icons.Default.Home),
    NavItem(Routes.History.route,  "History", Icons.Default.ShowChart),
    NavItem(Routes.Profile.route,  "Profile", Icons.Default.Person),
    NavItem(Routes.Settings.route, "Settings",Icons.Default.Settings),
)

@Composable
fun SerenityNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (isOnboardingCompleted) Routes.Home.route else Routes.Onboarding.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Onboarding.route) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Home.route) {
                HomeScreen(
                    onStartRitual = { navController.navigate(Routes.Ritual.route) },
                    onNavigateToProfile = { navController.navigate(Routes.Profile.route) },
                )
            }
            composable(Routes.Ritual.route) {
                RitualScreen(
                    onNavigateToHome = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(Routes.History.route) {
                            popUpTo(Routes.Home.route)
                        }
                    },
                )
            }
            composable(Routes.History.route) {
                HistoryScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
```

- [ ] **Step 3: Update MainActivity.kt — remove the safeDrawingPadding modifier (Scaffold now handles insets)**

In `MainActivity.kt`, change:
```kotlin
// OLD
SerenityNavGraph(
    navController = navController,
    isOnboardingCompleted = uiState.isOnboardingCompleted,
    modifier = Modifier.safeDrawingPadding(),
)
```
to:
```kotlin
// NEW
SerenityNavGraph(
    navController = navController,
    isOnboardingCompleted = uiState.isOnboardingCompleted,
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/navigation/Routes.kt \
        app/src/main/java/com/serenity/app/ui/navigation/SerenityNavGraph.kt \
        app/src/main/java/com/serenity/app/ui/MainActivity.kt
git commit -m "feat: add bottom navigation bar with Home/History/Profile/Settings"
```

---

## Task 14: Update HomeScreen — Remove Old Nav Buttons, Add Level Pill

**Files:**
- Modify: `app/src/main/java/com/serenity/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/serenity/app/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Add levelName + levelEmoji to HomeViewModel**

In `HomeViewModel.kt`:

1. Add `GetUserProgressUseCase` to the constructor:
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTodayRitualUseCase: GetTodayRitualUseCase,
    private val getStreakUseCase: GetStreakUseCase,
    private val getRandomQuoteUseCase: GetRandomQuoteUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val getUserProgressUseCase: GetUserProgressUseCase,
) : ViewModel() {
```

2. Add fields to `HomeUiState`:
```kotlin
data class HomeUiState(
    val userName: String = "",
    val todayRitual: DailyRitual? = null,
    val streak: Int = 0,
    val quote: WellnessQuote = WellnessQuote("", ""),
    val levelEmoji: String = "🌱",
    val levelName: String = "Seedling",
    val isLoading: Boolean = true,
)
```

3. Add a coroutine in `loadData()` to populate level info:
```kotlin
        viewModelScope.launch {
            runCatching { getUserProgressUseCase() }.onSuccess { progress ->
                _uiState.update { it.copy(levelEmoji = progress.levelEmoji, levelName = progress.levelName) }
            }
        }
```

- [ ] **Step 2: Update HomeScreen.kt**

Find and remove the two `TextButton` / button calls that navigate to History and Settings (they are in a `Row` near the bottom of the scrollable column). Then add a level pill below the wellness score ring.

Locate the section right after the score ring Canvas block and add:

```kotlin
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onNavigateToProfile() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(uiState.levelEmoji, fontSize = 16.sp)
                    Text(
                        uiState.levelName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
```

Also update the `HomeScreen` composable signature — remove `onNavigateToHistory` and `onNavigateToSettings`, add `onNavigateToProfile`:

```kotlin
@Composable
fun HomeScreen(
    onStartRitual: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
```

- [ ] **Step 3: Add missing imports to HomeScreen.kt if needed**

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.sp
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/home/HomeScreen.kt \
        app/src/main/java/com/serenity/app/ui/home/HomeViewModel.kt
git commit -m "feat: add level pill to HomeScreen and remove old nav buttons"
```

---

## Task 15: Build Verification + ProfileScreen UI Test

**Files:**
- Create: `app/src/androidTest/java/com/serenity/app/ui/profile/ProfileScreenTest.kt`

- [ ] **Step 1: Build the project**

```bash
cd app && ../gradlew assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`. Fix any compile errors before continuing.

- [ ] **Step 2: Run all unit tests**

```bash
cd app && ../gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Write ProfileScreen UI test**

```kotlin
package com.serenity.app.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.serenity.app.domain.model.Achievement
import com.serenity.app.domain.model.AchievementCategory
import com.serenity.app.domain.model.UserProgress
import com.serenity.app.ui.theme.SerenityTheme
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleProgress() = UserProgress(
        totalXP = 650,
        level = 3,
        levelName = "Blossom",
        levelEmoji = "🌸",
        xpIntoCurrentLevel = 150,
        xpRequiredForNextLevel = 350,
        achievements = listOf(
            Achievement("streak_7", "Week Warrior", "7-day streak", "🔥", AchievementCategory.STREAK, 20, java.time.Instant.now()),
            Achievement("streak_30", "Monthly Mover", "30-day streak", "🏅", AchievementCategory.STREAK, 20, null),
        )
    )

    @Test
    fun levelNameIsDisplayed() {
        composeTestRule.setContent {
            SerenityTheme { ProfileScreenContent(progress = sampleProgress()) }
        }
        composeTestRule.onNodeWithText("Blossom").assertIsDisplayed()
    }

    @Test
    fun levelNumberIsDisplayed() {
        composeTestRule.setContent {
            SerenityTheme { ProfileScreenContent(progress = sampleProgress()) }
        }
        composeTestRule.onNodeWithText("Level 3").assertIsDisplayed()
    }

    @Test
    fun unlockedBadgeTitleIsDisplayed() {
        composeTestRule.setContent {
            SerenityTheme { ProfileScreenContent(progress = sampleProgress()) }
        }
        composeTestRule.onNodeWithText("Week Warrior").assertIsDisplayed()
    }
}
```

- [ ] **Step 4: Run UI tests**

```bash
cd app && ../gradlew connectedAndroidTest --tests "com.serenity.app.ui.profile.ProfileScreenTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest/java/com/serenity/app/ui/profile/ProfileScreenTest.kt
git commit -m "test: add ProfileScreen Compose UI tests"
```
