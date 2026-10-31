# tend

*Take care of you. Offline.* A screen-time companion for Android: you get a small free budget each day and earn the rest by doing real things. Bo or Mia, a small blob character, reflects how your day is going.

This is the MVP skeleton described in the concept doc. Native Android, English only, distributed as a side-loaded APK.

## What works

| Part | Where |
|---|---|
| Budget logic: free base, earned minutes that expire each day, manual-credit cap, lock at ≤ 0, +5 s delay on every open, unused minutes go to the garden | `core/Budget.kt` (unit-tested) |
| 3 task types: move (30 min of exercise) and outside (4,000 steps) via Health Connect, reading via a 20 min focus timer | `core/Tasks.kt`, `health/` |
| Health Connect: reads today's steps + exercise minutes (read-only) on app open, every minute while open, and before every pause screen; progress bars and a celebration when a task completes | `health/HealthConnect.kt`, `health/TaskSync.kt` |
| Moods (happy / tired / scrolling / outside) and companion lines | `core/Mood.kt` |
| Monitoring: UsageStats polled every second, opens a delay screen, counts time used, locks at zero, logs every miss | `monitor/MonitorService.kt` |
| Delay screen: "What are you looking for?", 3 answers, "I'll take a walk instead", countdown | `monitor/InterstitialActivity.kt` |
| "Worth it?" (Yes / Kind of / Not really) after sessions of 30 s or more | `monitor/WorthItActivity.kt` |
| Evening recap (from 19:00): minutes got back, tonight's plant, steps / exercise / reading, "Worth it?" summary with late-night pattern, home milestones | `core/Garden.kt`, `ui/Garden.kt` |
| Garden: one plant per day from unused minutes (sprout < 15, tulip < 30, sunflower ≥ 30), planted at midnight; the home grows weekly (lit window + bench in week 2, own tree in week 4) | `core/Garden.kt`, `ui/Garden.kt` |
| Home screen widget: face, minutes left, next task; refreshes on every change | `widget/TendWidget.kt` |
| Onboarding (7 steps, suggested apps, in-context permissions), settings, home, focus timer | `ui/` |
| Brand: Moss/Mint/Coral/Sunny/Sky/Ink/Sage, Fredoka + Nunito (OFL, bundled) | `ui/Theme.kt` |

Logs are written to the app's private storage: `answers.csv` (looking-for and worth-it answers) and `misses.csv` (blocking gaps or opens that weren't caught).

## Not done yet

- **Background Health Connect reads**: data is read only while tend is on screen (home or pause screen). The widget can lag until then.
- **Step source**: Health Connect needs an app that writes steps (Google Fit, Samsung Health, Fitbit, or the phone's built-in step tracking on newer Pixels). Workouts count once a tracker writes an exercise session.
- **Rive**: the character is drawn by `CharacterPainter` (Canvas) as a placeholder until the Rive rig replaces it.
- Weekly patterns view, and an evening reminder notification for the recap.
- Out of MVP scope (from the concept doc): floating character, friends, coins/shop, iOS.

## Build

Requires JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew testDebugUnitTest   # domain logic tests
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk (signed with the debug key, for side-loading)
```

## Test setup on the phone

Install the APK and follow the onboarding: welcome, companion, how it works, apps (common time sinks are pre-selected), budget, then two switches.

- **Display over other apps** comes first. Once it's on, Android lets tend bring itself back to the front, so after the second switch the user returns to the app automatically.
- **Usage access** comes second.
- **"App was denied access"**: Android 13+ blocks these switches for side-loaded apps ("restricted settings"). Fix it once under App info → ⋮ → *Allow restricted settings*. The onboarding detects this and shows the steps. For testers using adb: `adb shell appops set app.tend ACCESS_RESTRICTED_SETTINGS allow`.
- **Health Connect** is connected from the home screen ("Connect Health Connect"). On Android 13 and below the button opens the Play Store to install it first.
- The reading timer keeps running when you leave tend. Opening a paused app from the pause screen ends it.
- No notification permission is requested. The monitor's foreground service runs without it, its notification just stays hidden.
- On Xiaomi, Samsung, Huawei and similar phones, turn off battery optimisation for tend, or the monitor gets killed (this shows up in the miss log).
