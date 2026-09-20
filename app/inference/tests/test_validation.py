"""Lightweight tests that do not require the VLM weights."""

from __future__ import annotations

import io
import unittest

from PIL import Image

from app.config import Settings
from app.image_validation import validate_image
from app.model_service import ModelService


class ImageValidationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.settings = Settings()
        self.settings.max_image_size_mb = 1

    def _png_bytes(self, size=(64, 64), color=(180, 180, 180)) -> bytes:
        buf = io.BytesIO()
        Image.new("RGB", size, color).save(buf, format="PNG")
        return buf.getvalue()

    def test_valid_png(self) -> None:
        result = validate_image(self._png_bytes(), "image/png", self.settings)
        self.assertTrue(result.usable)
        self.assertEqual(result.issues, [])
        self.assertIsNotNone(result.image)

    def test_rejects_non_image(self) -> None:
        result = validate_image(b"not-an-image", "text/plain", self.settings)
        self.assertFalse(result.usable)
        self.assertTrue(result.issues)

    def test_rejects_oversized(self) -> None:
        big = self._png_bytes(size=(2000, 2000))
        # Force tiny limit
        self.settings.max_image_size_mb = 0.0001
        result = validate_image(big, "image/png", self.settings)
        self.assertFalse(result.usable)
        self.assertTrue(any("file_too_large" in i for i in result.issues))


class JsonParseTests(unittest.TestCase):
    def test_extracts_wrapped_json(self) -> None:
        raw = '```json\n{"image_quality":{"usable":true,"issues":[]},"areas":[],"overall_summary":"ok","limitations":["x"]}\n```'
        parsed = ModelService._extract_json(raw)
        self.assertIsNotNone(parsed)
        assert parsed is not None
        self.assertEqual(parsed["overall_summary"], "ok")

    def test_maps_levels(self) -> None:
        self.assertEqual(ModelService._normalize_level("high"), "alta")
        self.assertEqual(ModelService._normalize_level("medium"), "media")


if __name__ == "__main__":
    unittest.main()
