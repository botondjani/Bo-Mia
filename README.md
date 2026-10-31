# tend

*Take care of you. Offline.* A screen-time companion for Android: you get a small free budget each day and earn the rest by doing real things. Bo or Mia, a small blob character, reflects how your day is going.

This is the MVP skeleton described in the concept doc. Native Android, English only, distributed as a side-loaded APK.

## What works

| Part | Where |
|---|---|
| Budget logic: free base, earned minutes that expire each day, manual-credit cap, lock at ≤ 0, +5 s delay on every open, unused minutes go to the garden | `core/Budget.kt` (unit-tested) |
| Activities the user picks (up to 5, nothing pre-selected): workout (30 min exercise) and walk (4,000 steps) via Health Connect, read/learn via a 20 min focus timer, friends & family / hobby / tidy up / cooking and own activities by check-off (daily check-off cap 45 min) | `core/Tasks.kt`, `ui/ActivityPicker.kt`, `health/` |
| Health Connect: reads today's steps + exercise minutes (read-only) on app open, every minute while open, and before every pause screen; progress bars and a celebration when a task completes | `health/HealthConnect.kt`, `health/TaskSync.kt` |
| Moods (happy / tired / scrolling / outside) and companion lines | `core/Mood.kt` |
| Monitoring: UsageStats polled every second, opens a delay screen, counts time used, locks at zero, logs every miss | `monitor/MonitorService.kt` |
| Pause pop-up: slides up over the opened app (translucent), "What are you looking for?", 3 answers, "I'll take a walk instead", countdown | `monitor/InterstitialActivity.kt` |
| "Worth it?" pop-up over the home screen, three emoji tiles, after sessions of 30 s or more | `monitor/WorthItActivity.kt` |
| Evening recap (from 19:00): minutes got back, tonight's plant, steps / exercise / reading, "Worth it?" summary with late-night pattern, home milestones | `core/Garden.kt`, `ui/Garden.kt` |
| Garden: one plant per day from unused minutes (sprout < 15, tulip < 30, sunflower ≥ 30), planted at midnight; the home grows weekly (lit window + bench in week 2, own tree in week 4) | `core/Garden.kt`, `ui/Garden.kt` |
| Patterns (last 7 days): screen time per day, minutes got back, "Worth it?" by time of day and by app, what you were looking for, one insight sentence | `core/Patterns.kt`, `ui/Patterns.kt` |
| Evening reminder: offered once on the recap, toggle + 20/21/22:00 in settings; inexact daily alarm, silent if the recap was already opened, rescheduled after reboot | `reminder/RecapReminder.kt` |
| Home screen widget (2×1): face and minutes left (or what earns minutes back when out); refreshes on every change | `widget/TendWidget.kt` |
| Onboarding (8 steps incl. "What would you rather do?", suggested apps, in-context permissions), settings, home, focus timer | `ui/` |
| Character: Rive runtime plays `assets/tend_character.riv` (mood/mia/tap inputs, see `docs/rive-rig.md`); without it a drawn fallback that breathes, blinks and glances around | `ui/Character.kt`, `ui/CharacterPainter.kt` |
| Brand: Moss/Mint/Coral/Sunny/Sky/Ink/Sage, Fredoka + Nunito (OFL, bundled) | `ui/Theme.kt` |

Logs are written to the app's private storage: `answers.csv` (looking-for and worth-it answers), `history.csv` (one line per closed day) and `misses.csv` (blocking gaps or opens that weren't caught).

## Not done yet

- **Background Health Connect reads**: data is read only while tend is on screen (home or pause screen). The widget can lag until then.
- **Step source**: Health Connect needs an app that writes steps (Google Fit, Samsung Health, Fitbit, or the phone's built-in step tracking on newer Pixels). Workouts count once a tracker writes an exercise session.
- **Rive rig file**: the runtime is wired in, but `tend_character.riv` still has to be made in the Rive editor (spec: `docs/rive-rig.md`). Until then the drawn character is used.
- Out of MVP scope (from the concept doc): floating character, friends, coins/shop, iOS.

## Build

Requires JDK 17+ and the Android SDK (platform 36). The APK contains ARM native code only (Rive), so it runs on phones but not on x86 emulators.

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
- Notification permission is asked only when you turn on the evening reminder. Until then the monitor's notification stays hidden.
- On Xiaomi, Samsung, Huawei and similar phones, turn off battery optimisation for tend, or the monitor gets killed (this shows up in the miss log).
