# M0 Product Scope

## Goal

Establish whether hands-only, two-hand MediaPipe tracking is technically viable on
the actual Android phone, with an aligned skeleton overlay and continuous operation.

## In scope

- Single Compose feasibility screen.
- Camera permission lifecycle.
- CameraX Preview + ImageAnalysis (front camera, 4:3).
- MediaPipe Hand Landmarker, `LIVE_STREAM`, two hands, handedness, 21 image and
  world landmarks.
- Compose skeleton overlay with correct mirroring and rotation.
- Tracking states and hand-count indicator.
- Debug-only ten-minute benchmark and export.
- Unit tests for pure logic; explicitly identified device tests for hardware.

## Out of scope

Sign recognition, temporal models, LiteRT, sign vocabulary, ISL dictionary, lessons,
quizzes, Room, DataStore, profiles, authentication, cloud, speech, sign correctness,
body pose, face landmarks, production branding, navigation, analytics.

Hands only. Up to two hands. No body anchors.

---

# M0.5 Addendum — Brand Theme and Live Sign Coach Camera Shell

M0.5 supersedes the "production branding" exclusion above for the camera shell only.
It adds a design system and a polished front end on top of the unchanged M0 engine.

## In scope

- Locked brand palette and semantic colour roles, light and dark.
- Typography, 4dp spacing grid, shape, elevation and motion tokens.
- Six core components: buttons, surfaces/cards, lesson card, progress indicator,
  top bar, camera overlay/feedback panel.
- Live Sign Coach states: starting, ready, no hands, one hand, two hands, practising,
  practice window complete, tracking paused, camera unavailable, permission required.
- A UI-only five-second practice window with honest "nothing saved" copy.
- Rotation-clean portrait and landscape layouts.
- Accessibility: contrast, 48dp targets, font scaling, screen-reader labels, reduced
  motion, non-colour status, permission recovery.
- `docs/spec/visual-spec.md` as the normative design reference.

## Preserved from M0

CameraX configuration, MediaPipe Hand Landmarker configuration, GPU fallback,
smoothing and exposure logic, lifecycle handling, overlay coordinate transform,
display-only mirroring, benchmark implementation and all M0 tests.

## Still out of scope

Sign recognition, GRU/CNN models, LiteRT, feature vectors, golden vectors,
vocabulary, lessons or quizzes, Room, DataStore, profiles, authentication, cloud,
speech, the assessment engine, full navigation, pose and face tracking.

Future recognition states (evaluating, recognised, feedback, low confidence) exist
only in debug previews and tests. The live presenter cannot emit them.

