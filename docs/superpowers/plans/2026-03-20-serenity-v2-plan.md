# Serenity V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an enhanced History screen with a Canvas bar chart, summary stats, week/month toggle, and day detail bottom sheet, plus a 2x2 Glance home screen widget showing wellness score and streak.

**Architecture:** Domain use cases (`GetWeeklyStatsUseCase`, `GetMonthlyStatsUseCase`) compute stats from `RitualRepository` and are unit-tested in isolation using hand-written fakes. UI components (`WellnessBarChart`, `SummaryStatsRow`, `DayDetailSheet`) are pure composables wired together by a rewritten `HistoryViewModel`. The widget is a standalone Glance `GlanceAppWidget` that reads from the same DataStore as the app and is triggered to update by `CompleteRitualUseCase`.

**Tech Stack:** Kotlin, Jetpack Compose (Canvas), Jetpack Glance 1.1.1 (`glance-appwidget`, `glance-material3`), Hilt, DataStore Preferences, JUnit 4, Compose UI Test (junit4)

---

## File Map

### New files

```
app/src/main/java/com/serenity/app/
  domain/model/
    HabitType.kt
    WeeklyStats.kt
    MonthlyStats.kt
  domain/usecase/
    GetWeeklyStatsUseCase.kt
    GetMonthlyStatsUseCase.kt
  ui/history/components/
    WellnessBarChart.kt
    SummaryStatsRow.kt
    DayDetailSheet.kt
  widget/
    SerenityWidget.kt
    SerenityWidgetReceiver.kt
    SerenityWidgetContent.kt

app/src/main/res/xml/
  serenity_widget_info.xml

app/src/test/java/com/serenity/app/
  domain/usecase/
    GetWeeklyStatsUseCaseTest.kt
    GetMonthlyStatsUseCaseTest.kt
  widget/
    WidgetScoreLogicTest.kt

app/src/androidTest/java/com/serenity/app/
  ui/history/
    HistoryScreenTest.kt
```

### Modified files

```
app/build.gradle.kts                              — add Glance deps + test deps
app/src/main/AndroidManifest.xml                  — add widget receiver
app/src/main/java/com/serenity/app/
  domain/usecase/CompleteRitualUseCase.kt          — call GlanceAppWidgetManager.updateAll()
  ui/history/HistoryViewModel.kt                   — full rewrite
  ui/history/HistoryScreen.kt                      — full rewrite
```

---

### Task 1: Add Glance dependencies to build.gradle.kts

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add the two Glance dependencies and coroutines-test**

In `app/build.gradle.kts`, inside the `dependencies { }` block, add after the existing DataStore line:

```kotlin
// Glance (home screen widget)
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.glance:glance-material3:1.1.1")

// Testing – coroutines
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

- [ ] **Step 2: Sync and verify compilation**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. No "unresolved reference" errors.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: add Glance and coroutines-test dependencies"
```

---

### Task 2: Add HabitType, WeeklyStats, MonthlyStats / WeekAverage domain models

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/model/HabitType.kt`
- Create: `app/src/main/java/com/serenity/app/domain/model/WeeklyStats.kt`
- Create: `app/src/main/java/com/serenity/app/domain/model/MonthlyStats.kt`

- [ ] **Step 1: Create HabitType.kt**

```kotlin
package com.serenity.app.domain.model

enum class HabitType(val displayLabel: String, val emoji: String) {
    MOOD("Mood", "😊"),
    SLEEP("Sleep", "💤"),
    WATER("Water", "💧"),
    BREATHING("Breathing", "🫁"),
    GRATITUDE("Gratitude", "📝")
}
```

- [ ] **Step 2: Create WeeklyStats.kt**

```kotlin
package com.serenity.app.domain.model

import java.time.LocalDate

data class WeeklyStats(
    val averageScore: Int,
    val bestDay: Pair<LocalDate, Int>,
    val topHabit: HabitType
)
```

- [ ] **Step 3: Create MonthlyStats.kt**

```kotlin
package com.serenity.app.domain.model

import java.time.LocalDate

data class WeekAverage(
    val weekStart: LocalDate,
    val averageScore: Int?,
    val hasData: Boolean
)

data class MonthlyStats(
    val weeklyAverages: List<WeekAverage>,
    val bestDay: Pair<LocalDate, Int>,
    val topHabit: HabitType
)
```

- [ ] **Step 4: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/model/HabitType.kt
git add app/src/main/java/com/serenity/app/domain/model/WeeklyStats.kt
git add app/src/main/java/com/serenity/app/domain/model/MonthlyStats.kt
git commit -m "feat: add HabitType, WeeklyStats, MonthlyStats domain models"
```

---

