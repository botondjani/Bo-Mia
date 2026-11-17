# tend on Google Play: listing and forms

Everything to paste into Play Console, in the order Play asks for it.

## Positioning

tend is a **screen-time app**: it limits the apps that take your time and lets you win minutes back by living your day. That is the promise, the category and the first words of the listing. The companion (Bo or Mia) and the place by the river are how it feels, not what it is.

Why this way: cozy habit trackers with a campfire theme exist (e.g. Tendcamp, iOS, a habit tracker with no app limits). tend competes on what they don't do: real limits on chosen apps, a pause before them, minutes unlocked offline, and friends doing it together. So: no "habit pet", no "habit tracker" in the title or short description, and "camp" stays in the body, not the headline.

- **Primary promise:** "Limit the apps that take your time, and win minutes back offline."
- **What only tend does:** a daily allowance for the apps you pick; a pause before them; minutes unlocked by a walk, a book or putting the phone down; Reels & Shorts closed while the rest of the app works; friends at the same fire.
- **Shelf:** Productivity (where people look for app limits and screen time).
- **Keyword clusters** (woven into title, short and full description, not stuffed):
  - The classic ones: screen time, app limits, app blocker, digital wellbeing, focus
  - The problem, in people's words: doomscrolling, phone addiction, social media detox, dopamine detox, stop scrolling, touch grass
  - The apps: TikTok, Instagram, YouTube Shorts, Reels (named only as examples)
  - The way out: phone down timer, walk, steps, sleep, reading, friends
- **Custom store listings later:** e.g. one for "dopamine detox", one for "app blocker"; worth it once there's traffic to compare.

## App

- **App name** (max 30): `tend: Screen Time & App Limit`
- **Package**: `hu.tend` (the play flavor's `applicationId`; side-loaded test builds are `app.tend`). It can't change after the first upload.
- **Category**: Productivity
- **Tags** (Play Console → Store settings): Digital wellbeing, Productivity, Focus, Self care
- **Contact e-mail**: botondjani@gmail.com (same as `support_email` in `app/src/main/res/values/strings.xml`)
- **Privacy policy URL**: `https://tend.hu/privacy/` (terms: `https://tend.hu/terms/`; `docs/` is served by Firebase Hosting, see `firebase.json`)

## Short description (max 80)

```
Limit TikTok & Instagram kindly. Walk, read or put the phone down to unlock more
```

## Full description (max 4000)

```
Less scrolling, without fighting yourself.

Pick the apps that take your time, like TikTok, Instagram or YouTube. They get a few free minutes a day. When those are gone, they stay closed until you unlock more: go for a walk, read, meet a friend, or simply put the phone down for a while. Bo, a small companion, keeps you company the whole way.

APP LIMITS THAT MAKE SENSE
• Only the apps you pick count: the rest of your phone is yours
• Free minutes every day, and a daily maximum you set
• Unlock more with real life: a walk, a workout, a good night's sleep, reading, learning, friends, cooking, tidying, or your own activities
• "Phone down": a timer from 15 to 120 minutes; every minute away unlocks a minute
• Walks, workouts and sleep count by themselves with Health Connect
• At the weekend, one extra hour when you run out (you choose, it never stays on)

A PAUSE BEFORE THE APPS THAT PULL HARDEST
Ask for a short pause before any app: Bo pops up for a breath, and often that's all it takes. "Not now" earns you something. Need it anyway? "Just checking something" opens it for a few minutes, and Bo lets you know when the time is up.
• Close only Reels & Shorts: messages, search and normal videos keep working
• Good night: your apps rest until 7:00, Bo sleeps too
• Your week at a glance: all your screen time, what counted, and the days you ran over

BETTER TOGETHER
Add friends and see each other's place along the river. Cheer for a walk, call someone to the fire and put your phones down at the same time, send a little present, trade things, reach a shared weekly goal. No chat, no feed, no likes.

A LITTLE PLACE THAT GROWS
Every activity and every "Not now" earns acorns. Spend them on a clearing by the river: a campfire, a hammock, a tent that becomes a cabin, a canoe, string lights. Levels unlock new things, seasons bring their own. Bo uses everything you get, even when you're away.

PRIVATE
No ads, no tracking, no account needed. Log in only for a backup or friends. Your health data and your app list never leave your phone.

A digital detox that sticks: less screen time, more of your day, and a small friend cheering you on.
```

## Store graphics (in `docs/play/`)

- App icon 512 × 512: `icon-512.png`
- Feature graphic 1024 × 500: `feature-graphic.png`
- Phone screenshots 1080 × 1920: `screenshot-1.png` … `screenshot-4.png` (made from the app as it is now; Play wants 2 to 8)

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
- Category: Productivity. No violence, sexuality, language, controlled substances, gambling, user-generated content, location sharing or purchases. Expected rating: Everyone / PEGI 3.

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
- Until Play Billing is in, Plus is on for everyone and no trial counts down (`TendStore.PLUS_ON_SALE = false`): nothing is locked that can't be bought. The listing says nothing about paid features, and the content rating answers "no purchases".
- When billing is added: create the product in Play Console → Monetise, set `PLUS_ON_SALE = true` (the 14-day trial starts counting then), answer "in-app purchases: yes", and update this file.

### Smaller download (R8)
- `./gradlew bundlePlayRelease -Pshrink` builds a shrunk bundle (about half the size). Try the shrunk side-loaded APK on a phone first (`assembleSideloadRelease -Pshrink`): every screen, Google sign-in, the pause, a timer. Once it holds, make shrinking the default in `app/build.gradle.kts`.

### Sensitive permissions
- **Usage access** (`PACKAGE_USAGE_STATS`): core function, detecting which app is opened and measuring screen time.
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`): core function, showing the pause screen over the app being opened.
- **Exact alarms**: not used (only inexact alarms).
- **Battery optimisation**: the Play build doesn't request the exemption directly; it opens the system list.

## Before the first upload: checklist

1. `versionCode` in `app/build.gradle.kts` (1 for the first upload, +1 for each one after).
2. `keystore.properties` in place, then `./gradlew bundlePlayRelease`; back up the upload keystore somewhere safe (it can't be recreated).
3. Publish `docs/` to https://tend.hu (`firebase deploy --only hosting`): privacy policy at `/privacy/`, terms at `/terms/`, invite page at `/i/`, app links at `/.well-known/assetlinks.json` (add the Play app signing SHA-256 there next to the upload key's).
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
