# HASTA Visual Specification

Version 1.0 — M0.5 (brand theme and Live Sign Coach camera shell).

This document is normative. Code that disagrees with it is a bug. Every token below
is implemented in `android/app/src/main/java/com/hasta/app/ui/theme/`, and the locked
contrast pairs are asserted by `ThemeTokensTest`.

---

## 1. Purpose and scope

The visual system exists to make HASTA feel like a calm, trustworthy learning tool
rather than a camera demo. It covers:

- the brand palette and its semantic roles,
- typography, spacing, shape, elevation and motion tokens,
- six core components,
- the Live Sign Coach camera shell and its feedback states,
- accessibility and rotation requirements.

It does **not** define sign recognition, lesson content, navigation graphs or any
future feature surface. Future-state previews exist only to prove the layout can
hold them; they are never emitted by the live app.

---

## 2. Brand foundations

Two brand colours carry the identity. Everything else is neutral or semantic.

| Token | Value | Role |
|---|---|---|
| Raspberry | `#C01D5A` | Brand primary. Fills primary actions and the brand mark. |
| Pale Sky | `#DFF1FD` | Brand container. Tinted informational surfaces. |
| Charcoal | `#24212B` | Primary text and the dark family base. |
| Muted | `#696574` | Secondary text, outlines, supporting copy. |
| White | `#FFFFFF` | Light surface. |
| Success | `#23845A` | Confirmed state. |
| Warning | `#C58B24` | Attention state. |

Rules:

- Raspberry is an **accent**, not a background wash. It fills primary actions and
  small marks; it never becomes a large reading surface.
- White text is used on Raspberry only. White is **never** placed on Pale Sky.
- Status is never communicated by colour alone: every status carries text and a
  glyph.

---

## 3. Semantic colour roles — light

Implemented in `HastaLightColors`.

| Role | Value | Used for |
|---|---|---|
| `brandPrimary` | `#C01D5A` | Primary button fill, brand mark, progress fill |
| `onBrandPrimary` | `#FFFFFF` | Text/icon on Raspberry |
| `brandContainer` | `#DFF1FD` | Informational card, level pill, secondary button |
| `onBrandContainer` | `#24212B` | Text/icon on Pale Sky |
| `brandAccent` | `#C01D5A` | Brand-tinted text and icons on light surfaces |
| `background` | `#FFFFFF` | Screen background off camera |
| `surface` | `#FFFFFF` | Cards and sheets |
| `surfaceRaised` | `#FFFFFF` | Elevated cards (border, not shadow) |
| `surfaceSubtle` | `#F7F5F9` | Grouped or inset sections |
| `textPrimary` | `#24212B` | Headings and body |
| `textSecondary` | `#696574` | Supporting copy, captions |
| `outline` | `#696574` | Focus and control outlines |
| `divider` | `#D9D4E0` | Hairlines and card borders |
| `success` | `#23845A` | Success fill and icon |
| `onSuccess` | `#FFFFFF` | Text on success fill |
| `successText` | `#1B6B48` | Success text on light surfaces |
| `warning` | `#C58B24` | Warning fill and icon |
| `onWarning` | `#24212B` | Text on warning fill |
| `warningText` | `#8A5A00` | Warning text on light surfaces |
| `error` | `#B3261E` | Error fill, icon and text |
| `onError` | `#FFFFFF` | Text on error fill |

---

## 4. Dark theme palette

Dark mode is a **separate tonal palette**, not an inversion. It is not generated
from the wallpaper. Implemented in `HastaDarkColors`.

| Role | Value |
|---|---|
| `brandPrimary` | `#C01D5A` |
| `onBrandPrimary` | `#FFFFFF` |
| `brandContainer` | `#24323C` |
| `onBrandContainer` | `#DFF1FD` |
| `brandAccent` | `#F49CBB` |
| `background` | `#141218` |
| `surface` | `#24212B` |
| `surfaceRaised` | `#2E2A36` |
| `surfaceSubtle` | `#1B1820` |
| `textPrimary` | `#F4F1F6` |
| `textSecondary` | `#B9B4C2` |
| `outline` | `#8E8798` |
| `divider` | `#3A3542` |
| `success` | `#5BC894` |
| `onSuccess` | `#141218` |
| `successText` | `#5BC894` |
| `warning` | `#E6B45C` |
| `onWarning` | `#2A1B00` |
| `warningText` | `#E6B45C` |
| `error` | `#FF9AAF` |
| `onError` | `#3B0710` |

Changes from light mode are deliberate:

