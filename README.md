# HabitStreaks 🔥

Track your habits GitHub-style — every habit gets its own contribution graph.

Born from a real 90-day challenge (10+ GitHub contributions a day). HabitStreaks
tracks any daily habit with streaks and a familiar green-squares heatmap — and for
the coding habit, it doesn't ask you to self-report: it pulls your **real GitHub
contribution data** via the GraphQL API and draws your actual squares.

## Features

- ✅ Create habits with a daily target (e.g. "10 GitHub contributions", "30 min reading")
- 🔥 Streak tracking with proper timezone/midnight handling
- 🟩 A contribution-style heatmap for every habit, drawn on Compose Canvas
- 🐙 GitHub-connected habits: your real contribution graph, fetched live via GraphQL
- 📱 Glance home-screen widget showing today's habits and current streaks
- ⏰ Daily reminder notifications via WorkManager
- 📴 Offline-first: everything lives in Room; GitHub data is cached

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · Ktor Client (GitHub GraphQL) ·
Glance (widget) · WorkManager · DataStore · MVVM + clean-ish layering

## Architecture

```
ui/        Compose screens + ViewModels (state via StateFlow)
domain/    Streak engine + heatmap models — pure Kotlin, fully unit tested
data/      Room database, GitHub GraphQL client, repositories
widget/    Glance widget
```

The streak engine is deliberately isolated from Android: computing streaks across
timezones, DST shifts, and "did I miss midnight?" edge cases is the hard part of a
habit tracker, so it lives in a pure-Kotlin module with exhaustive tests.

## Connecting your GitHub account

The GitHub heatmap needs a personal access token (classic) with **no scopes at all** —
public contribution data only:

1. GitHub → Settings → Developer settings → Personal access tokens → Generate new
2. Select **no scopes**, generate, copy
3. In HabitStreaks: Settings → Connect GitHub → paste token

The token is stored encrypted on-device and is used for exactly one query
(`contributionsCollection`). Nothing is ever written to your account.

## Status

🚧 In active development — see [issues](../../issues) for the roadmap.

## License

[MIT](LICENSE)
