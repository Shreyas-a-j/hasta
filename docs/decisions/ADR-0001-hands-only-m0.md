# ADR-0001 — Hands-Only M0 and Front-Camera Mirroring

- Status: Accepted
- Date: 2026-10-30
- Milestone: M0

## Context

M0 must prove two-hand MediaPipe tracking with an aligned skeleton overlay. Three
decisions interact: input scope, inference mirroring, and display mirroring.

Google's official Android sample pre-rotates and horizontally flips the camera frame
before inference, then draws landmarks with a scale factor. That produces a
viewer-oriented image, but Google's own documentation states that passing a mirrored
image inverts the handedness output. HASTA needs subject-relative handedness for
future sign features, so the sample's approach is unsuitable.

## Decision

1. **Hands only.** M0 processes hand landmarks only. No pose, face or body anchors.
2. **Never mirror the inference input.** The camera frame is rotated to upright but
   not flipped.
3. **Mirror the display only.** The front-camera preview is mirrored by CameraX; the
   overlay mirrors landmark x-coordinates in view space.
4. **Preserve subject-relative handedness.** The left/right label from MediaPipe is
   shown as returned. Visually, the signer's left hand appears on the viewer's right.
5. **Rotation is applied by MediaPipe** through `ImageProcessingOptions`, so
   landmarks are normalized to the upright analysis image.
6. **Front camera only** for M0. A lens switch is out of scope.

## Consequences

- Handedness is correct without heuristics or label flipping.
- The overlay and the preview agree because both are mirrored for display.
- Coordinate mapping must account for rotation, center-crop and display mirroring;
  this is isolated in `OverlayTransform` and unit tested.
- If a future milestone needs viewer-oriented landmarks, it must mirror at the
  feature boundary and relabel handedness explicitly. It must not mirror the raw
  input silently.

## Verification

- JVM tests cover mirroring and rotation.
- Device checklist covers alignment in 0° and 90° orientation, with left and right
  hands, using a physical phone.
