# ADR-0002 — Rotate the analysis frame in the app, not in MediaPipe

- Status: Accepted
- Date: 31 October 2026
- Supersedes: the rotation step in `docs/architecture/m0-pipeline.md` v1 and the
  "Rotation applied by MediaPipe through `ImageProcessingOptions`" invariant.

## Context

From M0 onwards HASTA handed the unrotated analysis bitmap to the MediaPipe Hand
Landmarker and asked the task to rotate it:

```kotlin
ImageProcessingOptions.builder().setRotationDegrees(rotationDegrees).build()
```

The overlay assumed the landmarks that came back were normalized against the
upright image, so `HandOverlay` only applied a centre-crop scale and a display-only
mirror. The M0 report claimed overlay alignment passed on device (gates G8–G10),
but that check was visual and did not measure landmark coordinates.

On 31 October 2026 the user reported the skeleton was "180 rotated, not oriented".
A debug-only probe logged the analysis metadata and the landmark coordinates for a
hand held in a known pose on the moto g64 5G (`ZA222MM3L5`, Android 15):

```
analysis image=640x480 rot=180 format=35 upright=640x480
RIGHT:wrist=(0.385,0.300) midtip=(0.401,0.749) thumbtip=(0.225,0.559)
```

The user held the right hand with the fingers pointing up and the wrist near the
bottom of the preview. In the returned landmarks the **middle fingertip is below
the wrist** (`0.749 > 0.300`), i.e. the landmark frame was rotated 180 degrees
relative to the upright image.

Mapping those numbers through `OverlayTransform` predicted the wrist at screen
`(0.615, 0.167)` and the fingertip at `(0.599, 0.915)`; the captured screenshot
matched to within a couple of percent. That confirmed the overlay was faithfully
drawing an inverted frame rather than mis-mapping a correct one.

The alternative hypothesis — that the task applies the rotation in the opposite
direction — is arithmetically excluded at 180 degrees: an inverted sign applies
`-180`, which is identical to `+180`, and would have produced a **correct** result.
A 180-degree error therefore means the request was not applied at all.

## Decision

The app rotates the analysis bitmap itself, clockwise by
`ImageProxy.imageInfo.rotationDegrees`, before handing it to MediaPipe, and never
asks the task to rotate.

- `perception/camera/FrameRotation.kt` performs the rotation with an exact 90-degree
  `Matrix` (`filter = false`) and returns the source bitmap unchanged for 0 degrees.
- `HandLandmarkerTracker.detect` calls the two-argument
  `detectAsync(image, timestamp)`.
- `HandResultMapper.map` receives the already-upright dimensions and passes them
  through; it no longer swaps width and height.

## Consequences

- Landmarks are upright by construction. A double rotation is structurally
  impossible because no rotation is requested from the task.
- `HandFrame.imageWidth` / `imageHeight` now describe the bitmap MediaPipe actually
  saw, so the overlay transform has one meaning and no re-derivation.
- MediaPipe sees an upright hand, which is the orientation its detector was trained
  on. Rotating the landmarks instead would have kept a sideways input.
- One extra bitmap allocation per processed frame at analysis resolution
  (640x480), plus a rotate. This must be measured against the M0 benchmark baseline
  before any performance claim is made.
- Handedness semantics are unchanged: the frame is rotated, never mirrored, so
  handedness stays subject-relative and the overlay mirror stays display-only.
- `FrameRotationInstrumentedTest` pins the rotation direction with colour-quadrant
  assertions, so a future refactor cannot silently reverse it.
