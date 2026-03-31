# Serenity

![CI](https://github.com/dinoudon/serenity/actions/workflows/ci.yml/badge.svg)

A holistic daily wellness Android app. Complete a 2-minute guided ritual each day — track mood, sleep, hydration, breathing, and gratitude — and watch your wellness score and streaks grow over time.

## Features

- **Daily Ritual Flow** — Step-by-step pager: mood check (1–5), sleep hours, water intake, breathing exercise, gratitude journal
- **Wellness Score** — Weighted composite score (0–100) calculated from each ritual
- **Streak Tracking** — Consecutive-day streaks with positive reinforcement (no guilt on missed days)
- **History & Analytics** — Weekly/monthly bar charts, day-by-day detail, summary stats
- **Home Screen Widget** — Quick-glance wellness status via Glance AppWidget
- **Theming** — Three Material 3 palettes (Sage Garden, Lavender Dusk, Warm Sand) with dark mode

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Widget | Glance AppWidget |
| Testing | JUnit 4, Compose UI Tests, Coroutines Test |

## Architecture

```
UI Layer  →  Domain Layer  ←  Data Layer
```

- **UI** — Compose screens, ViewModels, theme
- **Domain** — Pure Kotlin models, repository interfaces, use cases
- **Data** — Room DB, DAOs, DataStore, repository implementations

```
com.serenity.app/
├── data/
│   ├── local/          # Room DB, DAOs, DataStore
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # DailyRitual, UserPreferences, Stats
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic
├── ui/
│   ├── home/           # Dashboard with stats & quote
│   ├── ritual/         # 6-step guided ritual flow
│   ├── history/        # Charts & analytics
│   ├── onboarding/     # First-launch setup
│   ├── settings/       # Theme, notifications, preferences
│   ├── navigation/     # NavGraph & routes
│   └── theme/          # Colors, typography, shapes
├── widget/             # Glance home screen widget
└── di/                 # Hilt modules
```

## Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/dinoudon/serenity.git
   ```
2. Open in Android Studio Ladybug or newer
3. Sync Gradle and run on an emulator or device (API 26+)

## Build

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

Requires Android SDK 35 and JDK 17.

## Wellness Score Formula

| Component | Weight | Scoring |
|-----------|--------|---------|
| Mood | 30% | 1–5 mapped to 0–100 |
| Sleep | 30% | 7–9 hrs = 100, scales down outside range |
| Water | 20% | 8 glasses = 100, proportional |
| Breathing | 10% | Completed = 100 |
| Gratitude | 10% | Written = 100 |

Skipped steps redistribute their weight proportionally among completed steps.

## License

This project is for portfolio and educational purposes.
