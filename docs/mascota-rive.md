# Mascot (Rive) — rig, animations and inputs

Runtime file: `app/src/main/res/raw/mascota.riv` (≈ 641 KB, 60 fps, all images embedded).
Source file lives in the Rive cloud editor: <https://editor.rive.app/file/untitled/2659637>
(artboard **Mascota**, 1100 × 1344 px, transparent background, character centered with margins so
raised arms and the jump are never clipped). Runtime: `app.rive:rive-android:11.14.3` (Compose API).

## 1. Rig: pivot-node hierarchy

The Rive MCP used to build the file (v0.6) cannot create bones, so the character is a classic
cut-out rig: every part is an image under a pivot **node**; rotating/scaling a node moves
its children. Meshes/IK were not used (squash & stretch is done with node scale).

```
root                       ground pivot; jump = root.y
├─ tail_base               pivot at the body joint; rotates first
│  ├─ tail_seg1            image
│  └─ tail_tip             rotates after tail_base (overlap / follow-through)
│     └─ tail_tip_img
└─ body                    pivot at the hips; scale Y/X = breathing, squash & stretch
   ├─ chest                pivot at the waist; sways, drops (shrug)
   │  ├─ head              pivot at the neck
   │  │  ├─ head_img       head without pupils/mouth (nose included)
   │  │  ├─ eye_L / eye_R  pivots at each eye (look-up for "think")
   │  │  │  ├─ gaze_L/R    bound to lookX / lookY (pupil offset)
   │  │  │  │  └─ pupil_L/R
   │  │  │  ├─ lid_L/R     cream eyelid, scaleY 0→100 closes the eye
   │  │  │  └─ blink_line_L/R   closed-eye curve, fades in while closed
   │  │  ├─ nose
   │  │  └─ mouth          open_happy (default), smile, neutral, o (opacity swap)
   │  ├─ arm_L             pivot at the shoulder (arm and hand are one image)
   │  └─ arm_R
   ├─ foot_L / foot_R      pivot at the ankle; foot_R lifts on celebrate
   ├─ torso                shoulders, sides and legs, as drawn in the original
   └─ belly
```

Design notes: pieces come from the approved sheets; the belly, torso, tail and face parts were
extracted from the approved full-character image so the assembled pose matches it
(silhouette IoU 0.98). No new design elements were added (no thumbs-up hand: it is not in the sheet).

## 2. Animations (60 fps)

| Animation | Duration | Loop | What it does |
|---|---|---|---|
| `idle` | 3.6 s | loop | breathing (K1 base → K2 up + slight tilt → K3 center → K4 settle); head, arms and tail lag the body |
| `blink` | 0.2 s | once | lids close/open, closed-eye curve in between |
| `wait_a` / `wait_b` / `wait_c` | 2.0 / 4.5 / 3.0 s | once | silent waits between blinks (random) |
| `wave` | 1.2 s | once | anticipation, right arm up, 3 hand swings, head tilt, smile, tail balance |
| `celebrate` | 1.5 s | once | crouch, both arms up, jump with one foot raised, squash on landing, tail wag |
| `encourage` | 1.4 s | once | half-raised arm pumps, head tilt, smile and a wink (right eye) |
| `tryAgain` | 1.6 s | once | soft empathetic shrug, head tilt, arms slightly open, gentle smile — never negative |
| `think` | 1.8 s | once | head tilt, eyes look up, arm raised a little, "o" mouth |
| `expr_0…4` | 0.1 s | loop | single-pose holds for each expression |

Easing is cubic (ease-in-out / ease-out / ease-in), never linear; one-shots start and end on the
rest pose so they blend back to `idle` without jumps.

## 3. State machine `MascotSM` (four independent layers)

