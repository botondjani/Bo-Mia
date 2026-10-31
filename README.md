# tend

*Take care of you. Offline.* A screen-time companion for Android: you get a small free budget each day and earn the rest by doing real things. Bo or Mia, a small blob character, reflects how your day is going.

This is the MVP skeleton described in the concept doc. Native Android, English only, distributed as a side-loaded APK.

## What works

| Part | Where |
|---|---|
| Budget logic: free base, earned minutes that expire each day, manual-credit cap, lock at ≤ 0, +5 s delay on every open, unused minutes go to the garden | `core/Budget.kt` (unit-tested) |
| 3 task types: move, outside (automatic), reading (focus timer) | `core/Tasks.kt` |
| Moods (happy / tired / scrolling / outside) and companion lines | `core/Mood.kt` |
| Monitoring: UsageStats polled every second, opens a delay screen, counts time used, locks at zero, logs every miss | `monitor/MonitorService.kt` |
| Delay screen: "What are you looking for?", 3 answers, "I'll take a walk instead", countdown | `monitor/InterstitialActivity.kt` |
| "Worth it?" (Yes / Kind of / Not really) after sessions of 30 s or more | `monitor/WorthItActivity.kt` |
| Home screen widget: face, minutes left, next task; refreshes on every change | `widget/TendWidget.kt` |
| Home, setup (Bo/Mia, budget, permissions, blocked apps), focus timer | `ui/` |
| Brand: Moss/Mint/Coral/Sunny/Sky/Ink/Sage, Fredoka + Nunito (OFL, bundled) | `ui/Theme.kt` |

Logs are written to the app's private storage: `answers.csv` (looking-for and worth-it answers) and `misses.csv` (blocking gaps or opens that weren't caught).

## Not done yet

- **Health Connect**: the `ActivitySource` interface is there, but only a stub (`NotConnectedActivitySource`) implements it, so automatic tasks don't credit yet. Reading (the focus timer) is the only way to earn minutes for now.
- **Rive**: the character is drawn by `CharacterPainter` (Canvas) as a placeholder until the Rive rig replaces it.
- Evening recap screen, weekly patterns view, and the house growing week by week (the garden count is already tracked).
- Out of MVP scope (from the concept doc): floating character, friends, coins/shop, iOS.

## Build

Requires JDK 17+ and the Android SDK (platform 36).

```bash
./gradlew testDebugUnitTest   # domain logic tests
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk (signed with the debug key, for side-loading)
```

## Test setup on the phone

1. Install the APK, open it, pick Bo or Mia, set the budget.
2. Grant **Usage access** and **Display over other apps**. The second one is required: without it Android blocks starting the delay screen from the background.
3. Allow notifications. The persistent notification keeps the monitor alive.
4. Choose the apps to pause, then tap Done.
5. On Xiaomi, Samsung, Huawei and similar phones, turn off battery optimisation for tend, or the monitor gets killed (this shows up in the miss log).
