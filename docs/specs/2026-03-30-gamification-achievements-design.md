# Gamification & Achievements — Design Spec

**Date:** 2026-03-30
**Status:** Approved

## Overview

Add a visible XP progression system and badge collection to Serenity. Users earn XP for completing rituals and unlock badges tied to streaks, scores, and individual habits. Progress is surfaced through a new Profile screen, accessible via a bottom navigation bar added to the app.

---

## Architecture

Follows the existing Clean Architecture pattern. New code is additive — no existing layers are restructured.

```
UI Layer       ProfileScreen + ProfileViewModel
Domain Layer   Achievement, UserProgress models
               GetUserProgressUseCase
               CheckAndUnlockAchievementsUseCase
Data Layer     AchievementUnlockEntity, UserProgressEntity (new Room tables)
               AchievementRepository + AchievementRepositoryImpl
```

`CompleteRitualUseCase` is the only existing use case modified — it calls `CheckAndUnlockAchievementsUseCase` after saving the ritual. Widget update trigger is unaffected.

`MainActivity` gains a `NavigationBar` with 4 destinations: Home, History, Profile, Settings. The ritual flow stays modal (full-screen), unaffected by the nav bar. The loose History/Settings navigation buttons are removed from `HomeScreen`.

---

## Data Models

### Domain Models

```kotlin
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: AchievementCategory,
    val xpReward: Int,
    val unlockedAt: Instant?   // null = locked
)

enum class AchievementCategory { STREAK, SCORE, HABIT }

data class UserProgress(
    val totalXP: Int,
    val level: Int,
    val levelName: String,
    val levelEmoji: String,
    val xpIntoCurrentLevel: Int,
    val xpRequiredForNextLevel: Int?,  // null at max level
    val achievements: List<Achievement>
)
```

### Level Progression Table

| Level | Name | XP Required |
|---|---|---|
| 1 | 🌱 Seedling | 0 |
| 2 | 🌿 Sprout | 200 |
| 3 | 🌸 Blossom | 500 |
| 4 | 🌳 Grove Keeper | 1000 |
| 5 | 🏔️ Summit Walker | 2000 |
| 6 | 🌙 Serenity Sage | 4000 |

Level is computed from `totalXP` at call time — not stored in the database.

### XP Awards (per ritual completion)

| Event | XP |
|---|---|
| Ritual completed | +10 |
| Score ≥ 80 | +5 bonus |
| Score = 100 | +15 bonus (replaces the ≥80 bonus) |
| Streak milestone hit (7, 30, 100, 365) | +20 bonus |

### Achievement Catalogue

Hardcoded in the domain layer. Only unlock state (achievement ID + timestamp) is persisted in the database.

`UserProgressEntity` schema: `totalXP: Int` (single-row table, upserted on each ritual completion). All other `UserProgress` fields are computed at call time.

New DAO queries required on `RitualDao` to support achievement checking:
- `countNonNullSleep(): Int` — count rituals where `sleepHours IS NOT NULL`
- `countNonNullWater(): Int` — count rituals where `waterGlasses IS NOT NULL`
- `countNonNullGratitude(): Int` — count rituals where `gratitudeNote IS NOT NULL AND gratitudeNote != ''`
- `countBreathingCompleted(): Int` — count rituals where `breathingCompleted = 1`
- `countPerfectScores(): Int` — count rituals where `wellnessScore = 100`

| Category | ID | Trigger |
|---|---|---|
| Streak | `streak_7` | 7-day streak |
| Streak | `streak_30` | 30-day streak |
| Streak | `streak_100` | 100-day streak |
| Streak | `streak_365` | 365-day streak |
| Score | `score_perfect_1` | First score of 100 |
| Score | `score_perfect_5` | 5× score of 100 |
| Score | `score_avg_80` | 7-day average ≥ 80 |
| Habit | `habit_sleep_10` | Sleep logged 10 times |
| Habit | `habit_water_20` | Water logged 20 times |
| Habit | `habit_gratitude_30` | Gratitude logged 30 times |
| Habit | `habit_breathing_50` | Breathing completed 50 times |

---

## Components & Screens

### ProfileScreen

Single scrollable column:

- **LevelHeader** — full-width gradient card using current theme color. Shows level emoji, level name, XP progress bar, XP counts (current / required for next level).
- **QuickStatsRow** — 3-cell row: current streak, total rituals completed, 7-day average score. Reuses existing data sources.
- **BadgeSection** — one section per `AchievementCategory`. Each section has a label header and a lazy horizontal row of `BadgeItem` tiles. Locked badges rendered at 30% opacity with a lock icon overlay.
- **BadgeItem** — tappable. Opens a `ModalBottomSheet` showing title, description, XP reward, and unlock date (or "Keep going to unlock this" if locked).

### Changes to Existing Screens

- **MainActivity** — add `NavigationBar` with Home / History / Profile / Settings items.
- **HomeScreen** — remove History and Settings nav buttons. Add a small level pill (emoji + level name) below the wellness score ring; tapping it navigates to Profile.
- **CompleteRitualUseCase** — call `CheckAndUnlockAchievementsUseCase` after ritual save; accumulate XP into `UserProgressEntity`.

### ProfileViewModel

Exposes `StateFlow<ProfileUiState>` with `Loading`, `Success(UserProgress)`, and `Error` states — same pattern as all other ViewModels in the project.

---

## Data Flow

### On Ritual Completion

```
CompleteRitualUseCase
  → save ritual to Room (existing)
  → CheckAndUnlockAchievementsUseCase
      → query current streak, habit counts, score history
      → compare against achievement catalogue
      → for each newly unlocked achievement:
          → insert unlock record to DB
          → accumulate XP reward
      → add base XP + bonus to UserProgress in DB
  → trigger widget update (existing)
```

### Profile Screen Load

```
ProfileViewModel.init
  → GetUserProgressUseCase
      → read totalXP from DB → compute level
      → read all unlock records → hydrate Achievement list
  → emit ProfileUiState.Success
```

---

## Error Handling

Achievement unlocking is fire-and-forget. A failure inside `CheckAndUnlockAchievementsUseCase` must never block ritual saving. The call is wrapped in `try/catch` inside `CompleteRitualUseCase`; errors are logged silently. The ritual save is the primary action.

---

## Testing

- Unit test `CheckAndUnlockAchievementsUseCase` — cover each achievement trigger condition with a fake repository.
- Unit test `GetUserProgressUseCase` — verify level computation across all XP thresholds.
- Compose UI test for `ProfileScreen` — stateless content composable pattern, same as `HistoryScreen` tests.
- Existing ritual and history tests require no changes.
