"""MediaPipe Hand Landmarker adapter for research capture.

MediaPipe is an optional dependency. Importing this module is safe; the imports
happen inside [HandLandmarkerSession] so the rest of the research tooling and its
tests run without the ``capture`` extra installed.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np

from .capture import DetectedHand


@dataclass(frozen=True)
class TrackerConfig:
    model_path: Path
    max_hands: int = 2
    min_detection_confidence: float = 0.5
    min_presence_confidence: float = 0.5
    min_tracking_confidence: float = 0.5

    def validate(self) -> None:
        if not Path(self.model_path).is_file():
            raise ValueError(f"MediaPipe model not found: {self.model_path}")
        if self.max_hands != 2:
            raise ValueError("features-v1 research capture requires exactly two hand slots")
        for name, value in (
            ("min_detection_confidence", self.min_detection_confidence),
            ("min_presence_confidence", self.min_presence_confidence),
            ("min_tracking_confidence", self.min_tracking_confidence),
        ):
            if not 0.0 <= value <= 1.0:
                raise ValueError(f"{name} must be in [0, 1]")


class HandLandmarkerSession:
    """Thin, non-owning wrapper around MediaPipe's VIDEO-mode landmarker."""

    def __init__(self, config: TrackerConfig) -> None:
        config.validate()
        try:
            import mediapipe as mp
            from mediapipe.tasks import python as mp_python
            from mediapipe.tasks.python import vision
        except ImportError as error:  # pragma: no cover - depends on optional extra
            raise RuntimeError(
                "MediaPipe capture is not installed; run `uv sync --extra capture`"
            ) from error

        self._mp = mp
        self._vision = vision
        options = vision.HandLandmarkerOptions(
            base_options=mp_python.BaseOptions(model_asset_path=str(config.model_path)),
            running_mode=vision.RunningMode.VIDEO,
            num_hands=config.max_hands,
            min_hand_detection_confidence=config.min_detection_confidence,
            min_hand_presence_confidence=config.min_presence_confidence,
            min_tracking_confidence=config.min_tracking_confidence,
        )
        self._landmarker = vision.HandLandmarker.create_from_options(options)

    def detect(self, rgb: np.ndarray, timestamp_ms: int) -> list[DetectedHand]:
        image = self._mp.Image(
            image_format=self._mp.ImageFormat.SRGB,
            data=np.ascontiguousarray(rgb),
        )
        result = self._landmarker.detect_for_video(image, int(timestamp_ms))
        hands: list[DetectedHand] = []
        for index, landmarks in enumerate(result.hand_landmarks):
            points = np.asarray(
                [[landmark.x, landmark.y, landmark.z] for landmark in landmarks],
                dtype=np.float64,
            )
            if points.shape != (21, 3):
                continue
            categories = result.handedness[index] if index < len(result.handedness) else []
            category = categories[0] if categories else None
            label = (category.category_name or "").lower() if category is not None else ""
            hands.append(
                DetectedHand(
                    handedness=label if label in ("left", "right") else None,
                    handedness_score=float(category.score) if category is not None else 0.0,
                    landmarks=points,
                )
            )
        return hands

    def close(self) -> None:
        self._landmarker.close()

    def __enter__(self) -> "HandLandmarkerSession":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()
