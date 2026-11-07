# The clearing: plan

One world for everyone: a small clearing among trees on a riverbank. Minutes saved from scrolling
are spent on things for it. Friends' clearings sit next to yours along the river.

## Decisions (made)

- The clearing replaces the garden and the base. Their pieces become things you can buy.
- Fixed spots: about 10 places (bank, water, under the trees, fire pit); you pick what goes where.
- Sign-in: Google only, and only when you want friends or a backup. The app works fully without it.
- No chat, no free text between people, no public leaderboard.

## Economy

- Currency: **saved minutes**. Every night, the day's unused minutes go into your wallet
  (the same number that grew a plant until now). Weekly challenge: +60. Level up: +30.
- Prices: first thing on day one (flowers 15), weekly goals (hammock 120, grill 200),
  long goals (boat 600, dock 900, cabin 1500). Typical day: 20 to 40 saved.
- Things can be moved between spots any time; selling gives half back.
- Existing users: plants or logs collected so far become a starting balance
  (15 / 25 / 40 per night by size). Accessories worn by Bo stay as they are (earned, not bought).

## Things (first set, about 18)

| Spot | Things |
| - | - |
| Fire pit | campfire, grill, pizza oven |
| Under the trees | hammock, tent, log bench, swing |
| Bank | flowers, sunflowers, fishing chair, bike |
| Water | canoe, boat, small dock |
| Back | cabin, treehouse, bunting, string lights |

Bo uses them: sits on the bench, naps in the hammock (evening), stands at the grill, fishes.

## Friends

- **Adding:** "Invite a friend" shares a link (tend.app/i/CODE or the Play link with the code) and a
  QR code to scan side by side. Opening it in tend asks "Add Peti as a friend?". Both must say yes.
  A 6-letter code works too, typed in by hand. No search by name or e-mail, no contact upload.
- **What friends see:** your name (first name from Google, can be changed), companion, level,
  your clearing, this week's challenge progress. Never: which apps, minutes scrolled, health data.
- **The river:** your clearing in the middle, friends' clearings left and right along the same
  river; swipe to walk downstream. Tap one to visit (look only).
- **Interactions (one tap, all kind):**
  - **Wave:** once a day per friend. They see your companion waving on their home screen.
  - **Gift:** send a small thing from your wallet (flowers, lantern); it appears in their clearing.
  - **Together challenge:** pick a friend; when both finish the weekly challenge, a bridge
    appears between your clearings and both get +100.
  - **Cheer:** when a friend finishes a big activity (workout, long walk), a soft card offers a wave.
- **Safety:** remove a friend any time (silent); a removed friend can't re-add with an old code.
  Friends only, 13+, nothing public.

## Account and data

- Sign in with Google via Android Credential Manager, then Supabase Auth (ID token).
- Server keeps: profile (name, companion, level), clearing layout and wallet (also a backup when
  changing phones), friendships, waves, gifts. Row-level security: you read only your friends.
- Delete account in Settings (and a web page for Play's account deletion rule): removes everything.
- Waves and gifts arrive through the existing 15-minute background sync as local notifications;
  no Firebase push needed.
- Privacy page, data safety form and store listing are updated with each phase.

## Work, in shippable phases

Each phase ends with: unit tests, renders of every touched screen (large font, light and dark),
lint and tests on both flavors, a sideload APK to try on the phone.

1. **Clearing (offline).** Wallet, inventory, spots, prices, migration; the riverside scene and the
   things; shop and placing UI; Bo using things; home, world page, recap, widget, share card,
   intro and notes switched to the clearing. Then delete what's left of the garden and the base.
2. **Account.** Google sign-in, profile, backup and restore, account deletion, privacy updates.
   Needs from you first: a Google Cloud OAuth client (Android with the upload and Play signing
   SHA-1s, plus a Web client) and the Google provider switched on in Supabase.
3. **Friends.** Invite link, QR and code, accepting, the river view and visiting, waves, gifts,
   the together challenge and the bridge, notifications through the background sync.
4. **Later.** Seasons and weather, rare things for levels and challenges.

## Clean-up in phase 1

- Remove: `World` and its picker, the base (`ui/Base.kt`), the garden scene and plant beds,
  `Plant` storage once migrated, garden accessories (bunting, birdhouse, lanterns become things),
  the home stages track, garden/base wording, the 2x2 garden widget drawing (redrawn as the clearing).
- Keep: levels, weekly challenge, companion accessories, recap, notes, pause and timers.
