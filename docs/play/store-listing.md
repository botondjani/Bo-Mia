# tend on Google Play: listing and forms

Everything to paste into Play Console, in the order Play asks for it.

## Positioning

Most screen-time apps sell restriction ("block apps", "limit social media") and compete in Productivity / Tools for the same few keywords. tend is positioned as a **cozy habit pet**: a companion you care for by living well (walks, sleep, reading, friends), where less scrolling is the reward, not the punishment. That puts it next to self-care and habit apps, a different shelf and a different set of searches, and it still catches the screen-time ones through the description.

- **Primary promise:** "Care for Bo by caring for yourself."
- **Your own camp:** activities earn acorns; spend them on a camp by the river (a fire, a tent, a canoe, string lights). Something new every day, a long goal every few weeks.
- **Shelf:** Health & Fitness (self-care, habits, wellbeing), not Productivity.
- **Keyword clusters** (woven into title, short and full description, not stuffed):
  - Companion / cozy: habit pet, virtual pet, cozy, self care, wellbeing, cute
  - Progress: level up, habit RPG, XP, weekly challenge, self improvement
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

Go for a walk, sleep well, read, learn, meet friends, cook, or add your own habits. Every one you do makes Bo happy, levels you up, and earns back screen time. Scroll less without trying harder: tend makes the offline part the fun part.

CARE FOR BO BY CARING FOR YOURSELF
• Daily goals you pick: steps, workouts, sleep, reading, learning, friends, hobbies, tidying, cooking, or your own
• Walks, workouts and sleep count by themselves with Health Connect (great with a smartwatch)
• Focus timers and phone-free timers for everything else: while one runs, every app waits
• Or just a timer: 25, 50 or 90 minutes of focus, one tap
• Level up Body, Mind and Rest: every activity is XP
• A weekly challenge picked from your habits, with bonus XP
• Bo earns little things for good habits: a lucky leaf, a sun hat, a nightcap

YOUR CAMP BY THE RIVER
Walks, books and every "Not now" earn acorns. Spend them on a camp in a clearing by the river: a campfire, a hammock, a canvas tent, a canoe, string lights between the trees. A little something every day, a cabin or a boat to save for. Share it with friends.

A KIND PAUSE, NOT A BLOCK
When you open TikTok, Instagram or another app you picked, Bo pops up for a breath. Often that's all it takes to stop doomscrolling. Still want in? Take a quick look, or open it for 5 or 15 minutes; Bo checks in when the time is up. No guilt, no shame.

CALM BY DESIGN
• Evening mode: a sleepy Bo and a longer pause at night, for better sleep
• Scheduled lock for bedtime or work hours
• Morning plan, evening recap, Sunday letter: at most three notes a day
• Home screen widgets, light and dark theme

PRIVATE
No ads, no tracking, no account needed. Log in only if you want a backup; your health data never leaves your phone.

