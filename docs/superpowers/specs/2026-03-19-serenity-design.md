# Serenity — Holistic Daily Wellness App

## Overview

Serenity is an Android wellness app built as an indefinitely-improvable portfolio project. It centers on a daily guided ritual — a 2-minute morning/evening check-in that tracks mood, sleep, hydration, breathing, and gratitude, distilling them into a daily wellness score.

## Core Concept

A single guided ritual (morning + evening) that takes ~2 minutes. The user flows through a step-by-step pager sequence, and the app produces a daily wellness score (0–100). Over time, trends and streaks emerge.

**Tone principle:** The app never makes the user feel bad. Missed a day? "Welcome back." Skipped a step? Partial credit. Positive reinforcement only.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Clean Architecture (3 layers)
- **DI:** Hilt
- **Local storage:** Room (rituals, scores), DataStore (preferences)
- **Navigation:** Navigation Compose

## Architecture

### Layer Structure

```
UI Layer → Domain Layer ← Data Layer
```

- **UI Layer** — Compose screens + ViewModels
- **Domain Layer** — Pure Kotlin. Models, repository interfaces, use cases. No Android dependencies.
- **Data Layer** — Room DB, DAOs, DataStore, repository implementations

### Project Structure

```
com.serenity.app/
├── data/
│   ├── local/           (RoomDB, DAOs, DataStore)
│   └── repository/      (implements domain interfaces)
├── domain/
│   ├── model/           (DailyRitual, UserPreferences)
│   ├── repository/      (repository interfaces)
│   └── usecase/         (CompleteRitualUseCase, etc.)
├── ui/
│   ├── theme/           (colors, typography, shapes, 3 palettes)
│   ├── navigation/
│   ├── onboarding/
│   ├── home/
│   ├── ritual/
│   ├── history/
│   └── settings/
└── di/                  (Hilt modules)
```

## Data Model

### DailyRitual
- `id`: Long (auto-generated)
- `date`: LocalDate
- `mood`: Int (1-5)
- `sleepHours`: Float
- `waterGlasses`: Int
- `breathingCompleted`: Boolean
- `gratitudeNote`: String? (nullable)
- `wellnessScore`: Int (0-100)
- `createdAt`: Instant

### UserPreferences (DataStore)
- `name`: String
- `ritualTime`: LocalTime
- `darkMode`: Boolean
- `notificationsEnabled`: Boolean
- `theme`: String ("sage", "lavender", "sand")

## Wellness Score Formula

| Component | Weight | Scoring |
|-----------|--------|---------|
| Mood | 30% | Mapped 1-5 → 0-100 |
| Sleep | 30% | 7-9hrs = 100, scales down outside range |
| Water | 20% | 8 glasses = 100, proportional |
| Breathing | 10% | Completed = 100, skipped = 0 |
| Gratitude | 10% | Written = 100, skipped = 0 |

If steps are skipped, weights redistribute proportionally among completed steps.

## Screens

### 1. Onboarding
- Name input, preferred ritual time, notification permission request
- Clean, minimal, 2-3 slides max

### 2. Home — Journal-Style Dashboard
- Warm greeting with user's name and date
- Small wellness score ring in top-right corner
- Daily inspirational quote card
- 2x2 grid of colored stat cards (Mood, Sleep, Water, Streak)
- "Start Ritual" CTA button at bottom

### 3. Ritual Flow — Step-by-Step Pager
Horizontal pager with progress bar at top. One step per screen:

1. **Mood Check** — 5 illustrated faces (Awful → Great), tap to select with scale animation
2. **Sleep** — Circular slider (0-12h in 0.5h increments), moon/stars illustration
3. **Hydration** — 8 water glass icons in grid, tap to fill with water animation
4. **Breathing** — Expanding/contracting circle (4s inhale, 4s hold, 4s exhale), 1 minute, skippable
5. **Gratitude** — Single text field, optional, skip button

**Completion screen:** Wellness score circular progress animation, encouraging message, "See your week" link

Transitions: 300ms crossfade between steps.

### 4. History
- Weekly bar/line chart of wellness scores
- Simple, clean visualization

### 5. Settings
- Notification time picker
- Theme selector (Sage Garden, Lavender Dusk, Warm Sand)
- Dark mode toggle
- About section

## Color Themes

### Sage Garden (Default)
- Primary: `#7c9a7c`
- Secondary: `#a8c4a8`
- Surface: `#f4f7f4`
- On-surface: `#3a4a3a`

### Lavender Dusk
- Primary: `#8b7eaa`
- Secondary: `#b4a8cc`
- Surface: `#f5f3f8`
- On-surface: `#3a3548`

### Warm Sand
- Primary: `#b09a7c`
- Secondary: `#c8b8a4`
- Surface: `#f7f4f0`
- On-surface: `#4a4238`

## Error Handling & Edge Cases

- **No ritual today:** Shows last ritual data with gentle nudge ("Ready for today's check-in?")
- **Skipped steps:** Score recalculates with proportional weights
- **Missed days:** Streak resets, "Welcome back" message (no guilt)
- **First launch:** Empty state with "Let's start your first ritual" prompt
- **Breathing interrupted:** Partial credit for completed portion

## Testing Strategy

- **Unit tests:** Use cases, wellness score calculation, data mapping
- **UI tests:** Compose testing for ritual flow navigation, home screen states
- **Repository tests:** Room operations with in-memory DB
- **Key scenarios:** First launch, complete ritual, partial ritual, streak logic, theme switching

## Future Improvement Roadmap

- **V2:** Mood history graphs, weekly/monthly reports, home screen widget
- **V3:** Medication reminders, custom ritual steps, notification customization
- **V4:** Sleep tracking integration (Health Connect API), step counter
- **V5:** Journaling with prompts, gratitude streak, data export
- **V6:** Social features — share milestones, anonymous community stats
- **V7:** AI insights — pattern detection ("You sleep better on days you drink more water")
- **Beyond:** Wearable integration, Wear OS companion, multi-language support