| Layer | Behaviour |
|---|---|
| `Base` | `idle` forever |
| `Blink` | `wait_a/b/c` → `blink` → one of `wait_a/b/c` chosen at random (random transitions on) |
| `Expression` | `{Any State}` → `expr_k` when `expression == k`, 180 ms mix; starts on `expr_1` (happy) |
| `Actions` | `rest` → action on its trigger (200 ms mix); action → `rest` when it finishes (250 ms mix) |

Layer order matters: later layers override earlier ones, so an action overrides the idle motion
and the expression only while it plays.

## 4. Inputs (view model `MascotVM`, default instance `Default`)

| Input | Type | Values | Effect |
|---|---|---|---|
| `wave` | trigger | — | greeting |
| `celebrate` | trigger | — | correct answer / lesson completed |
| `encourage` | trigger | — | encouragement with wink |
| `tryAgain` | trigger | — | wrong answer, empathetic |
| `think` | trigger | — | thinking pose |
| `expression` | number | 0 neutral, 1 happy (default), 2 curious, 3 encouraging, 4 empathic | face |
| `lookX` | number | −14 … 14 (artboard px) | pupils horizontal offset |
| `lookY` | number | −14 … 14 | pupils vertical offset |

`MascotController.lookAt(x, y)` takes −1…1 and scales/clamps to that range.

## 5. Kotlin API (`presentation/mascot`)

| Piece | Role |
|---|---|
| `MascotEvent` | `Idle, Wave, Celebrate, Encourage, TryAgain, Think` |
| `MascotExpression` | `Neutral, Happy, Curious, Encouraging, Empathic` |
| `MascotController` | `play(event)`, `setExpression(e)`, `lookAt(x, y)`; queues commands until the Rive file is ready |
| `MascotaRive(controller, modifier, paused)` | composable that renders the `.riv` and binds the view model |
| `MascotViewModel` | owns the controller (survives rotation); `onCue(MascotCue)`; idle → curious every 20 s of inactivity |
| `MascotCue` | `ScreenStarted→wave`, `AnswerCorrect→celebrate`, `AnswerIncorrect→tryAgain`, `LessonCompleted→celebrate`, `Thinking→think` |
| `MascotPlaygroundRoute` | debug screen with a button for every animation, expression and gaze |

Wired today: `SessionSummaryScreen` celebrates on lesson completion. The app has no explicit
correct/incorrect answer logic yet; when it does, call `mascotViewModel.onCue(AnswerCorrect / AnswerIncorrect)`.

Open the playground without logging in (debug builds only):

```bash
adb shell am start -n co.edu.uniautonoma.inclusivereadingar/.MascotPlaygroundActivity
```

## 6. Measured behaviour

Emulator `sdk_gphone64_x86_64`, Android 16 (API 36.1), 1080×2400, **2 GB RAM with swap in use** (a
deliberately low-end target) and an emulated GPU, playground screen:

| Scenario | Frames/s (`gfxinfo`) | p50 / p95 frame time |
|---|---|---|
| idle loop | 29 | 28 ms / 48 ms |
| wave · think · tryAgain | 24 | 30 ms / 65 ms |
| expression changes | 29 | 32 ms / 61 ms |
| celebrate ×3 (heaviest) | 20 | 30 ms / 101 ms |

These numbers do **not** reach 60 fps on this emulator: the Rive worker thread takes about half a
core and Rive logs that the emulated GPU lacks fragment interlock, so it falls back to
depth-stencil with 4× MSAA. They are a worst-case floor, not a real-phone figure; measure on a
physical low-end device before tuning. Cheap levers if needed: shrink the artboard margins (the
character fills ~60 % of the surface), or cap the frame rate through the `frameRate` parameter of `Rive`.

Other checks: pauses at 0 frames when the app is in the background and resumes on return;
4 consecutive rotations without crash or Rive errors; ≈ 130 MB PSS for the whole app.

## 7. Changing the animation

Edit the file in the Rive editor, export *For Runtime* and replace `res/raw/mascota.riv`.
Keep the view model name `MascotVM` and the input names above, or update `MascotInputNames`.