- Raspberry keeps its exact value and stays a **fill**; on a dark surface it is only
  2.69:1 as text, so dark mode uses `brandAccent` (`#F49CBB`) for brand-tinted text.
- Pale Sky becomes `onBrandContainer` inside a desaturated brand container, never a
  page background.
- Status colours are lightened and desaturated so they stay readable without glowing.

---

## 5. Locked contrast pairings

These four pairings are fixed by the product blueprint and must not drift.

| Foreground | Background | Ratio | Required |
|---|---|---|---|
| White `#FFFFFF` | Raspberry `#C01D5A` | 5.89:1 | ≥ 4.5 |
| Raspberry `#C01D5A` | Pale Sky `#DFF1FD` | 5.09:1 | ≥ 4.5 |
| Charcoal `#24212B` | Pale Sky `#DFF1FD` | 13.66:1 | ≥ 4.5 |
| Muted `#696574` | Pale Sky `#DFF1FD` | 4.88:1 | ≥ 4.5 |

`ThemeTokensTest.documentedBlueprintPairsKeepTheirContrast` asserts all four within
0.02. Any new foreground/background pair must reach 4.5:1 for normal text and 3:1
for large text or non-text indicators. The camera chrome is additionally checked at
7:1 because it sits over live imagery.

---

## 6. Typography

System sans (Roboto on Android). No font is bundled, so platform text scaling and
script coverage stay native.

| Style | Size | Line height | Weight | Use |
|---|---|---|---|---|
| `displayLarge` | 36sp | 44sp | Bold | Brand/hero statements |
| `displayMedium` | 32sp | 40sp | Bold | Large numerals |
| `headlineLarge` | 30sp | 38sp | SemiBold | Screen titles |
| `headlineMedium` | 26sp | 34sp | SemiBold | Permission title |
| `titleLarge` | 22sp | 28sp | SemiBold | Panel title, brand name |
| `titleMedium` | 18sp | 24sp | SemiBold | Card title |
| `titleSmall` | 15sp | 20sp | SemiBold | Dense card title |
| `bodyLarge` | 16sp | 24sp | Normal | Primary reading size |
| `bodyMedium` | 14sp | 20sp | Normal | Panel body |
| `bodySmall` | 13sp | 18sp | Normal | Supporting copy |
| `labelLarge` | 14sp | 20sp | SemiBold | Buttons, status pill |
| `labelMedium` | 12sp | 16sp | SemiBold | Debug banner |
| `labelSmall` | 12sp | 16sp | Medium | Footnotes, level pill |

Rules:

- Body copy is never below 14sp on a camera screen and never below 12sp anywhere.
- Line height is always at least the font size; long-form copy uses 1.5×.
- Text must survive `fontScale = 2.0` without clipping or overlapping controls. The
  camera panel scrolls rather than truncating.
- The brand wordmark uses `titleLarge` with 3sp letter spacing; it is not an image.

---

## 7. Spacing and layout grid

A 4dp base grid, exposed as `HastaSpacing`.

| Token | Value | Typical use |
|---|---|---|
| `xxs` | 4dp | Icon/text inset |
| `xs` | 8dp | Icon-to-label gap |
| `sm` | 12dp | Row gaps, tight padding |
| `md` | 16dp | Screen gutter (portrait) |
| `lg` | 24dp | Panel padding, section gap |
| `xl` | 32dp | Screen padding (permission) |
| `xxl` | 48dp | Hero separation |

Layout rules:

- Screen gutter is 16dp in portrait and 24dp in landscape.
- Reading content is capped at 440–560dp so lines stay comfortable on tablets.
- The camera panel is capped at 560dp portrait / 320dp landscape.
- Nothing is placed inside the system gesture area; all screens apply
  `WindowInsets.safeDrawing`.

---

## 8. Shape

`HastaShapes`, mapped from Material 3 `Shapes`.

| Token | Radius | Use |
|---|---|---|
| `small` | 8dp | Small chips, inline highlights |
| `medium` | 12dp | Buttons, level pill |
| `large` | 16dp | Cards |
| `extraLarge` | 24dp | Camera feedback panel |

The brand mark uses a 3dp-radius square and the status dot is a true circle. Pills use
a fully rounded shape.

---

## 9. Elevation and surfaces

- Elevation is expressed with **tone and a 1dp border**, not drop shadows.
- Cards use `tonalElevation = 0.dp` and a `divider` border in light mode.
- The camera feedback panel is a solid `cameraPanel` (`#18151D`) surface with a
  subtle `cameraGuide` border at 16% alpha. It is intentionally **not** a blurred
  translucent panel: text contrast must never depend on the camera image.