For a digital detox that sticks: less screen time, more of your life, and a small friend cheering you on.
```

## Store graphics (in `docs/play/`)

- App icon 512 × 512: `icon-512.png`
- Feature graphic 1024 × 500: `feature-graphic.png`
- Phone screenshots 1080 × 1920: `screenshot-1.png` … (2 to 8)

## App content forms

### Data safety
- Does your app collect or share user data? **Yes, collects.** Shared with third parties: **No** (Supabase and Google sign-in are service providers, not sharing).
- Is data encrypted in transit? **Yes** (HTTPS).
- Can users request deletion? **Yes**: in the app (Settings → Account → Delete account, removes it right away), and by e-mail to botondjani@gmail.com.
- Account creation: **optional**; methods: Google sign-in, e-mail with a one-time code. Account deletion URL: the privacy page's "Your account" section (Play asks for a web link: use the privacy policy URL).

| Data type (Play's names) | Collected | Optional | Purpose | Notes |
| - | - | - | - | - |
| Personal info → Name | Yes | Yes (only with an account) | Account management, App functionality | From Google, or the e-mail's first part |
| Personal info → Email address | Yes | Yes (only with an account) | Account management | Log-in |
| App activity → App interactions | Yes | Yes | Analytics (opt-in stats), App functionality (backup) | Stats: anonymous counts; backup: activities done per day, pauses, acorns, camp |
| App activity → Other user-generated content | Yes | Yes (only with an account) | App functionality | The camp, settings and "Worth it?" answers in the backup |
| App info and performance → Other | Yes | Yes | Analytics | App and Android version with the opt-in stats |
| Location → Approximate location | Yes | Yes (only if a town is typed) | App functionality | The town's name and rounded coordinates go to Open-Meteo for the camp's weather; never the phone's location. Declared to be on the safe side |
| Personal info → Other info | Yes | Yes | Analytics | Age range and gender, if given: with the opt-in stats (anonymous) and in the backup |

- Not collected: the phone's own location, health and fitness (steps, exercise, sleep stay on the phone), the list of installed or used apps (the backup leaves out which apps are paused and their minutes; the stats never name an app), messages, contacts, photos, financial info.
- Data is processed ephemerally: No. Linked to the user's identity: account data and backup yes; opt-in stats no.
- Everything else (minutes, Health Connect readings) stays on the device.

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

### Accessibility API declaration (`ShortsGuard`)
- Is it an accessibility tool? **No** (`isAccessibilityTool` is not set).
- What it's for:
```
Optional feature "Close only Reels & Shorts". When the user turns it on, tend's accessibility service checks, only in Instagram, YouTube and Facebook, whether the short-video player (Reels / Shorts) is on screen, by its view ids, and presses Back if it is. This lets people keep using messages, search and regular videos while avoiding the endless short-video feeds, which is what they asked tend to help with. The service reads no text or content from the screen, stores nothing and sends nothing. It is off by default; the app shows a disclosure and asks for consent before sending the user to Android's accessibility settings, and it can be turned off there or in the app at any time.
```
- Video (same as for the foreground service is fine, a second clip): Settings → Apps → "Close only Reels & Shorts" → the disclosure dialog → Android's switch → open YouTube Shorts → it goes back, Bo says so.
- Fallback if the review objects: the feature is self-contained (`monitor/ShortsGuard.kt`, its `<service>` in the manifest, `ShortsGuardCard` in settings); it can be left out of the Play build without touching anything else.

### Health apps declaration
- Play Console → App content → Health apps: category **Activity and fitness** / **Sleep management** features used for habit rewards; no medical claims.

### tend Plus (not sold yet)
- The Play build has no purchases yet (`canBuy = false`): the listing says nothing about paid features, and the content rating answers "no purchases". When Play Billing is added: create the product in Play Console → Monetise, answer "in-app purchases: yes", and update this file.

### Sensitive permissions
- **Usage access** (`PACKAGE_USAGE_STATS`): core function, detecting which app is opened and measuring screen time.
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`): core function, showing the pause screen over the app being opened.
- **Exact alarms**: not used (only inexact alarms).
- **Battery optimisation**: the Play build doesn't request the exemption directly; it opens the system list.

## Before the first upload: checklist

1. `versionCode` in `app/build.gradle.kts` (1 for the first upload, +1 for each one after).
2. `keystore.properties` in place, then `./gradlew bundlePlayRelease`; back up the upload keystore somewhere safe (it can't be recreated).
3. Publish `docs/` (GitHub Pages): privacy policy at `/privacy/`, terms at `/terms/`, invite page at `/i/`.
4. Google Cloud → OAuth consent screen → **Publish app** (in "Testing" only the listed test users can sign in with Google). Add the Play **app signing** SHA-1 (Play Console → Setup → App signing) as an Android OAuth client, or Google sign-in fails for Play installs.
5. Play Console forms, from the sections above: Data safety, Content rating, Target audience, Ads, App access, Foreground service, Accessibility API, Health Connect / Health apps.
6. New screenshots if the look changed since `docs/play/screenshot-*.png` were made.

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
