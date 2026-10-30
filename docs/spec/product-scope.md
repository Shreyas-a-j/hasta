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