- No surface uses wallpaper-derived colour.

---

## 10. Motion

`HastaMotion` defines three durations:

| Token | Value | Use |
|---|---|---|
| `fastMs` | 120ms | Press feedback, icon swap |
| `standardMs` | 180ms | Progress interpolation, panel state change |
| `emphasizedMs` | 240ms | Entering a new panel |

Rules:

- No looping or decorative animation runs while the camera is active.
- Progress is animated in the **draw phase** (`drawBehind`) so a five-second window
  does not relayout the panel each frame.
- Only transform and alpha are animated; layout properties are not.
- Motion conveys state change, never decoration.

### Reduced motion

When the system animator scale is `0`, `rememberReducedMotion()` selects
`HastaReducedMotion`, and every duration collapses to `0`. Content, labels and state
transitions remain; only the interpolation is removed. The practice timer still
advances (at 1s granularity instead of 100ms) so the window completes honestly.

---

## 11. Core components

### 11.1 Buttons — `HastaButton`

- Variants: `Primary` (Raspberry fill, white label), `Secondary` (Pale Sky fill,
  Charcoal label), `Text` (brand-accent label).
- Minimum height: 56dp for primary/secondary, 48dp for text.
- Fully rounded 12dp corners, 20dp horizontal padding, optional 18dp leading glyph.
- Disabled state uses `surfaceRaised` + `textSecondary`, never reduced alpha.
- The visible label is the accessible name. Controls must not replace it with a
  hidden description; if a label needs more context (for example a camera retry),
  make the visible label itself explicit ("Try camera again").

### 11.2 Surfaces and cards — `HastaCard`

- Variants: `Standard` (surface + divider border), `Subtle` (`surfaceSubtle`),
  `Accent` (Pale Sky).
- 16dp radius, 20dp inner padding.
- Never used to wrap the camera preview.

### 11.3 Lesson card — `LessonCard`

- 44dp Pale Sky icon tile with a hand glyph.
- Title (`titleMedium`), subtitle (`bodySmall`), duration (`labelSmall`), level pill.
- Optional determinate progress with a text label; progress is never colour-only.
- M0.5 renders this only with preview data. No lesson engine exists.

### 11.4 Progress indicator — `PracticeProgress`

- 4dp rounded track; 8dp radius fill.
- Determinate only; exposes `progressBarRangeInfo` to accessibility services.
- Track defaults to `cameraGuide` at 16% alpha; fill is Raspberry.
- On light cards the track is `divider` and the fill is Raspberry.
- The remaining time is also printed as text (`0:03`), so progress is readable
  without seeing the bar.

### 11.5 Navigation and top bar — `HastaTopBar`

- 56dp minimum height, brand lockup on the start edge, status on the end edge.
- On camera, the lockup uses `cameraText` and the status pill sits on the scrim.
- Off camera, it uses `textPrimary` / `textSecondary`.
- `compact` shrinks the gap for landscape; the bar never wraps to two rows.
- M0.5 has no navigation destinations, so no back button or tab bar is drawn.

### 11.6 Camera overlay and feedback panel — `CameraCoachPanel`

- Solid dark surface, 24dp radius, 24dp padding, scrollable body.
- Order: preview badge (previews only) → hand visibility → title → body →
  progress/time → footnote → primary action.
- The title is marked as a heading for screen readers.
- Maximum height is 56% of the safe drawing area in portrait, and the safe drawing area
  minus the top bar's full footprint (bar height plus its vertical padding) in
  landscape. The panel is top-anchored in landscape, so a tall state grows downward
  instead of being vertically centred and clipped. Height is always measured from the
  inset box that actually hosts the panel, never from the raw window.
- The panel never shows raw coordinates, FPS, confidence, latency or MediaPipe
  internals.

---

## 12. Camera chrome and Live Sign Coach states

The camera chrome is always dark, in both system themes, because the preview and
skeleton need a dark surround. Light/dark theming applies to permission and other
non-camera screens.

### 12.1 Chrome layers

1. Camera preview (`FILL_CENTER`) with the hand skeleton overlay.
2. Top scrim: vertical gradient from `cameraScrim` to transparent, 120dp portrait /
   96dp landscape.
3. Top bar with brand lockup and status pill.
4. Framing guide: four static corner brackets in `cameraGuide` at 55% alpha. Shown
   only in ready, no-hand and out-of-view guidance states.
5. Feedback panel.

### 12.2 Live states