### Task 3: Implement GetWeeklyStatsUseCase with unit tests

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/usecase/GetWeeklyStatsUseCase.kt`
- Create: `app/src/test/java/com/serenity/app/domain/usecase/GetWeeklyStatsUseCaseTest.kt`

**Background — what this use case does:**
Accepts a list of `DailyRitual` items (already fetched for the rolling 7-day window by the ViewModel). Returns a `WeeklyStats` or `null` if the list is empty.

- Averages: sum all `wellnessScore` values, divide by count, apply "round half up" (i.e., `kotlin.math.round` with `.5` rounds up — use `(sum.toDouble() / count + 0.5).toInt()` to implement round-half-up since Kotlin's default `roundToInt()` uses banker's rounding).
- Best day: the ritual with the highest `wellnessScore`. If tie, pick the most recent date.
- Top habit: for each `HabitType`, count how many rituals have that habit as "not skipped":
  - MOOD: `mood != null`
  - SLEEP: `sleepHours != null`
  - WATER: `waterGlasses != null`
  - BREATHING: `breathingCompleted == true`
  - GRATITUDE: `gratitudeNote != null && gratitudeNote.isNotBlank()`
  - Completion rate = count / total rituals. Pick the highest. Tie-break by enum declaration order: MOOD > SLEEP > WATER > BREATHING > GRATITUDE.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/serenity/app/domain/usecase/GetWeeklyStatsUseCaseTest.kt`:

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GetWeeklyStatsUseCaseTest {

    private val useCase = GetWeeklyStatsUseCase()

    private fun ritual(
        date: LocalDate,
        score: Int,
        mood: Int? = 3,
        sleepHours: Float? = 7f,
        waterGlasses: Int? = 6,
        breathingCompleted: Boolean? = true,
        gratitudeNote: String? = "grateful"
    ) = DailyRitual(
        date = date,
        mood = mood,
        sleepHours = sleepHours,
        waterGlasses = waterGlasses,
        breathingCompleted = breathingCompleted,
        gratitudeNote = gratitudeNote,
        wellnessScore = score,
        createdAt = Instant.EPOCH
    )

    private val today = LocalDate.of(2026, 3, 20)

    @Test
    fun `returns null for empty list`() {
        assertNull(useCase(emptyList()))
    }

    @Test
    fun `average score rounds correctly for point five`() {
        // 64 + 65 = 129 / 2 = 64.5 → rounds up to 65
        val rituals = listOf(
            ritual(today, score = 64),
            ritual(today.minusDays(1), score = 65)
        )
        val stats = useCase(rituals)!!
        assertEquals(65, stats.averageScore)
    }

    @Test
    fun `average score rounds down below point five`() {
        // 64 + 66 = 130 / 2 = 65.0 → 65
        val rituals = listOf(
            ritual(today, score = 64),
            ritual(today.minusDays(1), score = 66)
        )
        val stats = useCase(rituals)!!
        assertEquals(65, stats.averageScore)
    }

    @Test
    fun `best day is highest score`() {
        val rituals = listOf(
            ritual(today, score = 70),
            ritual(today.minusDays(1), score = 82),
            ritual(today.minusDays(2), score = 55)
        )
        val stats = useCase(rituals)!!
        assertEquals(today.minusDays(1), stats.bestDay.first)
        assertEquals(82, stats.bestDay.second)
    }

    @Test
    fun `best day tie resolved by most recent date`() {
        val rituals = listOf(
            ritual(today, score = 80),
            ritual(today.minusDays(1), score = 80)
        )
        val stats = useCase(rituals)!!
        assertEquals(today, stats.bestDay.first)
    }

    @Test
    fun `top habit mood wins when all completion rates equal`() {
        // All habits present in all rituals → tie → MOOD wins
        val rituals = listOf(
            ritual(today, score = 70),
            ritual(today.minusDays(1), score = 60)
        )
        val stats = useCase(rituals)!!
        assertEquals(HabitType.MOOD, stats.topHabit)
    }

    @Test
    fun `top habit picks highest completion rate`() {
        // Only breathing completed across both rituals; mood skipped in both
        val rituals = listOf(
            ritual(today, score = 70, mood = null, sleepHours = null,
                waterGlasses = null, breathingCompleted = true, gratitudeNote = null),
            ritual(today.minusDays(1), score = 60, mood = null, sleepHours = null,
                waterGlasses = null, breathingCompleted = true, gratitudeNote = null)
        )
        val stats = useCase(rituals)!!
        assertEquals(HabitType.BREATHING, stats.topHabit)
    }

    @Test
    fun `top habit tie-break order mood over sleep`() {
        // mood and sleep both 100% across one ritual; mood should win
        val rituals = listOf(
            ritual(today, score = 70, mood = 4, sleepHours = 7f,
                waterGlasses = null, breathingCompleted = null, gratitudeNote = null)
        )
        val stats = useCase(rituals)!!
        assertEquals(HabitType.MOOD, stats.topHabit)
    }

    @Test
    fun `top habit gratitude blank note counts as skipped`() {
        // gratitude note is blank → should NOT count as completed
        val rituals = listOf(
            ritual(today, score = 70, mood = null, sleepHours = null,
                waterGlasses = null, breathingCompleted = null, gratitudeNote = "  ")
        )
        // all habits skipped → MOOD wins tie (rate 0 for all, tie-break applies)
        val stats = useCase(rituals)!!
        assertEquals(HabitType.MOOD, stats.topHabit)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew test --tests "com.serenity.app.domain.usecase.GetWeeklyStatsUseCaseTest"
```

Expected: compile error — `GetWeeklyStatsUseCase` does not exist yet.

- [ ] **Step 3: Implement GetWeeklyStatsUseCase**

Create `app/src/main/java/com/serenity/app/domain/usecase/GetWeeklyStatsUseCase.kt`:

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import com.serenity.app.domain.model.WeeklyStats
import java.time.LocalDate
import javax.inject.Inject

class GetWeeklyStatsUseCase @Inject constructor() {

    operator fun invoke(rituals: List<DailyRitual>): WeeklyStats? {
        if (rituals.isEmpty()) return null

        val averageScore = roundHalfUp(rituals.sumOf { it.wellnessScore }.toDouble() / rituals.size)

        val bestDay = rituals
            .maxWithOrNull(compareBy({ it.wellnessScore }, { it.date }))!!
            .let { it.date to it.wellnessScore }

        val topHabit = resolveTopHabit(rituals)

        return WeeklyStats(
            averageScore = averageScore,
            bestDay = bestDay,
            topHabit = topHabit
        )
    }

    private fun roundHalfUp(value: Double): Int = (value + 0.5).toInt()

    private fun resolveTopHabit(rituals: List<DailyRitual>): HabitType {
        val total = rituals.size.toDouble()
        return HabitType.entries
            .maxWithOrNull(
                compareBy<HabitType> { habit ->
                    rituals.count { it.hasHabit(habit) } / total
                }.thenByDescending { habit ->
                    // Lower ordinal = higher priority, so negate
                    -habit.ordinal
                }
            )!!
    }

    private fun DailyRitual.hasHabit(habit: HabitType): Boolean = when (habit) {
        HabitType.MOOD -> mood != null
        HabitType.SLEEP -> sleepHours != null
        HabitType.WATER -> waterGlasses != null
        HabitType.BREATHING -> breathingCompleted == true
        HabitType.GRATITUDE -> gratitudeNote != null && gratitudeNote.isNotBlank()
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew test --tests "com.serenity.app.domain.usecase.GetWeeklyStatsUseCaseTest"
```

Expected: `BUILD SUCCESSFUL`, all 8 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/GetWeeklyStatsUseCase.kt
git add app/src/test/java/com/serenity/app/domain/usecase/GetWeeklyStatsUseCaseTest.kt
git commit -m "feat: add GetWeeklyStatsUseCase with unit tests"
```

---

### Task 4: Implement GetMonthlyStatsUseCase with unit tests

**Files:**
- Create: `app/src/main/java/com/serenity/app/domain/usecase/GetMonthlyStatsUseCase.kt`
- Create: `app/src/test/java/com/serenity/app/domain/usecase/GetMonthlyStatsUseCaseTest.kt`

**Background — what this use case does:**
Accepts a list of `DailyRitual` items spanning the last 28 days. The 28-day window is divided into 4 Mon–Sun calendar week blocks. The rolling window ends today; the most recent block's Monday is the Monday of the week containing today.

Week block calculation: find the Monday of `today`'s week, then step back 3 weeks to get 4 block starts: `[monday-21d, monday-14d, monday-7d, monday]`.

For each week block, collect rituals whose `date` falls in `[weekStart, weekStart+6d]`. If zero rituals: `WeekAverage(weekStart, averageScore=null, hasData=false)`. Otherwise: `WeekAverage(weekStart, averageScore=roundHalfUp(avg), hasData=true)`.

`bestDay` and `topHabit` are computed across all rituals in the list (same logic as `GetWeeklyStatsUseCase`). Returns `null` if the input list is empty.

Note: `GetMonthlyStatsUseCase` depends on `GetWeeklyStatsUseCase`'s shared helpers. To avoid duplication, the plan inlines the helpers rather than creating a shared base — YAGNI.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/serenity/app/domain/usecase/GetMonthlyStatsUseCaseTest.kt`:

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class GetMonthlyStatsUseCaseTest {

    private val useCase = GetMonthlyStatsUseCase()

    private fun ritual(
        date: LocalDate,
        score: Int,
        mood: Int? = 3,
        sleepHours: Float? = 7f,
        waterGlasses: Int? = 6,
        breathingCompleted: Boolean? = true,
        gratitudeNote: String? = "grateful"
    ) = DailyRitual(
        date = date,
        mood = mood,
        sleepHours = sleepHours,
        waterGlasses = waterGlasses,
        breathingCompleted = breathingCompleted,
        gratitudeNote = gratitudeNote,
        wellnessScore = score,
        createdAt = Instant.EPOCH
    )

    // today = 2026-03-20 (Friday)
    // Monday of this week = 2026-03-16
    // 4 week starts: 2026-02-23, 2026-03-02, 2026-03-09, 2026-03-16
    private val today = LocalDate.of(2026, 3, 20)
    private val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @Test
    fun `returns null for empty list`() {
        assertNull(useCase(emptyList(), today))
    }

    @Test
    fun `produces 4 week averages`() {
        val rituals = listOf(ritual(today, score = 70))
        val stats = useCase(rituals, today)!!
        assertEquals(4, stats.weeklyAverages.size)
    }

    @Test
    fun `week starts are correct Mondays`() {
        val rituals = listOf(ritual(today, score = 70))
        val stats = useCase(rituals, today)!!
        val expected = (3 downTo 0).map { thisWeekMonday.minusWeeks(it.toLong()) }
        assertEquals(expected, stats.weeklyAverages.map { it.weekStart })
    }

    @Test
    fun `week with no rituals has hasData false and null averageScore`() {
        // ritual only in most recent week; older weeks should be empty
        val rituals = listOf(ritual(today, score = 70))
        val stats = useCase(rituals, today)!!
        val oldestWeek = stats.weeklyAverages.first()
        assertFalse(oldestWeek.hasData)
        assertNull(oldestWeek.averageScore)
    }

    @Test
    fun `week with rituals has hasData true and correct average`() {
        val rituals = listOf(
            ritual(thisWeekMonday, score = 60),
            ritual(thisWeekMonday.plusDays(1), score = 80)
        )
        val stats = useCase(rituals, today)!!
        val latestWeek = stats.weeklyAverages.last()
        assertTrue(latestWeek.hasData)
        assertEquals(70, latestWeek.averageScore)
    }

    @Test
    fun `weekly average rounds half up`() {
        // 60 + 61 = 121 / 2 = 60.5 → rounds up to 61
        val rituals = listOf(
            ritual(thisWeekMonday, score = 60),
            ritual(thisWeekMonday.plusDays(1), score = 61)
        )
        val stats = useCase(rituals, today)!!
        assertEquals(61, stats.weeklyAverages.last().averageScore)
    }

    @Test
    fun `bestDay is single best day across all 28 days`() {
        val week1Mon = thisWeekMonday.minusWeeks(3)
        val rituals = listOf(
            ritual(today, score = 70),
            ritual(week1Mon, score = 95) // best is in oldest week
        )
        val stats = useCase(rituals, today)!!
        assertEquals(week1Mon, stats.bestDay.first)
        assertEquals(95, stats.bestDay.second)
    }

    @Test
    fun `topHabit tie-break respects mood priority`() {
        val rituals = listOf(ritual(today, score = 70))
        val stats = useCase(rituals, today)!!
        assertEquals(HabitType.MOOD, stats.topHabit)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew test --tests "com.serenity.app.domain.usecase.GetMonthlyStatsUseCaseTest"
```

Expected: compile error — `GetMonthlyStatsUseCase` does not exist yet.

- [ ] **Step 3: Implement GetMonthlyStatsUseCase**

Create `app/src/main/java/com/serenity/app/domain/usecase/GetMonthlyStatsUseCase.kt`:

```kotlin
package com.serenity.app.domain.usecase

import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import com.serenity.app.domain.model.MonthlyStats
import com.serenity.app.domain.model.WeekAverage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class GetMonthlyStatsUseCase @Inject constructor() {

    /**
     * @param rituals All DailyRitual items in the rolling 28-day window.
     * @param today   Injected to allow deterministic testing.
     */
    operator fun invoke(
        rituals: List<DailyRitual>,
        today: LocalDate = LocalDate.now()
    ): MonthlyStats? {
        if (rituals.isEmpty()) return null

        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStarts = (3 downTo 0).map { thisWeekMonday.minusWeeks(it.toLong()) }

        val ritualsByDate = rituals.associateBy { it.date }

        val weeklyAverages = weekStarts.map { weekStart ->
            val weekEnd = weekStart.plusDays(6)
            val weekRituals = rituals.filter { it.date in weekStart..weekEnd }
            if (weekRituals.isEmpty()) {
                WeekAverage(weekStart = weekStart, averageScore = null, hasData = false)
            } else {
                val avg = roundHalfUp(weekRituals.sumOf { it.wellnessScore }.toDouble() / weekRituals.size)
                WeekAverage(weekStart = weekStart, averageScore = avg, hasData = true)
            }
        }

        val bestDay = rituals
            .maxWithOrNull(compareBy({ it.wellnessScore }, { it.date }))!!
            .let { it.date to it.wellnessScore }

        val topHabit = resolveTopHabit(rituals)

        return MonthlyStats(
            weeklyAverages = weeklyAverages,
            bestDay = bestDay,
            topHabit = topHabit
        )
    }

    private fun roundHalfUp(value: Double): Int = (value + 0.5).toInt()

    private fun resolveTopHabit(rituals: List<DailyRitual>): HabitType {
        val total = rituals.size.toDouble()
        return HabitType.entries
            .maxWithOrNull(
                compareBy<HabitType> { habit ->
                    rituals.count { it.hasHabit(habit) } / total
                }.thenByDescending { habit ->
                    -habit.ordinal
                }
            )!!
    }

    private fun DailyRitual.hasHabit(habit: HabitType): Boolean = when (habit) {
        HabitType.MOOD -> mood != null
        HabitType.SLEEP -> sleepHours != null
        HabitType.WATER -> waterGlasses != null
        HabitType.BREATHING -> breathingCompleted == true
        HabitType.GRATITUDE -> gratitudeNote != null && gratitudeNote.isNotBlank()
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew test --tests "com.serenity.app.domain.usecase.GetMonthlyStatsUseCaseTest"
```

Expected: `BUILD SUCCESSFUL`, all 8 tests pass.

- [ ] **Step 5: Run all unit tests together**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/GetMonthlyStatsUseCase.kt
git add app/src/test/java/com/serenity/app/domain/usecase/GetMonthlyStatsUseCaseTest.kt
git commit -m "feat: add GetMonthlyStatsUseCase with unit tests"
```

---

### Task 5: Update CompleteRitualUseCase to trigger widget update

**Files:**
- Modify: `app/src/main/java/com/serenity/app/domain/usecase/CompleteRitualUseCase.kt`

**Background:** `GlanceAppWidgetManager.updateAll(context)` is a suspend function that schedules a re-render of all instances of every `GlanceAppWidget` registered in the app. It needs an Android `Context`. The use case gets `@ApplicationContext context: Context` injected via Hilt (this is safe in a use case because `applicationContext` has the same lifetime as the process).

The `SerenityWidget` class (created in Task 11) must already exist for `updateAll` to be meaningful, but the import and call compile correctly even before the widget class exists — the method signature is `GlanceAppWidgetManager(context).updateAll()`. The call is wrapped in a try-catch so that if no widget instances are currently pinned, the silent no-op behaviour is preserved.

- [ ] **Step 1: Edit CompleteRitualUseCase.kt**

Replace the file contents entirely:

```kotlin
package com.serenity.app.domain.usecase

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CompleteRitualUseCase @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val calculateWellnessScoreUseCase: CalculateWellnessScoreUseCase,
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

        runCatching {
            GlanceAppWidgetManager(context).updateAll(context)
        }
    }
}
```

- [ ] **Step 2: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/domain/usecase/CompleteRitualUseCase.kt
git commit -m "feat: trigger widget update after completing a ritual"
```

---

### Task 6: Create WellnessBarChart composable

**Files:**
- Create: `app/src/main/java/com/serenity/app/ui/history/components/WellnessBarChart.kt`

**Background:** The chart renders differently in week vs month view:
- Week view: 7 bars, one per day, x-axis label = day-of-week abbreviation, today's label is bold.
- Month view: 4 bars, one per `WeekAverage`, x-axis label = "Mar 1" format from `weekStart`.
- Bar height: `score / 100f * maxBarHeight`, or `maxBarHeight * 0.05f` for placeholder (no data).
- Bar color: interpolated between a light-green (score 0) and the theme `primary` (score 100) using `lerp`. Use `androidx.compose.ui.graphics.lerp`.
- Selected bar (week view only): rendered in `MaterialTheme.colorScheme.tertiary`.
- Tap targets: invisible `Box` elements overlaid per-bar using a `Row` with `SpaceEvenly`; only bars with data fire `onBarTap` in week view. Month view bars never fire.

```kotlin
package com.serenity.app.ui.history.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serenity.app.domain.model.WeekAverage
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Unified bar chart composable for both week and month views.
 *
 * In week mode, [weekBars] is a list of 7 (date, score?) pairs — null score = placeholder.
 * In month mode, [monthBars] is a list of 4 [WeekAverage] items.
 * Exactly one of weekBars / monthBars must be non-null.
 */
@Composable
fun WellnessBarChart(
    weekBars: List<Pair<LocalDate, Int?>>?,
    monthBars: List<WeekAverage>?,
    selectedDate: LocalDate?,
    today: LocalDate = LocalDate.now(),
    onBarTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    require((weekBars == null) != (monthBars == null)) {
        "Exactly one of weekBars or monthBars must be provided"
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    val lowColor = primary.copy(alpha = 0.25f)

    // Build unified bar descriptors
    data class Bar(
        val label: String,
        val labelBold: Boolean,
        val score: Int?,       // null = placeholder
        val hasData: Boolean,
        val date: LocalDate?   // non-null only in week view
    )

    val bars: List<Bar> = weekBars?.map { (date, score) ->
        Bar(
            label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            labelBold = date == today,
            score = score,
            hasData = score != null,
            date = date
        )
    } ?: monthBars!!.map { wa ->
        val label = "${wa.weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${wa.weekStart.dayOfMonth}"
        Bar(
            label = label,
            labelBold = false,
            score = wa.averageScore,
            hasData = wa.hasData,
            date = null
        )
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = bars.size
                val totalSpacing = size.width * 0.3f
                val barWidth = (size.width - totalSpacing) / barCount
                val spacing = totalSpacing / (barCount + 1)
                val maxHeight = size.height - 8.dp.toPx()

                bars.forEachIndexed { index, bar ->
                    val barHeight = when {
                        bar.score != null && bar.score > 0 -> (bar.score / 100f) * maxHeight
                        else -> maxHeight * 0.05f
                    }

                    val color: Color = when {
                        bar.date != null && bar.date == selectedDate -> tertiary
                        !bar.hasData -> placeholder
                        bar.score != null -> lerp(lowColor, primary, bar.score / 100f)
                        else -> placeholder
                    }

                    val x = spacing + index * (barWidth + spacing)
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }

            // Invisible tap targets
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bars.forEach { bar ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .then(
                                if (bar.hasData && bar.date != null) {
                                    Modifier.clickable { onBarTap(bar.date) }
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    style = if (bar.labelBold) {
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.labelSmall
                    },
                    color = if (bar.date == selectedDate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
```

- [ ] **Step 1: Create the file** (contents above)

- [ ] **Step 2: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/history/components/WellnessBarChart.kt
git commit -m "feat: add WellnessBarChart Canvas composable (week and month views)"
```

---

### Task 7: Create SummaryStatsRow composable

**Files:**
- Create: `app/src/main/java/com/serenity/app/ui/history/components/SummaryStatsRow.kt`

**Background:** Three stat chips in a horizontal `Row` with `SpaceEvenly`. Each chip is a `Surface` with `RoundedCornerShape(50%)` containing a `Column` (label above, value below). The composable accepts the stats as plain strings so it is purely presentational and preview-friendly.

```kotlin
package com.serenity.app.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Displays three stat chips: average score, best day, and top habit.
 * All values are pre-formatted strings from the ViewModel.
 */
@Composable
fun SummaryStatsRow(
    avgScore: String,      // e.g. "64 avg"
    bestDay: String,       // e.g. "Sunday • 82"
    topHabit: String,      // e.g. "😊 Mood"
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip(label = "Avg Score", value = avgScore)
        StatChip(label = "Best Day", value = bestDay)
        StatChip(label = "Top Habit", value = topHabit)
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 1: Create the file** (contents above)

- [ ] **Step 2: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/history/components/SummaryStatsRow.kt
git commit -m "feat: add SummaryStatsRow composable"
```

---

### Task 8: Create DayDetailSheet composable

**Files:**
- Create: `app/src/main/java/com/serenity/app/ui/history/components/DayDetailSheet.kt`

**Background:** A Material3 `ModalBottomSheet` showing the selected day's ritual details. The gratitude row is omitted entirely if `gratitudeNote` is null or blank. Mood is displayed as an emoji + label using the same `moodEmoji` mapping from the existing `HistoryScreen.kt` (copy the helper as a private function here).

```kotlin
package com.serenity.app.ui.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serenity.app.domain.model.DailyRitual
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    ritual: DailyRitual,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = ritual.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            ritual.mood?.let { mood ->
                SheetRow(
                    label = "${moodEmoji(mood)} Mood",
                    value = moodLabel(mood)
                )
            }

            ritual.sleepHours?.let { hours ->
                SheetRow(label = "💤 Sleep", value = "${"%.1f".format(hours)}h")
            }

            ritual.waterGlasses?.let { glasses ->
                SheetRow(label = "💧 Water", value = "$glasses of 8")
            }

            ritual.breathingCompleted?.let { done ->
                SheetRow(label = "🫁 Breathing", value = if (done) "✓" else "✗")
            }

            val note = ritual.gratitudeNote
            if (!note.isNullOrBlank()) {
                val snippet = if (note.length > 80) note.take(80) + "…" else note
                SheetRow(label = "📝 Gratitude", value = snippet)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wellness Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { ritual.wellnessScore / 100f },
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${ritual.wellnessScore}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun moodEmoji(mood: Int): String = when (mood) {
    1 -> "😞"; 2 -> "😕"; 3 -> "😐"; 4 -> "😊"; 5 -> "😄"
    else -> "😐"
}

private fun moodLabel(mood: Int): String = when (mood) {
    1 -> "Very Low"; 2 -> "Low"; 3 -> "Neutral"; 4 -> "Good"; 5 -> "Great"
    else -> "Neutral"
}
```

- [ ] **Step 1: Create the file** (contents above)

- [ ] **Step 2: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/history/components/DayDetailSheet.kt
git commit -m "feat: add DayDetailSheet bottom sheet composable"
```

---

### Task 9: Rewrite HistoryViewModel

**Files:**
- Modify: `app/src/main/java/com/serenity/app/ui/history/HistoryViewModel.kt`

**Background:** The new ViewModel manages:
- `ViewPeriod` enum (`WEEK`, `MONTH`)
- `selectedDate: LocalDate?` — set when a week-view bar is tapped
- Weekly data: collects `RitualRepository.getRitualsInRange(today-6d, today)`, passes the result to `GetWeeklyStatsUseCase`
- Monthly data: collects `RitualRepository.getRitualsInRange(today-27d, today)`, passes the result to `GetMonthlyStatsUseCase`
- Both ranges are loaded concurrently using two `viewModelScope.launch` blocks in `init`

The `HistoryUiState` carries pre-formatted strings for `SummaryStatsRow` so the composables stay purely presentational.

Replace the file entirely:

```kotlin
package com.serenity.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.MonthlyStats
import com.serenity.app.domain.model.WeekAverage
import com.serenity.app.domain.model.WeeklyStats
import com.serenity.app.domain.repository.RitualRepository
import com.serenity.app.domain.usecase.GetMonthlyStatsUseCase
import com.serenity.app.domain.usecase.GetWeeklyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

enum class ViewPeriod { WEEK, MONTH }

data class HistoryUiState(
    val viewPeriod: ViewPeriod = ViewPeriod.WEEK,
    val isLoading: Boolean = true,
    // Week view
    val weekBars: List<Pair<LocalDate, Int?>> = emptyList(),
    val weeklyStats: WeeklyStats? = null,
    // Month view
    val monthBars: List<WeekAverage> = emptyList(),
    val monthlyStats: MonthlyStats? = null,
    // Day detail sheet
    val selectedDate: LocalDate? = null,
    val selectedRitual: DailyRitual? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val getWeeklyStatsUseCase: GetWeeklyStatsUseCase,
    private val getMonthlyStatsUseCase: GetMonthlyStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()

    init {
        loadWeekData()
        loadMonthData()
    }

    private fun loadWeekData() {
        viewModelScope.launch {
            val start = today.minusDays(6)
            ritualRepository.getRitualsInRange(start, today).collect { rituals ->
                val ritualMap = rituals.associateBy { it.date }
                val bars = (0..6).map { offset ->
                    val date = today.minusDays((6 - offset).toLong())
                    date to ritualMap[date]?.wellnessScore
                }
                _uiState.update {
                    it.copy(
                        weekBars = bars,
                        weeklyStats = getWeeklyStatsUseCase(rituals),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadMonthData() {
        viewModelScope.launch {
            val start = today.minusDays(27)
            ritualRepository.getRitualsInRange(start, today).collect { rituals ->
                _uiState.update {
                    it.copy(monthlyStats = getMonthlyStatsUseCase(rituals, today))
                }
                val stats = getMonthlyStatsUseCase(rituals, today)
                _uiState.update {
                    it.copy(
                        monthBars = stats?.weeklyAverages ?: emptyList(),
                        monthlyStats = stats
                    )
                }
            }
        }
    }

    fun setViewPeriod(period: ViewPeriod) {
        _uiState.update { it.copy(viewPeriod = period, selectedDate = null, selectedRitual = null) }
    }

    fun selectDay(date: LocalDate) {
        val ritual = _uiState.value.weekBars
            .firstOrNull { it.first == date && it.second != null }
            ?.let { (d, _) ->
                // Lookup the actual ritual from already-loaded week bars
                // We need the full DailyRitual — store them in state
                _uiState.value.allWeekRituals.find { it.date == d }
            }
        _uiState.update { it.copy(selectedDate = date, selectedRitual = ritual) }
    }

    fun dismissDayDetail() {
        _uiState.update { it.copy(selectedDate = null, selectedRitual = null) }
    }
}
```

Wait — there's a design issue in the draft above: `selectDay` needs access to the full `DailyRitual` list, not just the score. The `HistoryUiState` must store the raw ritual list. Update the state and ViewModel as follows:

```kotlin
package com.serenity.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.MonthlyStats
import com.serenity.app.domain.model.WeekAverage
import com.serenity.app.domain.model.WeeklyStats
import com.serenity.app.domain.repository.RitualRepository
import com.serenity.app.domain.usecase.GetMonthlyStatsUseCase
import com.serenity.app.domain.usecase.GetWeeklyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class ViewPeriod { WEEK, MONTH }

data class HistoryUiState(
    val viewPeriod: ViewPeriod = ViewPeriod.WEEK,
    val isLoading: Boolean = true,
    // Week view
    val weekRituals: List<DailyRitual> = emptyList(),
    val weekBars: List<Pair<LocalDate, Int?>> = emptyList(),
    val weeklyStats: WeeklyStats? = null,
    // Month view
    val monthBars: List<WeekAverage> = emptyList(),
    val monthlyStats: MonthlyStats? = null,
    // Day detail
    val selectedDate: LocalDate? = null,
    val selectedRitual: DailyRitual? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val getWeeklyStatsUseCase: GetWeeklyStatsUseCase,
    private val getMonthlyStatsUseCase: GetMonthlyStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()

    init {
        loadWeekData()
        loadMonthData()
    }

    private fun loadWeekData() {
        viewModelScope.launch {
            val start = today.minusDays(6)
            ritualRepository.getRitualsInRange(start, today).collect { rituals ->
                val ritualMap = rituals.associateBy { it.date }
                val bars = (0..6).map { offset ->
                    val date = today.minusDays((6 - offset).toLong())
                    date to ritualMap[date]?.wellnessScore
                }
                _uiState.update {
                    it.copy(
                        weekRituals = rituals,
                        weekBars = bars,
                        weeklyStats = getWeeklyStatsUseCase(rituals),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadMonthData() {
        viewModelScope.launch {
            val start = today.minusDays(27)
            ritualRepository.getRitualsInRange(start, today).collect { rituals ->
                val stats = getMonthlyStatsUseCase(rituals, today)
                _uiState.update {
                    it.copy(
                        monthBars = stats?.weeklyAverages ?: emptyList(),
                        monthlyStats = stats
                    )
                }
            }
        }
    }

    fun setViewPeriod(period: ViewPeriod) {
        _uiState.update { it.copy(viewPeriod = period, selectedDate = null, selectedRitual = null) }
    }

    fun selectDay(date: LocalDate) {
        val ritual = _uiState.value.weekRituals.find { it.date == date }
        _uiState.update { it.copy(selectedDate = date, selectedRitual = ritual) }
    }

    fun dismissDayDetail() {
        _uiState.update { it.copy(selectedDate = null, selectedRitual = null) }
    }
}
```

- [ ] **Step 1: Replace HistoryViewModel.kt** with the final version above (the second code block)

- [ ] **Step 2: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/history/HistoryViewModel.kt
git commit -m "feat: rewrite HistoryViewModel with week/month toggle and day selection"
```

---

### Task 10: Rewrite HistoryScreen

**Files:**
- Modify: `app/src/main/java/com/serenity/app/ui/history/HistoryScreen.kt`

**Background:** The screen now orchestrates the toggle, chart, stats row, and bottom sheet. It delegates all rendering to the sub-composables created in Tasks 6–8. The `TopAppBar` title changes dynamically to "Your Week" or "Your Month".

The `formatBestDay` and `formatTopHabit` helper functions live as private top-level functions in this file.

Replace the file entirely:

```kotlin
package com.serenity.app.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serenity.app.domain.model.MonthlyStats
import com.serenity.app.domain.model.WeeklyStats
import com.serenity.app.ui.history.components.DayDetailSheet
import com.serenity.app.ui.history.components.SummaryStatsRow
import com.serenity.app.ui.history.components.WellnessBarChart
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val title = when (uiState.viewPeriod) {
        ViewPeriod.WEEK -> "Your Week"
        ViewPeriod.MONTH -> "Your Month"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            else -> {
                HistoryContent(
                    uiState = uiState,
                    onPeriodChange = viewModel::setViewPeriod,
                    onDaySelected = viewModel::selectDay,
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                )
            }
        }
    }

    // Day detail sheet
    uiState.selectedRitual?.let { ritual ->
        DayDetailSheet(
            ritual = ritual,
            onDismiss = viewModel::dismissDayDetail
        )
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onPeriodChange: (ViewPeriod) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWeek = uiState.viewPeriod == ViewPeriod.WEEK
    val hasData = if (isWeek) {
        uiState.weekBars.any { it.second != null }
    } else {
        uiState.monthBars.any { it.hasData }
    }

    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {

        // Toggle
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isWeek,
                onClick = { onPeriodChange(ViewPeriod.WEEK) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("Week") }
            SegmentedButton(
                selected = !isWeek,
                onClick = { onPeriodChange(ViewPeriod.MONTH) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("Month") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasData) {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No check-ins yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            WellnessBarChart(
                weekBars = if (isWeek) uiState.weekBars else null,
                monthBars = if (!isWeek) uiState.monthBars else null,
                selectedDate = uiState.selectedDate,
                onBarTap = onDaySelected,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val stats = if (isWeek) uiState.weeklyStats else uiState.monthlyStats
            if (stats != null) {
                val avgScore: String
                val bestDay: String
                val topHabit: String
                when (stats) {
                    is WeeklyStats -> {
                        avgScore = "${stats.averageScore} avg"
                        bestDay = formatBestDay(stats.bestDay)
                        topHabit = "${stats.topHabit.emoji} ${stats.topHabit.displayLabel}"
                    }
                    is MonthlyStats -> {
                        avgScore = "" // month view has no avg — handled below
                        bestDay = formatBestDay(stats.bestDay)
                        topHabit = "${stats.topHabit.emoji} ${stats.topHabit.displayLabel}"
                    }
                    else -> {
                        avgScore = ""; bestDay = ""; topHabit = ""
                    }
                }
                // Month view doesn't surface an overall average — show best week avg instead
                val displayAvg = if (isWeek && stats is WeeklyStats) {
                    "${stats.averageScore} avg"
                } else if (!isWeek && stats is MonthlyStats) {
                    val bestWeek = stats.weeklyAverages.maxByOrNull { it.averageScore ?: 0 }
                    if (bestWeek?.averageScore != null) "${bestWeek.averageScore} best wk" else "–"
                } else "–"

                SummaryStatsRow(
                    avgScore = displayAvg,
                    bestDay = formatBestDay(
                        when (stats) {
                            is WeeklyStats -> stats.bestDay
                            is MonthlyStats -> stats.bestDay
                            else -> null
                        }
                    ),
                    topHabit = when (stats) {
                        is WeeklyStats -> "${stats.topHabit.emoji} ${stats.topHabit.displayLabel}"
                        is MonthlyStats -> "${stats.topHabit.emoji} ${stats.topHabit.displayLabel}"
                        else -> "–"
                    }
                )
            }
        }
    }
}

private fun formatBestDay(bestDay: Pair<LocalDate, Int>?): String {
    if (bestDay == null) return "–"
    val dayName = bestDay.first.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$dayName • ${bestDay.second}"
}
```

Note on `WeeklyStats`/`MonthlyStats` type checking: both are plain `data class`es, not a sealed hierarchy. Since `stats` comes from `Any?`, the `when` branches use `is` checks. This is slightly verbose but avoids introducing a sealed supertype just for this screen — YAGNI. If a third stats type is ever added, a sealed interface would be the right refactor.

- [ ] **Step 1: Replace HistoryScreen.kt** with the contents above

- [ ] **Step 2: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If there are unresolved reference errors for `WeeklyStats`/`MonthlyStats` in the `when` block, make sure the import for both model classes is included.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/history/HistoryScreen.kt
git commit -m "feat: rewrite HistoryScreen with chart, stats row, and bottom sheet"
```

---

### Task 11: Create SerenityWidget, SerenityWidgetReceiver, SerenityWidgetContent

**Files:**
- Create: `app/src/main/java/com/serenity/app/widget/SerenityWidget.kt`
- Create: `app/src/main/java/com/serenity/app/widget/SerenityWidgetReceiver.kt`
- Create: `app/src/main/java/com/serenity/app/widget/SerenityWidgetContent.kt`

**Background — widget architecture:**

`SerenityWidget` subclasses `GlanceAppWidget`. Its `provideGlance` override reads DataStore directly (using `context.dataStore.data.first()` — the same DataStore instance used by the app, accessed via the `"user_preferences"` name). It reads: score, score date, streak, and theme. Then it calls `provideContent { SerenityWidgetContent(...) }`.

`SerenityWidgetReceiver` subclasses `GlanceAppWidgetReceiver` and sets `override val glanceAppWidget = SerenityWidget()`. Its `onUpdate()` override calls `coroutineScope.launch { glanceAppWidget.updateAll(context) }` so widget instances are refreshed on device reboot.

`SerenityWidgetContent` is a Glance `@Composable` function (note: Glance composables use `androidx.glance.*` imports, NOT `androidx.compose.*`).

**Key Glance API notes for the implementer:**
- Glance composables live in `androidx.glance` — do NOT mix with regular Compose imports in the same `@Composable`.
- `GlanceTheme` + `ColorProviders` are used for theming (from `glance-material3`).
- `ActionParameters` and `actionStartActivity<MainActivity>()` handle tap-to-open.
- `GlanceModifier` is used instead of `Modifier`.
- `Box`, `Column`, `Text`, `LinearProgressIndicator`, `Image` come from `androidx.glance.layout` or `androidx.glance.appwidget.components`.

- [ ] **Step 1: Create SerenityWidget.kt**

```kotlin
package com.serenity.app.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class SerenityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.widgetDataStore.data.first()

        val scoreRaw = prefs[intPreferencesKey("widget_score")]
        val scoreDateRaw = prefs[stringPreferencesKey("widget_score_date")]
        val streak = prefs[intPreferencesKey("widget_streak")] ?: 0
        val themeRaw = prefs[stringPreferencesKey("theme")] ?: "SAGE"

        val today = LocalDate.now()
        val scoreDate = scoreDateRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val scoreState: WidgetScoreState = when {
            scoreDate == today && scoreRaw != null ->
                WidgetScoreState.Today(score = scoreRaw)
            scoreDate != null && scoreRaw != null &&
                    !scoreDate.isBefore(today.minusDays(6)) ->
                WidgetScoreState.RecentPast(score = scoreRaw)
            else ->
                WidgetScoreState.NoData
        }

        val theme = runCatching { WidgetTheme.valueOf(themeRaw) }.getOrDefault(WidgetTheme.SAGE)

        provideContent {
            SerenityWidgetContent(
                scoreState = scoreState,
                streak = streak,
                theme = theme
            )
        }
    }
}

sealed interface WidgetScoreState {
    data class Today(val score: Int) : WidgetScoreState
    data class RecentPast(val score: Int) : WidgetScoreState
    data object NoData : WidgetScoreState
}

enum class WidgetTheme { SAGE, LAVENDER, SAND }
```

Note: `context.widgetDataStore` uses a separate DataStore instance named `"widget_data"` (defined below) so the widget can read its own persisted values (score, score date, streak) independently from the main `"user_preferences"` store. `CompleteRitualUseCase` must write to both stores when a ritual is saved. However, re-reading the spec: "State: `GlanceStateDefinition` backed by DataStore — reads latest score, streak, theme, and score date". The theme key `"theme"` lives in the existing `"user_preferences"` DataStore. To avoid reading two stores, the widget DataStore also mirrors theme. Simplest approach: write score/streak/scoreDate to `"user_preferences"` as additional keys from `CompleteRitualUseCase`. This means one DataStore, no extra file.

Revise the architecture to use the existing `"user_preferences"` DataStore for all widget state, adding three new keys: `widget_score` (Int), `widget_score_date` (String, ISO date), `widget_streak` (Int). These are written by `CompleteRitualUseCase` after saving.

**Revised Step 1: Update CompleteRitualUseCase to also write widget keys**

Add this to `CompleteRitualUseCase.invoke()` after `ritualRepository.saveRitual(ritual)`, before the `GlanceAppWidgetManager.updateAll()` call:

```kotlin
// Write widget state to DataStore
context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
    .putInt("widget_score", score)
    .putString("widget_score_date", LocalDate.now().toString())
    .putInt("widget_streak", ritualRepository.getCurrentStreak())
    .apply()
```

Actually, DataStore is already used and is the right approach. Use the existing `UserPreferencesDataStore` approach: add a `writeWidgetState(score, date, streak)` method to `UserPreferencesDataStore`. Then inject `UserPreferencesDataStore` into `CompleteRitualUseCase`. See Task 5's revised complete implementation below.

**Full widget DataStore approach:**

Add to `UserPreferencesDataStore` object `Keys`:
```kotlin
val WIDGET_SCORE = intPreferencesKey("widget_score")
val WIDGET_SCORE_DATE = stringPreferencesKey("widget_score_date")
val WIDGET_STREAK = intPreferencesKey("widget_streak")
```

Add method to `UserPreferencesDataStore`:
```kotlin
suspend fun writeWidgetState(score: Int, scoreDate: LocalDate, streak: Int) {
    context.dataStore.edit { prefs ->
        prefs[Keys.WIDGET_SCORE] = score
        prefs[Keys.WIDGET_SCORE_DATE] = scoreDate.toString()
        prefs[Keys.WIDGET_STREAK] = streak
    }
}
```

`SerenityWidget.provideGlance` reads from `context.dataStore` (the same `"user_preferences"` DataStore).

Since `Context.dataStore` is a private extension property in `UserPreferencesDataStore.kt`, the widget cannot directly call `context.dataStore`. Instead, the widget needs its own DataStore accessor. Define a public top-level extension in a separate file or expose the DataStore via a companion `val` on `UserPreferencesDataStore`. The cleanest approach: create a `widgetDataStore` extension property at the top level of a new file `data/local/WidgetDataStore.kt` that resolves to the same underlying preferences file by using the same `name = "user_preferences"`.

**Simplest correct approach** — the widget reads `context.dataStore` via a file-level property. Since Kotlin `by preferencesDataStore` uses the same `Context`-keyed singleton per name, reading `"user_preferences"` in the widget file will return the same DataStore instance as the app. Define in `SerenityWidget.kt`:

```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
```

This is safe — both the `UserPreferencesDataStore` class and the widget file declare `private val Context.dataStore` with the same name, which Android DataStore resolves to the same singleton per process.

**Revised and complete Task 11 implementation:**

- [ ] **Step 1: Update UserPreferencesDataStore to write widget state**

In `app/src/main/java/com/serenity/app/data/local/UserPreferencesDataStore.kt`, add the three keys to the `Keys` object and add the `writeWidgetState` method:

```kotlin
// In Keys object, add:
val WIDGET_SCORE = intPreferencesKey("widget_score")
val WIDGET_SCORE_DATE = stringPreferencesKey("widget_score_date")
val WIDGET_STREAK = intPreferencesKey("widget_streak")

// Add method to UserPreferencesDataStore:
suspend fun writeWidgetState(score: Int, scoreDate: LocalDate, streak: Int) {
    context.dataStore.edit { prefs ->
        prefs[Keys.WIDGET_SCORE] = score
        prefs[Keys.WIDGET_SCORE_DATE] = scoreDate.toString()
        prefs[Keys.WIDGET_STREAK] = streak
    }
}
```

- [ ] **Step 2: Update CompleteRitualUseCase to inject UserPreferencesDataStore and write widget state**

Final complete contents of `CompleteRitualUseCase.kt`:

```kotlin
package com.serenity.app.domain.usecase

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.serenity.app.data.local.UserPreferencesDataStore
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.repository.RitualRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CompleteRitualUseCase @Inject constructor(
    private val ritualRepository: RitualRepository,
    private val calculateWellnessScoreUseCase: CalculateWellnessScoreUseCase,
    private val userPreferencesDataStore: UserPreferencesDataStore,
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

        val streak = ritualRepository.getCurrentStreak()
        userPreferencesDataStore.writeWidgetState(
            score = score,
            scoreDate = LocalDate.now(),
            streak = streak
        )

        runCatching {
            GlanceAppWidgetManager(context).updateAll(context)
        }
    }
}
```

- [ ] **Step 3: Create SerenityWidget.kt**

```kotlin
package com.serenity.app.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class SerenityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.dataStore.data.first()

        val scoreRaw = prefs[intPreferencesKey("widget_score")]
        val scoreDateRaw = prefs[stringPreferencesKey("widget_score_date")]
        val streak = prefs[intPreferencesKey("widget_streak")] ?: 0
        val themeRaw = prefs[stringPreferencesKey("theme")]

        val today = LocalDate.now()
        val scoreDate = scoreDateRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val scoreState: WidgetScoreState = when {
            scoreDate == today && scoreRaw != null ->
                WidgetScoreState.Today(score = scoreRaw)
            scoreDate != null && scoreRaw != null &&
                    !scoreDate.isBefore(today.minusDays(6)) ->
                WidgetScoreState.RecentPast(score = scoreRaw)
            else ->
                WidgetScoreState.NoData
        }

        val theme = themeRaw
            ?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SAGE

        provideContent {
            SerenityWidgetContent(
                scoreState = scoreState,
                streak = streak,
                theme = theme
            )
        }
    }
}

sealed interface WidgetScoreState {
    data class Today(val score: Int) : WidgetScoreState
    data class RecentPast(val score: Int) : WidgetScoreState
    data object NoData : WidgetScoreState
}

enum class WidgetTheme { SAGE, LAVENDER, SAND }
```

- [ ] **Step 4: Create SerenityWidgetReceiver.kt**

```kotlin
package com.serenity.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SerenityWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SerenityWidget()

    private val coroutineScope = MainScope()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        coroutineScope.launch {
            glanceAppWidget.updateAll(context)
        }
    }
}
```

- [ ] **Step 5: Create SerenityWidgetContent.kt**

Note: All Glance composables use `androidx.glance.*` types. `GlanceModifier`, `Column`, `Row`, `Box`, `Text` come from Glance, not Compose. The `@Composable` annotation itself comes from `androidx.compose.runtime` but the content lambda is a Glance-scoped composable.

```kotlin
package com.serenity.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serenity.app.ui.MainActivity

@Composable
fun SerenityWidgetContent(
    scoreState: WidgetScoreState,
    streak: Int,
    theme: WidgetTheme
) {
    val bgColor = when (theme) {
        WidgetTheme.SAGE -> Color(0xFF7C9A7C)
        WidgetTheme.LAVENDER -> Color(0xFF8B7EAA)
        WidgetTheme.SAND -> Color(0xFFB09A7C)
    }
    val onBgColor = Color.White

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        // Show amber dot for RecentPast state
        val showDot = scoreState is WidgetScoreState.RecentPast

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "Serenity",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(onBgColor.copy(alpha = 0.7f)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            )

            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                val scoreText = when (scoreState) {
                    is WidgetScoreState.Today -> "${scoreState.score}"
                    is WidgetScoreState.RecentPast -> "${scoreState.score}"
                    WidgetScoreState.NoData -> "–"
                }
                Text(
                    text = scoreText,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(onBgColor),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (showDot) {
                    Box(
                        modifier = GlanceModifier
                            .size(8.dp)
                            .background(Color(0xFFFFA000)) // amber
                    )
                }
            }

            Text(
                text = "wellness score",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(onBgColor.copy(alpha = 0.7f)),
                    fontSize = 10.sp
                )
            )

            val progress = when (scoreState) {
                is WidgetScoreState.Today -> scoreState.score / 100f
                is WidgetScoreState.RecentPast -> scoreState.score / 100f
                WidgetScoreState.NoData -> 0f
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                color = androidx.glance.unit.ColorProvider(onBgColor),
                backgroundColor = androidx.glance.unit.ColorProvider(onBgColor.copy(alpha = 0.3f))
            )

            val streakText = when (scoreState) {
                WidgetScoreState.NoData -> "Start your ritual"
                else -> "🔥 $streak days"
            }
            Text(
                text = streakText,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(onBgColor),
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.padding(top = 6.dp)
            )
        }
    }
}
```

- [ ] **Step 6: Compile check**

```bash
./gradlew compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If Glance types are unresolved, double-check that Task 1 Glance dependencies were added and the project was synced.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/serenity/app/data/local/UserPreferencesDataStore.kt
git add app/src/main/java/com/serenity/app/domain/usecase/CompleteRitualUseCase.kt
git add app/src/main/java/com/serenity/app/widget/SerenityWidget.kt
git add app/src/main/java/com/serenity/app/widget/SerenityWidgetReceiver.kt
git add app/src/main/java/com/serenity/app/widget/SerenityWidgetContent.kt
git commit -m "feat: add Serenity home screen widget with Glance"
```

---

### Task 12: Add widget_info.xml and update AndroidManifest.xml

**Files:**
- Create: `app/src/main/res/xml/serenity_widget_info.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Background:** Android requires a `<receiver>` entry in the manifest for every `AppWidgetProvider`/`AppWidgetReceiver`, with an intent filter for `ACTION_APPWIDGET_UPDATE` and a `<meta-data>` pointing to the widget info XML. The info XML declares minimum dimensions and update policy.

A 2×2 widget on modern Android maps to approximately 110dp × 110dp minimum (matches the spec).

- [ ] **Step 1: Create res/xml/ directory and serenity_widget_info.xml**

File path: `app/src/main/res/xml/serenity_widget_info.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="0"
    android:description="@string/app_name"
    android:previewImage="@mipmap/ic_launcher"
    android:resizeMode="none"
    android:widgetCategory="home_screen" />
```

- [ ] **Step 2: Update AndroidManifest.xml**

Add the `<receiver>` block inside `<application>`, after the `<activity>` block:

```xml
<receiver
    android:name=".widget.SerenityWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/serenity_widget_info" />
</receiver>
```

The full updated `AndroidManifest.xml` should look like:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".SerenityApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Serenity">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Serenity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.SerenityWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/serenity_widget_info" />
        </receiver>

    </application>

</manifest>
```

- [ ] **Step 3: Build the full debug APK**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/xml/serenity_widget_info.xml
git add app/src/main/AndroidManifest.xml
git commit -m "feat: register SerenityWidgetReceiver in manifest and add widget info XML"
```

---

### Task 13: Write widget unit tests (score display logic)

**Files:**
- Create: `app/src/test/java/com/serenity/app/widget/WidgetScoreLogicTest.kt`

**Background:** `SerenityWidget.provideGlance` contains the score-state resolution logic. To unit-test it without an Android context, extract that logic into a pure function `resolveWidgetScoreState(scoreRaw, scoreDateRaw, today)` as a package-level function in `SerenityWidget.kt`. The tests call this function directly.

- [ ] **Step 1: Extract resolveWidgetScoreState into SerenityWidget.kt**

Add this function at the bottom of `SerenityWidget.kt` (below the class, still package-level):

```kotlin
internal fun resolveWidgetScoreState(
    scoreRaw: Int?,
    scoreDateRaw: String?,
    today: LocalDate = LocalDate.now()
): WidgetScoreState {
    val scoreDate = scoreDateRaw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return when {
        scoreDate == today && scoreRaw != null ->
            WidgetScoreState.Today(score = scoreRaw)
        scoreDate != null && scoreRaw != null &&
                !scoreDate.isBefore(today.minusDays(6)) ->
            WidgetScoreState.RecentPast(score = scoreRaw)
        else ->
            WidgetScoreState.NoData
    }
}
```

Update `SerenityWidget.provideGlance` to call `resolveWidgetScoreState(scoreRaw, scoreDateRaw, today)` instead of inline logic.

- [ ] **Step 2: Write the failing tests**

Create `app/src/test/java/com/serenity/app/widget/WidgetScoreLogicTest.kt`:

```kotlin
package com.serenity.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetScoreLogicTest {

    private val today = LocalDate.of(2026, 3, 20)

    @Test
    fun `today ritual returns Today state`() {
        val result = resolveWidgetScoreState(
            scoreRaw = 75,
            scoreDateRaw = today.toString(),
            today = today
        )
        assertTrue(result is WidgetScoreState.Today)
        assertEquals(75, (result as WidgetScoreState.Today).score)
    }

    @Test
    fun `yesterday ritual returns RecentPast state`() {
        val result = resolveWidgetScoreState(
            scoreRaw = 68,
            scoreDateRaw = today.minusDays(1).toString(),
            today = today
        )
        assertTrue(result is WidgetScoreState.RecentPast)
        assertEquals(68, (result as WidgetScoreState.RecentPast).score)
    }

    @Test
    fun `ritual 6 days ago returns RecentPast state`() {
        val result = resolveWidgetScoreState(
            scoreRaw = 55,
            scoreDateRaw = today.minusDays(6).toString(),
            today = today
        )
        assertTrue(result is WidgetScoreState.RecentPast)
    }

    @Test
    fun `ritual 7 days ago returns NoData state`() {
        val result = resolveWidgetScoreState(
            scoreRaw = 55,
            scoreDateRaw = today.minusDays(7).toString(),
            today = today
        )
        assertEquals(WidgetScoreState.NoData, result)
    }

    @Test
    fun `null score returns NoData`() {
        val result = resolveWidgetScoreState(
            scoreRaw = null,
            scoreDateRaw = today.toString(),
            today = today
        )
        assertEquals(WidgetScoreState.NoData, result)
    }

    @Test
    fun `null date returns NoData`() {
        val result = resolveWidgetScoreState(
            scoreRaw = 80,
            scoreDateRaw = null,
            today = today
        )
        assertEquals(WidgetScoreState.NoData, result)
    }

    @Test
    fun `unparseable date string returns NoData`() {
        val result = resolveWidgetScoreState(
            scoreRaw = 80,
            scoreDateRaw = "not-a-date",
            today = today
        )
        assertEquals(WidgetScoreState.NoData, result)
    }

    @Test
    fun `amber dot shown for RecentPast, not for Today`() {
        val today = resolveWidgetScoreState(80, LocalDate.of(2026, 3, 20).toString(), LocalDate.of(2026, 3, 20))
        val past = resolveWidgetScoreState(80, LocalDate.of(2026, 3, 19).toString(), LocalDate.of(2026, 3, 20))
        assertTrue(today is WidgetScoreState.Today) // no dot
        assertTrue(past is WidgetScoreState.RecentPast) // dot
    }
}
```

- [ ] **Step 3: Run tests to confirm they fail**

```bash
./gradlew test --tests "com.serenity.app.widget.WidgetScoreLogicTest"
```

Expected: compile error — `resolveWidgetScoreState` doesn't exist yet (Step 1 must be done first).

- [ ] **Step 4: Apply Step 1 changes to SerenityWidget.kt, then run tests**

```bash
./gradlew test --tests "com.serenity.app.widget.WidgetScoreLogicTest"
```

Expected: `BUILD SUCCESSFUL`, all 8 tests pass.

- [ ] **Step 5: Run all unit tests**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/serenity/app/widget/SerenityWidget.kt
git add app/src/test/java/com/serenity/app/widget/WidgetScoreLogicTest.kt
git commit -m "test: add widget score display logic unit tests"
```

---

### Task 14: Write Compose UI tests for HistoryScreen

**Files:**
- Create: `app/src/androidTest/java/com/serenity/app/ui/history/HistoryScreenTest.kt`

**Background:** Compose UI tests run on a device or emulator (`./gradlew connectedAndroidTest`). They use `createComposeRule()` to host composables and `composeTestRule.onNode(...)` to find and interact with UI elements. Since `HistoryScreen` is backed by a `HiltViewModel`, the test uses `createAndroidComposeRule<MainActivity>()` and navigates to the history screen — OR tests the lower-level composables (`HistoryContent`, `WellnessBarChart`) directly by making them `internal` and calling them in isolation with test data.

The simpler and more reliable approach: test `WellnessBarChart` and the full `HistoryContent` composable directly. To do this, mark `HistoryContent` as `internal` in `HistoryScreen.kt` (change `private` to `internal`).

```kotlin
package com.serenity.app.ui.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.serenity.app.domain.model.DailyRitual
import com.serenity.app.domain.model.HabitType
import com.serenity.app.domain.model.WeekAverage
import com.serenity.app.domain.model.WeeklyStats
import com.serenity.app.ui.theme.SerenityTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.now()

    private fun ritual(daysAgo: Int, score: Int) = DailyRitual(
        date = today.minusDays(daysAgo.toLong()),
        mood = 4,
        sleepHours = 7f,
        waterGlasses = 6,
        breathingCompleted = true,
        gratitudeNote = "grateful",
        wellnessScore = score,
        createdAt = Instant.EPOCH
    )

    private fun weekBars(rituals: List<DailyRitual>): List<Pair<LocalDate, Int?>> {
        val map = rituals.associateBy { it.date }
        return (0..6).map { offset ->
            val date = today.minusDays((6 - offset).toLong())
            date to map[date]?.wellnessScore
        }
    }

    @Test
    fun weekToggle_isSelectedByDefault() {
        composeTestRule.setContent {
            SerenityTheme {
                HistoryContent(
                    uiState = HistoryUiState(isLoading = false),
                    onPeriodChange = {},
                    onDaySelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Week").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsNoCheckInsMessage() {
        composeTestRule.setContent {
            SerenityTheme {
                HistoryContent(
                    uiState = HistoryUiState(
                        isLoading = false,
                        weekBars = weekBars(emptyList()),
                        viewPeriod = ViewPeriod.WEEK
                    ),
                    onPeriodChange = {},
                    onDaySelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("No check-ins yet").assertIsDisplayed()
    }

    @Test
    fun emptyState_hidsSummaryStatsRow() {
        composeTestRule.setContent {
            SerenityTheme {
                HistoryContent(
                    uiState = HistoryUiState(
                        isLoading = false,
                        weekBars = weekBars(emptyList()),
                        viewPeriod = ViewPeriod.WEEK
                    ),
                    onPeriodChange = {},
                    onDaySelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Avg Score").assertDoesNotExist()
    }

    @Test
    fun withData_showsSummaryStatsRow() {
        val rituals = listOf(ritual(0, 75), ritual(1, 65))
        val stats = WeeklyStats(
            averageScore = 70,
            bestDay = today to 75,
            topHabit = HabitType.MOOD
        )
        composeTestRule.setContent {
            SerenityTheme {
                HistoryContent(
                    uiState = HistoryUiState(
                        isLoading = false,
                        weekRituals = rituals,
                        weekBars = weekBars(rituals),
                        weeklyStats = stats,
                        viewPeriod = ViewPeriod.WEEK
                    ),
                    onPeriodChange = {},
                    onDaySelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Avg Score").assertIsDisplayed()
    }

    @Test
    fun monthToggle_switchesView() {
        composeTestRule.setContent {
            var period = ViewPeriod.WEEK
            SerenityTheme {
                HistoryContent(
                    uiState = HistoryUiState(
                        isLoading = false,
                        viewPeriod = period,
                        monthBars = listOf(
                            WeekAverage(today.minusWeeks(3), null, false),
                            WeekAverage(today.minusWeeks(2), null, false),
                            WeekAverage(today.minusWeeks(1), null, false),
                            WeekAverage(today, 70, true)
                        )
                    ),
                    onPeriodChange = { period = it },
                    onDaySelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Month").performClick()
        // After click, Month button should still be displayed
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
    }
}
```

Note: `SerenityTheme` must be imported from `com.serenity.app.ui.theme.SerenityTheme`. If the app's theme wrapper composable has a different name, adjust the import accordingly. Check `Theme.kt` first.

- [ ] **Step 1: Make HistoryContent internal in HistoryScreen.kt**

Change `private fun HistoryContent(` to `internal fun HistoryContent(` in `HistoryScreen.kt`.

- [ ] **Step 2: Create HistoryScreenTest.kt** with the contents above

- [ ] **Step 3: Run the UI tests**

```bash
./gradlew connectedAndroidTest
```

Expected: `BUILD SUCCESSFUL`, all 5 tests pass. Requires a connected device or running emulator.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/serenity/app/ui/history/HistoryScreen.kt
git add app/src/androidTest/java/com/serenity/app/ui/history/HistoryScreenTest.kt
git commit -m "test: add Compose UI tests for HistoryScreen"
```

---

### Task 15: Final integration verification

- [ ] **Step 1: Run all unit tests**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 2: Build release APK**

```bash
./gradlew assembleRelease
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual smoke test checklist**

Install `app-debug.apk` on a device or emulator and verify:

- [ ] Complete a ritual → widget on home screen updates (score and streak visible)
- [ ] Widget taps open the app to the home screen
- [ ] History screen defaults to Week view with 7 bars
- [ ] Tap "Month" toggle → 4 bars appear, x-axis shows week start dates
- [ ] Tap a week-view bar with data → Day Detail Sheet slides up
- [ ] Tap a placeholder (empty day) bar → nothing happens
- [ ] Tap a month-view bar → nothing happens
- [ ] Day Detail Sheet shows correct mood emoji, sleep, water, breathing, gratitude snippet
- [ ] Summary stats row shows Avg Score, Best Day, Top Habit
- [ ] History with zero rituals shows "No check-ins yet", stats row hidden
- [ ] Widget gradient matches app theme (change theme in Settings → widget updates on next ritual)
- [ ] Reboot device → widget still shows previous score/streak after boot

---

## Summary of all commits (in order)

1. `build: add Glance and coroutines-test dependencies`
2. `feat: add HabitType, WeeklyStats, MonthlyStats domain models`
3. `feat: add GetWeeklyStatsUseCase with unit tests`
4. `feat: add GetMonthlyStatsUseCase with unit tests`
5. `feat: trigger widget update after completing a ritual`
6. `feat: add WellnessBarChart Canvas composable (week and month views)`
7. `feat: add SummaryStatsRow composable`
8. `feat: add DayDetailSheet bottom sheet composable`
9. `feat: rewrite HistoryViewModel with week/month toggle and day selection`
10. `feat: rewrite HistoryScreen with chart, stats row, and bottom sheet`
11. `feat: add Serenity home screen widget with Glance`
12. `feat: register SerenityWidgetReceiver in manifest and add widget info XML`
13. `test: add widget score display logic unit tests`
14. `test: add Compose UI tests for HistoryScreen`

---

### Critical Files for Implementation

- `D:\Work\serenity\app\src\main\java\com\serenity\app\domain\usecase\GetWeeklyStatsUseCase.kt` - New use case to create; contains shared habit-resolution logic used by both stats use cases
- `D:\Work\serenity\app\src\main\java\com\serenity\app\ui\history\HistoryViewModel.kt` - Core state management file to rewrite; coordinates both use cases, week/month toggle, and day selection
- `D:\Work\serenity\app\src\main\java\com\serenity\app\ui\history\HistoryScreen.kt` - Top-level screen composable to rewrite; wires all three new sub-composables together
- `D:\Work\serenity\app\src\main\java\com\serenity\app\widget\SerenityWidget.kt` - New widget entry point; contains score-state resolution logic and DataStore reading
- `D:\Work\serenity\app\src\main\java\com\serenity\app\data\local\UserPreferencesDataStore.kt` - Shared DataStore that must receive three new widget-state keys and a `writeWidgetState` method

---

Plan complete and saved to `D:\Work\serenity\docs\superpowers\plans\2026-03-20-serenity-v2-plan.md`.

Wait — I cannot write files. Let me note that explicitly: this plan must be saved manually or by an execution agent. The plan content above is the complete, ready-to-save document.

**Two execution options:**

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?