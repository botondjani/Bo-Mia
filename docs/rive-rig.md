# Bo & Mia Rive rig

`app/src/main/assets/tend_character.riv` is **generated** by `tools/rive/make_character.py` (no Rive editor needed). Every character in the app (home, onboarding, pause pop-up, "Worth it?", celebration, garden, reading timer) plays it. The home-screen widget uses the drawn version (`ui/CharacterPainter.kt`), because widgets can't run Rive. If the file is missing, the app falls back to the drawn version everywhere.

```bash
python3 tools/rive/make_character.py   # rewrites app/src/main/assets/tend_character.riv
```

`tools/rive/riv.py` is a small writer for the Rive runtime format (major version 7). Type and property keys come from the rive-runtime generated headers. The file was checked with the official Rive web runtime (`@rive-app/canvas` 2.44.0): it loads, has all inputs (incl. `scientist`), and all moods, both looks, breathing, blinking and the tap hop play.

## Contract

Read by `ui/Character.kt` → `RiveRig`. Names are case-sensitive.

| What | Name | Type | Values |
|---|---|---|---|
| Artboard | `Character` | 500 × 500 | feet at (250, 460) |
| State machine | `Mood` | | autoplays |
| Input | `mood` | Number | `0` happy · `1` tired · `2` scrolling · `3` outside · `4` reading |
| Input | `mia` | Boolean | `false` Bo · `true` Mia |
| Input | `tap` | Trigger | a short hop |
| Input | `headband`, `glasses`, `hat`, `flower` | Boolean | accessories earned by a week of habits (200 ms fade) |
| Input | `scientist` | Boolean | lab coat, glasses and a pointer (onboarding slides, home tour) |
| Input | `prop` | Number | during timers: `0` none · `1` dumbbell · `2` hearts · `3` palette and brush · `4` broom · `5` chef's hat |

A missing input doesn't crash the app; the rig just won't react to it.

## Layers

| Layer | What it does |
|---|---|
| mood | One pose per `mood` value, 250 ms blend between them: mouth, lids, body colour (grey when tired), pupils, phone, book, "z z", sun and grass |
| skin | Bo's tuft or Mia's ears and bow |
| breathe | Squash and stretch from the feet, 1.2 s each way, eased |
| blink | Blinks every 4.5 s |
| glance | Looks left, then right, every 9 s |
| scientist | Shows or hides the lab coat, glasses and pointer (200 ms) |
| point | The pointer waves around the hand, 1.6 s each way |
| prop | Shows the one prop the `prop` input names |
| tap | Hop with a little bounce; the shadow, grass and sun stay on the ground |

## Look

Same as the brand board and the drawn fallback: a soft egg-shaped blob in Mint `#7ECAA3` with a lighter belly, big eyes, cheeks; Bo with a brown tuft, Mia with coral ears and a sunny bow. In the `.riv` format the first drawable in the file is drawn on top, so the generator writes shapes front to back.

## Editing

The generated file is a runtime file; the Rive editor can't open it. Change the look or motion in `make_character.py` and regenerate. If a designer later builds the rig in the Rive editor, exporting it to the same path with the contract above replaces the generated one.