| State | Panel | Tone | Primary action |
|---|---|---|---|
| Starting | "Starting camera" | Neutral | none (an action would fail) |
| Ready | "Ready when you are" | Neutral | Start practice |
| No hands | "Bring your hands into view" | Neutral | Start practice |
| One hand | "One hand visible" | Warning | Start practice |
| Two hands | "Both hands visible" | Success | Start practice |
| Practising | "Practising…" | Success | Stop practice |
| Practising, hands lost | "Practising…" + hands-out-of-view body | Warning | Stop practice |
| Window complete | "Practice window complete" | Neutral | Practise again |
| Tracking paused | "Hand tracking paused" | Error | Try tracking again |
| Camera unavailable | "Camera could not start" | Error | Try camera again |
| Permission required | "Camera access is needed" | Neutral | Allow camera |

Notes:

- No-hand and out-of-view states are **guidance**, not errors. They use neutral or
  warning tone and keep the practice window running.
- The completed window states plainly that no sign was recognised or saved. M0.5 has
  no recognition.
- Camera and tracker failures outrank an in-flight practice window.

### 12.3 Future states (previews only)

Evaluating, Recognised, Feedback available and Low confidence are rendered only in
`CoachPreviews.kt` and instrumented tests. They carry a "Preview only — not live"
badge. `LiveCoachPresenter` cannot emit them, which is asserted by
`livePresenterNeverEmitsAFutureRecognitionState`.

### 12.4 Hand skeleton

- 21 points per hand, connected with the topological edges.
- Left hand: Pale Sky. Right hand: Raspberry. Unknown: white.
- Every segment and point has a black 55%-alpha underlay so the skeleton reads over
  bright and dark backgrounds.
- Mirroring is display-only. The camera frame is never mirrored before inference, so
  handedness stays subject-relative.

---

## 13. Accessibility

Required, not optional:

- **Contrast**: all text pairs meet WCAG AA; camera text meets 7:1.
- **Touch targets**: every interactive control is at least 48dp in both dimensions.
- **Font scaling**: layouts reflow and scroll at `fontScale = 2.0`; no fixed-height
  text containers.
- **Screen readers**: the brand lockup, hand visibility, status and camera stage have
  explicit descriptions. Decorative glyphs and the framing guide are hidden from the
  accessibility tree.
- **Status**: text plus glyph plus tone; never colour alone. The status pill exposes
  its label as `stateDescription`.
- **Progress**: exposed as a determinate range with a text label.
- **Reduced motion**: durations collapse to zero; nothing is removed that conveys
  state.
- **Permission recovery**: rationale, denial and permanent denial each explain the
  consequence and offer the correct next step (allow or open settings).

---

## 14. Rotation and responsive behaviour

Rotation is a first-class layout case, not an afterthought.

Portrait:

- Top bar spans the width under the status bar.
- Framing guide sits slightly above centre.
- Feedback panel is anchored to the bottom, 16dp side margins, up to 56% viewport
  height.

Landscape:

- Top bar is compact and start-aligned so the status does not collide with the panel.
- Framing guide shifts left of centre to keep the signing area visible.
- Feedback panel moves to the **right edge**, top-anchored below the top bar, capped at
  320dp wide. Its height budget is the safe area minus the top bar footprint, so the
  primary action is never clipped at the bottom edge.
- The panel never covers the vertical centre of the preview.

Both:

- `WindowInsets.safeDrawing` keeps content clear of cutouts, the status bar and the
  gesture bar.
- The practice window is saved with `rememberSaveable`, so rotating mid-window keeps
  the remaining time and does not restart or double-count. The activity itself is
  recreated, so the camera is unbound and rebound; the learner sees at most a brief
  "Starting camera" state, never a lost session.
- The window only advances while the screen is started. Backgrounding the app
  freezes the remaining time instead of completing a session during which no frames
  were analysed, and a camera or tracker failure stops the window rather than letting
  the timer outlive the frames.
- Frame and panel state are separate streams, so 21-point landmark updates do not
  recompose the feedback panel.

`LiveCoachRotationTest` asserts panel placement, margins and top-bar clearance in
both orientations and across a runtime rotation.

---

## 15. Governance

- Colour, type, spacing, shape and motion values live **only** in the theme package.
  Components read `HastaTheme.colors` / `HastaTheme.motion`; hex values in components
  are a review failure.
- Changing a locked brand value or a documented pairing requires an update to this
  document, the blueprint, and `ThemeTokensTest` in the same change.
- New components must state which of the six core patterns they extend and must pass
  the accessibility checklist in section 13.
- This specification covers M0.5 only. Lesson flows, recognition feedback and full
  navigation will extend it rather than replace it.
