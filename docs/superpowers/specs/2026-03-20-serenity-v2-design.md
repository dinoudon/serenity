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
Rounded bar chart built with Compose Canvas showing the last 7 days of wellness scores.
- Bar color intensity scales with score: light green (low) → dark green (high)
- Bars use theme primary color family
- Tapping a bar highlights it and opens the Day Detail Sheet

### Summary Stats Row
Three stat chips displayed horizontally below the chart:
- **Avg Score** — average wellness score for the displayed period (e.g. "64 avg")
- **Best Day** — day name + score of the highest scoring day (e.g. "Sunday • 82")
- **Top Habit** — most consistently completed habit across the period (e.g. "💧 Water most consistent")

### Week / Month Toggle
Segmented control at the top of the screen:
- **Week view** — 7 daily bars (default)
- **Month view** — 4 weekly average bars (Mon–Sun blocks)

### Day Detail Bottom Sheet
Slides up when a bar is tapped. Shows full ritual breakdown for that day:
- Mood emoji + label
- Sleep hours
- Water glasses count
- Breathing completed ✓ or ✗
- Gratitude note snippet (if present)
- Wellness score with circular mini indicator

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
- If no ritual completed today: shows yesterday's score with a subtle dot indicator
- Widget color matches user's selected theme

### Technical Implementation
- **Library:** Jetpack Glance (`androidx.glance:glance-appwidget`)
- **`SerenityWidget`** — `GlanceAppWidget` subclass, defines widget content
- **`SerenityWidgetReceiver`** — `AppWidgetReceiver` registered in manifest
- **`SerenityWidgetContent`** — Glance composable rendering the widget UI
- **State:** `GlanceStateDefinition` backed by DataStore — reads latest score, streak, and theme
- **Updates:** `CompleteRitualUseCase` calls `GlanceAppWidgetManager.updateAll()` after saving a ritual

### Manifest & Resources
- `AndroidManifest.xml` — add `<receiver>` for `SerenityWidgetReceiver` with `APPWIDGET_UPDATE` intent filter
- `res/xml/serenity_widget_info.xml` — widget metadata: minWidth/minHeight for 2×2, updatePeriodMillis, preview layout

---

## New Files

```
app/src/main/java/com/serenity/app/
├── domain/usecase/
│   ├── GetWeeklyStatsUseCase.kt       ← avg score, best day, top habit for 7-day range
│   └── GetMonthlyStatsUseCase.kt      ← 4-week summary (weekly averages)
├── ui/history/
│   ├── HistoryScreen.kt               ← rewrite: chart + stats + toggle
│   ├── HistoryViewModel.kt            ← rewrite: week/month state + day detail
│   └── components/
│       ├── WellnessBarChart.kt        ← Canvas bar chart composable
│       ├── SummaryStatsRow.kt         ← avg/best/top habit chips
│       └── DayDetailSheet.kt          ← bottom sheet for selected day
└── widget/
    ├── SerenityWidget.kt              ← GlanceAppWidget
    ├── SerenityWidgetReceiver.kt      ← AppWidgetReceiver
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

### WeeklyStats (domain model)
```kotlin
data class WeeklyStats(
    val averageScore: Int,
    val bestDay: Pair<LocalDate, Int>,   // date + score
    val topHabit: String                  // "mood" | "sleep" | "water" | "breathing" | "gratitude"
)
```

### MonthlyStats (domain model)
```kotlin
data class MonthlyStats(
    val weeklyAverages: List<Pair<LocalDate, Int>>  // week start date + avg score (4 entries)
)
```

---

## Error Handling

- **No data for period:** Show empty state with "No check-ins yet" message, chart area shows flat line at 0
- **Partial week:** Only show bars for days that have data; empty days show a faint placeholder bar
- **Widget with no data:** Show "–" for score, "Start your ritual" text instead of streak
- **Widget theme mismatch:** Falls back to Sage Garden colors if stored theme is unrecognized

---

## Testing

- Unit test `GetWeeklyStatsUseCase` — avg calculation, best day selection, top habit logic
- Unit test `GetMonthlyStatsUseCase` — week grouping and averaging
- Compose UI test for week/month toggle switching
- Compose UI test for bar tap → bottom sheet appearance
