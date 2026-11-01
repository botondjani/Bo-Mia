# tend on Google Play: listing and forms

Everything to paste into Play Console, in the order Play asks for it.

## Positioning

Most screen-time apps sell restriction ("block apps", "limit social media") and compete in Productivity / Tools for the same few keywords. tend is positioned as a **cozy habit pet**: a companion you care for by living well (walks, sleep, reading, friends), where less scrolling is the reward, not the punishment. That puts it next to self-care and habit apps, a different shelf and a different set of searches, and it still catches the screen-time ones through the description.

- **Primary promise:** "Care for Bo by caring for yourself."
- **Shelf:** Health & Fitness (self-care, habits, wellbeing), not Productivity.
- **Keyword clusters** (woven into title, short and full description, not stuffed):
  - Companion / cozy: habit pet, virtual pet, cozy, self care, wellbeing, cute
  - Habits: habit tracker, daily goals, walk, steps, sleep goal, reading timer, focus timer
  - The problem, in people's words: doomscrolling, phone addiction, social media detox, dopamine detox, touch grass
  - The classic ones: screen time, app limits, digital wellbeing
- **Custom store listings later:** Play lets you show a different listing per search intent (e.g. "dopamine detox" vs "habit pet"); worth it once there's traffic to compare.

## App

- **App name** (max 30): `tend: Habit Pet & Screen Time`
- **Package**: `app.tend` (if Play says it's taken, change `applicationId` in `app/build.gradle.kts` before the first upload; it can't change later)
- **Category**: Health & Fitness
- **Tags** (Play Console → Store settings): Habit tracker, Self care, Mindfulness, Digital wellbeing
- **Contact e-mail**: botondjani@gmail.com (same as `support_email` in `app/src/main/res/values/strings.xml`)
- **Privacy policy URL**: where `docs/privacy/index.html` is published

## Short description (max 80)

```
A cozy habit pet: walk, sleep, read, and earn your screen time back.
```

## Full description (max 4000)

```
Meet Bo, a little habit pet who grows when you live your real day.

Go for a walk, sleep well, read, learn, meet friends, cook, or add your own habits. Every one you do makes Bo happy, grows your garden, and earns back screen time. Scroll less without trying harder: tend makes the offline part the fun part.

CARE FOR BO BY CARING FOR YOURSELF
• Daily goals you pick: steps, workouts, sleep, reading, learning, friends, hobbies, tidying, cooking, or your own
• Walks, workouts and sleep count by themselves with Health Connect (great with a smartwatch)
• Focus timers and phone-free timers for everything else
• Bo earns little things for good habits: a lucky leaf, a sun hat, a nightcap. The garden gets bunting, a birdhouse and lanterns

A GARDEN THAT GROWS WITH YOU
Minutes you don't scroll become a plant every night. Week by week, Bo's home grows too. Share your garden with friends.

A KIND PAUSE, NOT A BLOCK
When you open TikTok, Instagram or another app you picked, Bo pops up for a breath. Often that's all it takes to stop doomscrolling. Still want in? Take a quick look, or open it for 5 or 15 minutes; Bo checks in when the time is up. No guilt, no shame.

CALM BY DESIGN
• Evening mode: a sleepy Bo and a longer pause at night, for better sleep
• Scheduled lock for bedtime or work hours
• Morning plan, evening recap, Sunday letter: at most three notes a day
• Home screen widgets, light and dark theme

PRIVATE
Your data stays on your phone. No account, no ads, no tracking. Anonymous usage stats only if you say yes.

For a digital detox that sticks: less screen time, more of your life, and a small friend cheering you on.
```

## Store graphics (in `docs/play/`)

- App icon 512 × 512: `icon-512.png`
- Feature graphic 1024 × 500: `feature-graphic.png`
- Phone screenshots 1080 × 1920: `screenshot-1.png` … (2 to 8)

## App content forms

### Data safety
- Does your app collect or share user data? **Yes, collects** (only the opt-in anonymous usage stats). Shared with third parties: **No** (Supabase is a service provider, not sharing).
- Data types collected: **App activity → App interactions** (e.g. "pause ended in Not now", "activity done"); **App info and performance → Other** (app version, Android version). Not collected: location, personal info, health, app list, messages.
- For each: collected, not shared; **optional** (user can choose); processed ephemerally: no; purpose: **Analytics**.
- Is data encrypted in transit? **Yes** (HTTPS).
- Can users request deletion? **Yes**: turn stats off in Settings, Start over (new random id), or e-mail botondjani@gmail.com.
- Everything else (minutes, answers, Health Connect data) stays on the device.

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
