# Bo & Mia Rive rig

The app looks for `app/src/main/assets/tend_character.riv`. If the file is there, every character in the app (home, onboarding, pause screen, "Worth it?", celebration, garden, patterns) plays the Rive rig. Without it, the drawn fallback (`ui/CharacterPainter.kt`) is used. The home-screen widget always uses the drawn version, because widgets can't run Rive.

## Contract

These names are what the code reads (`ui/Character.kt` → `RiveRig`). They are case-sensitive.

| What | Name | Type | Values |
|---|---|---|---|
| Artboard | `Character` | 500 × 500, square | character centred, feet on the ground ~8% above the bottom edge |
| State machine | `Mood` | | must be the only one the app needs; autoplays |
| Input | `mood` | Number | `0` happy · `1` tired · `2` scrolling · `3` outside |
| Input | `mia` | Boolean | `false` Bo · `true` Mia |
| Input | `tap` | Trigger | optional, fired when the user taps the character |

A missing input doesn't crash the app; the rig just won't react to it.

## Look (from the brand board)

- A soft egg-shaped blob with big eyes. The eyes and posture carry the emotion.
- Body Mint `#7ECAA3`, belly a lighter mint `#A9DDC1`, cheeks `#C9937A` at ~60%, outline-free.
- **Bo:** a brown hair tuft `#5A3A2A` on top.
- **Mia:** coral ears/buns `#E76649` and a sunny bow `#F6D673`.
- One rig, two skins: switch the hair/ears/bow with `mia` (e.g. solo or opacity on a layer).

## States

| `mood` | Name | Pose and loop |
|---|---|---|
| 0 | Happy | Gentle breathing (squash/stretch from the feet, ~2.4 s), blinks every 2.5–5.5 s, occasional glance left/right. Smile. |
| 1 | Tired | Grey body `#AEB8B0`, heavy lids, small frown, slow breathing (~3.6 s), "z z" floating. A nudge, never a punishment. |
| 2 | Scrolling | Half-lidded eyes staring down at a phone in its hand, blue screen glow on the pupils, flat mouth, thumb scrolling. A mirror, not a lecture. |
| 3 | Outside | Big open smile, a little bounce, sun in the corner, grass at the feet. After steps, a workout or time outdoors. |

Transitions between states: ~250 ms blends. `tap`: a short happy hop (~400 ms) that returns to the current state.

## Try it

1. Build the rig in the Rive editor (rive.app) with the names above.
2. Export as `.riv` (runtime format) and save it as `app/src/main/assets/tend_character.riv`.
3. Build the APK. Nothing else needs to change in the code.
