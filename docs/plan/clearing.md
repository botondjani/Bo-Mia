# The clearing: plan

One world for everyone: a small clearing among trees on a riverbank. Minutes saved from scrolling
are spent on things for it. Friends' clearings sit next to yours along the river.

## Decisions (made)

- The clearing replaces the garden and the base. Their pieces become things you can buy.
- Fixed spots: about 10 places (bank, water, under the trees, fire pit); you pick what goes where.
- Sign-in: "Continue with Google", and a small "Log in with email instead" (6-digit code by e-mail,
  no password). Only when you want friends or a backup; the app works fully without it. E-mail needs
  our own sender (Supabase's built-in one allows only a few mails an hour), e.g. Resend with a domain.
- No chat, no free text between people, no public leaderboard.

## Economy

- Currency: **acorns**. Earned for what you do and for doing better than your own normal, never
  for the size of your budget (a 4-hour budget must not earn more than a 2-hour one).

| Source | Acorns | Cap per day |
| - | - | - |
| An activity done | +10 | 50 |
| "Not now" on a pause | +2 | 10 |
| Stayed within your budget (at day close) | +10 | 10 |
| Below your own normal (at day close) | +1 per 10 min | 20 |
| Weekly challenge done | +60 | |
| Level up | +30 | |

- Your normal: average daily minutes in the picked apps in the 14 days before tend (from usage
  stats); without that data, the first 7 days with tend.
- A good day is about 40 to 50, a weak one about 15. New users start with 50, and the first steps add 70 more.
- Prices so something is in reach every day: small things 10 to 15 (always buyable, 8 small
  spots around the camp), medium 40 to 90, big 150 to 300, long goals 500 to 900. The shop shows how many acorns
  are missing for the next thing. Small spots are few (8) and placed around the camp, never scattered.
- Existing users: plants and logs so far become acorns (15 / 25 / 40 per night by size).
- Selling gives half back.

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

1. **Clearing (offline)**, in small steps, each verified and committed:
   - 1a. Acorns, the catalogue of things, spots, placement, your normal, migration (logic and tests).
   - 1b. The riverside scene and the things, drawn (renders).
   - 1c. Shop and placing UI, acorns on home.
   - 1d. Home, recap, widget, share card, intro and notes switched to the clearing; garden and base
     code deleted.
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
