# Serenity V2 — Enhanced History & Home Screen Widget

## Overview

V2 adds two improvements to the Serenity wellness app:
1. **Enhanced History Screen** — rounded bar chart, summary stats, week/month toggle, day detail bottom sheet
2. **Home Screen Widget** — small 2×2 Glance widget showing today's wellness score and streak

## New Dependencies

Add to `app/build.gradle.kts`:
```kotlin
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.glance:glance-material3:1.1.1")
```

---

## Feature 1: Enhanced History Screen

### Chart
Rounded bar chart built with Compose Canvas.
- Bar color intensity scales with score: light green (low) → dark green (high), using theme primary color family
- **Week view:** Rolling 7 days ending today (e.g. if today is Thursday, bars span last Thursday → today). X-axis labels are day names (Mon, Tue, … today's day name shown in bold)
- **Month view:** Last 28 days split into 4 Mon–Sun calendar week blocks. X-axis labels are the week start date formatted as "Mar 1", "Mar 8", etc. Week blocks with zero completed rituals show a faint placeholder bar at 5% height (same as empty days in week view). Week blocks with one or more rituals show a bar based on the weekly average score.
- Days with a completed ritual show a full-height bar based on their score
- Days without a completed ritual in week view show a faint placeholder bar at 5% height
- Tapping a bar in **week view** that has ritual data highlights it and opens the Day Detail Sheet
- Tapping a bar in **week view** that has no data (placeholder bar) does nothing
- Tapping a bar in **month view** does nothing (bars represent averages)

### Empty State
- If the selected period has **zero completed rituals**, the chart area shows no bars and a centered "No check-ins yet" message replaces the chart. Summary stats are hidden.
- If the period has **one or more completed rituals**, the chart renders normally with placeholder bars for missing days.

### Summary Stats Row
Three stat chips displayed horizontally below the chart. Stats reflect the currently selected view period. Hidden when in empty state.
- **Avg Score** — average of all wellness scores in the period, rounded to nearest integer (0.5 rounds up). Displayed as "64 avg"
- **Best Day** — the single day with the highest score in the period, shown as day name + score (e.g. "Sunday • 82"). In month view, shows the single best individual day across all 28 days
- **Top Habit** — the habit with the highest completion rate across all rituals in the period. Completion rate = (days habit was not skipped) ÷ (days a ritual was completed). Tie-breaking priority: mood > sleep > water > breathing > gratitude. Displayed using the `HabitType` enum (see Data Model section)

### Week / Month Toggle
Segmented control at the top of the screen:
- **Week view** — rolling last 7 days (default)
- **Month view** — last 28 days grouped into 4 Mon–Sun calendar week blocks

### Completed Ritual Definition
A `DailyRitual` is considered **completed** if its `wellnessScore` field is non-null. Only completed rituals produce tappable bars in the history chart.

### Day Detail Bottom Sheet
Slides up when a completed ritual's bar is tapped in week view. Shows:
- Mood emoji + label
- Sleep hours (e.g. "7.5h")
- Water glasses count (e.g. "6 of 8")
- Breathing completed ✓ or ✗
- Gratitude note snippet — first 80 characters, truncated with "…" if longer; row omitted entirely if no note was entered
- Wellness score with circular mini indicator

### Chart Data Source
`HistoryViewModel` fetches bar data by calling `RitualRepository.getRitualsInRange()` directly (for the list of `DailyRitual` items to render bars). It calls `GetWeeklyStatsUseCase` or `GetMonthlyStatsUseCase` separately for the summary stats row.

---

## Feature 2: Home Screen Widget

### Appearance (2×2 small widget)
- Gradient background using user's selected app theme (Sage/Lavender/Sand)
- App label "Serenity" at top in small light text
- Large wellness score number (light font weight)
- "wellness score" label below number
- Thin progress bar showing score percentage
- Streak count at bottom (e.g. "🔥 5 days")

### Behavior
- Updates automatically when a ritual is completed
- Tapping the widget opens the app to the Home screen
- **Score display logic (in priority order):**
  1. Today has a completed ritual → show today's score, no indicator dot
  2. No ritual today, but one exists within the last 7 days → show the most recent past score with a small amber dot (8dp circle) overlaid at the top-right corner of the score `Text` element using a Glance `Box` with `contentAlignment = Alignment.TopEnd` wrapping the score text
  3. No ritual within the last 7 days → show "–" for score, "Start your ritual" instead of streak, no dot
- Widget color matches user's selected theme. If DataStore has no stored theme key (e.g. widget added before first app launch) or the stored value is unrecognized, defaults to Sage Garden colors

### Reboot Handling
`SerenityWidgetReceiver` overrides `onUpdate()` to re-read DataStore and refresh widget content. This handles the case where Android restores widgets after a device reboot — the widget will re-render with the latest persisted data on the first `onUpdate` call after boot.

### Streak Definition
Streak = number of consecutive calendar days ending today (or yesterday if no ritual today) on which a ritual was completed. This matches V1's streak logic in `RitualRepositoryImpl`.

### Technical Implementation
- **Library:** Jetpack Glance (`androidx.glance:glance-appwidget`)
- **`SerenityWidget`** — `GlanceAppWidget` subclass, defines widget content
- **`SerenityWidgetReceiver`** — `AppWidgetReceiver` registered in manifest; overrides `onUpdate()` for reboot recovery
- **`SerenityWidgetContent`** — Glance composable rendering the widget UI
- **State:** `GlanceStateDefinition` backed by DataStore — reads latest score, streak, theme, and score date
- **Updates:** `CompleteRitualUseCase` calls `GlanceAppWidgetManager.updateAll()` after saving a ritual
- **`updatePeriodMillis` = 0** — no periodic polling; relies on explicit updates from use case and `onUpdate()` for reboot recovery

### Manifest & Resources
- `AndroidManifest.xml` — add `<receiver>` for `SerenityWidgetReceiver` with `APPWIDGET_UPDATE` intent filter
- `res/xml/serenity_widget_info.xml` — widget metadata: minWidth/minHeight 110dp × 110dp, updatePeriodMillis=0

---

## New Files

```
app/src/main/java/com/serenity/app/
├── domain/model/
│   └── HabitType.kt                   ← enum with display label + emoji
├── domain/usecase/
│   ├── GetWeeklyStatsUseCase.kt       ← avg score, best day, top habit for rolling 7-day range
│   └── GetMonthlyStatsUseCase.kt      ← best day, top habit, 4-week averages for rolling 28-day range
├── ui/history/
│   ├── HistoryScreen.kt               ← rewrite: chart + stats + toggle
│   ├── HistoryViewModel.kt            ← rewrite: week/month state + day detail
│   └── components/
│       ├── WellnessBarChart.kt        ← Canvas bar chart composable
│       ├── SummaryStatsRow.kt         ← avg/best/top habit chips
│       └── DayDetailSheet.kt          ← bottom sheet for selected day
└── widget/
    ├── SerenityWidget.kt              ← GlanceAppWidget
    ├── SerenityWidgetReceiver.kt      ← AppWidgetReceiver with onUpdate()
    └── SerenityWidgetContent.kt       ← Glance composable UI

app/src/main/res/xml/
└── serenity_widget_info.xml
```

## Modified Files

- `domain/usecase/CompleteRitualUseCase.kt` — add `GlanceAppWidgetManager.updateAll()` after save
- `app/src/main/AndroidManifest.xml` — add widget receiver registration
- `app/build.gradle.kts` — add Glance dependencies

---

## Data Model Additions

### HabitType (domain model)
```kotlin
enum class HabitType(val displayLabel: String, val emoji: String) {
    MOOD("Mood", "😊"),
    SLEEP("Sleep", "💤"),
    WATER("Water", "💧"),
    BREATHING("Breathing", "🫁"),
    GRATITUDE("Gratitude", "📝")
}
```

### WeeklyStats (domain model)
```kotlin
data class WeeklyStats(
    val averageScore: Int,               // rounded to nearest integer
    val bestDay: Pair<LocalDate, Int>,   // date + score
    val topHabit: HabitType
)
```

### MonthlyStats (domain model)
```kotlin
data class WeekAverage(
    val weekStart: LocalDate,   // Monday of that week
    val averageScore: Int?,     // null if zero rituals in that week block
    val hasData: Boolean        // false if zero rituals; used to render placeholder bar vs. real bar
)

data class MonthlyStats(
    val weeklyAverages: List<WeekAverage>,           // 4 entries, one per Mon–Sun block
    val bestDay: Pair<LocalDate, Int>,               // single best individual day across all 28 days
    val topHabit: HabitType
)
```

---

## Error Handling

- **Zero rituals in selected period:** Hide chart bars, show "No check-ins yet" message, hide stats row
- **Partial week:** Days with data show bars; days without show a faint placeholder bar at 5% height; tapping empty bars does nothing
- **Widget — no ritual within 7 days:** Show "–" for score, "Start your ritual" instead of streak, no dot
- **Widget — no ritual today:** Show most recent past score (within 7 days) with amber dot indicator
- **Widget — no theme in DataStore:** Default to Sage Garden
- **Widget — unrecognized theme value:** Fall back to Sage Garden

---

## Testing

- Unit test `GetWeeklyStatsUseCase` — avg rounding, best day selection, top habit tie-breaking, empty period
- Unit test `GetMonthlyStatsUseCase` — 28-day rolling window, Mon–Sun week grouping, weekly averaging, best day across all 28 days
- Unit test top habit tie-breaking — verify priority order: mood > sleep > water > breathing > gratitude
- Compose UI test — week/month toggle switches chart and stats correctly
- Compose UI test — tapping week bar with data opens Day Detail Sheet
- Compose UI test — tapping month bar does nothing
- Compose UI test — tapping empty-day placeholder bar does nothing
- Compose UI test — zero rituals in period shows empty state, hides stats row
- Widget unit test — score display logic: today data, recent past data (with dot), no data within 7 days
- Widget unit test — amber dot shown when showing yesterday's/past score, hidden for today's score
- Widget unit test — theme fallback: absent DataStore key and unrecognized value both produce Sage Garden
- Widget unit test — `onUpdate()` re-reads DataStore and updates content (reboot recovery)
