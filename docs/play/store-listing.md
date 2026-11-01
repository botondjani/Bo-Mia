# tend on Google Play: listing and forms

Everything to paste into Play Console, in the order Play asks for it.

## App

- **App name** (max 30): `tend: screen time with Bo`
- **Package**: `app.tend` (if Play says it's taken, change `applicationId` in `app/build.gradle.kts` before the first upload; it can't change later)
- **Category**: Productivity (alternative: Health & Fitness)
- **Contact e-mail**: your address
- **Privacy policy URL**: where `docs/privacy/index.html` is published (see "Privacy policy" below)

## Short description (max 80)

```
A little pause before you scroll, and a companion who grows when you live.
```

## Full description (max 4000)

```
We tend to scroll more than we mean to. tend helps you notice, without bans or guilt.

Meet Bo (or Mia), a small companion who lives on your phone.

A BREATH BEFORE YOU SCROLL
Open an app you picked, and Bo pops up for a few seconds. Often that's all it takes. Still want in? Open it for 5 or 15 minutes, or take a one-minute quick look. When the time is up, Bo checks in, kindly.

EARN YOUR MINUTES BY LIVING
You get free minutes every day. Go for a walk, work out, read, learn, sleep well, meet friends, cook, or add your own: each one gives screen time back. Walks, workouts and sleep count by themselves with Health Connect (a smartwatch helps). The rest is a timer, phone put away.

A GARDEN THAT GROWS
Minutes you don't use become a plant at midnight. Week by week, Bo's home grows too. Keep a habit and Bo gets something to wear: a lucky leaf, a sun hat, a nightcap; the garden gets lanterns and a birdhouse.

CALM BY DESIGN
• Evening mode: the pause gets longer and Bo gets sleepy
• Scheduled lock for bedtime or work hours
• A gentle recap at night and a letter on Sunday
• At most three notes a day, never at night
• Widgets with Bo and your minutes
• Light and dark theme

PRIVATE
Everything stays on your phone. No account, no ads, no tracking. tend reads your screen time and, if you connect it, your steps, workouts and sleep from Health Connect, only to give minutes back.

tend needs two permissions to work: usage access (to see which app you open) and display over other apps (to show the pause). Nothing leaves your phone.
```

## Store graphics (in `docs/play/`)

- App icon 512 × 512: `icon-512.png`
- Feature graphic 1024 × 500: `feature-graphic.png`
- Phone screenshots 1080 × 1920: `screenshot-1.png` … (2 to 8)

## App content forms

### Data safety
- Does your app collect or share user data? **No.** (All data is processed and stored only on the device; nothing is sent off it.)
- Is data encrypted in transit? Not applicable (no data leaves the device).
- Can users delete data? Yes: uninstalling or clearing the app's storage deletes everything.

### Content rating (IARC questionnaire)
- Category: Utility / Productivity. No violence, sexuality, language, controlled substances, gambling, user-generated content, location sharing or purchases. Expected rating: Everyone / PEGI 3.

### Target audience
- 13 and over (not designed for children).

### Ads
- No ads.

### App access (for Google's reviewers)
- All features available without an account. To see the pause: finish onboarding, allow the two permissions, then open one of the picked apps (Instagram, TikTok…).

### Foreground service declaration (`specialUse`)
```
tend is a screen-time companion. Its core feature is a short pause screen that appears the moment the user opens an app they chose to limit. To do that it must notice, in real time, which app comes to the foreground (via usage access) and keep counting the minutes used. A foreground service (special use) keeps this monitoring running while the user uses other apps; it shows a persistent, low-priority notification. Without it the system stops the monitor and the pause never appears. The service does no network access and keeps all data on the device.
```
Video for the declaration (30–60 s, screen recording): open tend → show the persistent notification → open Instagram → the pause appears → tap "Not now".

### Health Connect declaration
- Data types: Steps (read), Exercise (read), Sleep (read), Read in background.
- Justification:
```
tend rewards healthy offline activities with screen-time minutes. It reads steps to complete "Walk outside" (4,000 steps), exercise sessions to complete "Workout or run" (30 minutes), and sleep sessions to complete "Sleep well" (7 hours). Background reading lets a finished walk or workout count without opening the app, and tells the user so. Data is read only for these goals, stored only on the device, never shared or used for ads.
```
- Tip: if the review slows the launch, publish the first version without the Health Connect permissions (remove them from the manifest) and add them in an update.

### Sensitive permissions
- **Usage access** (`PACKAGE_USAGE_STATS`): core function, detecting which app is opened and measuring screen time.
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`): core function, showing the pause screen over the app being opened.
- **Exact alarms**: not used (only inexact alarms).
- **Battery optimisation**: the Play build doesn't request the exemption directly; it opens the system list.

## Testing before production

1. Play Console → Testing → **Internal app sharing** or **Internal testing**: upload `app-play-release.aab`, install from the link on your phone. This is exactly what Play users get.
2. **Closed testing** with at least 12 testers for 14 days in a row (required for new personal developer accounts) before you can apply for production.
3. Check the **Pre-launch report** (Play runs the app on real devices) after every upload.

## Building

```bash
# keystore.properties next to settings.gradle.kts (never committed):
#   storeFile=/path/to/tend-upload.jks
#   storePassword=…
#   keyAlias=tend-upload
#   keyPassword=…
./gradlew bundlePlayRelease        # app/build/outputs/bundle/playRelease/app-play-release.aab
./gradlew assembleSideloadRelease  # app/build/outputs/apk/sideload/release/app-sideload-release.apk
```
Raise `versionCode` in `app/build.gradle.kts` before every upload.
