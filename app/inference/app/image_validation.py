"""Image MIME and size validation before model inference."""

from __future__ import annotations

import io
from dataclasses import dataclass

from PIL import Image, UnidentifiedImageError

from app.config import Settings

_MIME_BY_FORMAT = {
    "JPEG": "image/jpeg",
    "PNG": "image/png",
    "WEBP": "image/webp",
    "BMP": "image/bmp",
}


@dataclass
class ValidationResult:
    usable: bool
    issues: list[str]
    image: Image.Image | None = None
    mime_type: str | None = None


def _sniff_mime(data: bytes) -> str | None:
    if len(data) < 12:
        return None
    if data[:3] == b"\xff\xd8\xff":
        return "image/jpeg"
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return "image/png"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    if data[:2] == b"BM":
        return "image/bmp"
    return None


def validate_image(
    data: bytes,
    content_type: str | None,
    settings: Settings,
) -> ValidationResult:
    issues: list[str] = []

    max_bytes = int(settings.max_image_size_mb * 1024 * 1024)
    if len(data) == 0:
        return ValidationResult(usable=False, issues=["empty_file"])
    if len(data) > max_bytes:
        return ValidationResult(
            usable=False,
            issues=[f"file_too_large_max_{settings.max_image_size_mb}mb"],
        )

    declared = (content_type or "").split(";")[0].strip().lower()
    if (
        declared
        and declared not in settings.allowed_mime_types
        and declared != "application/octet-stream"
    ):
        issues.append(f"unsupported_mime_type:{declared}")

    sniffed = _sniff_mime(data)
    try:
        with Image.open(io.BytesIO(data)) as opened:
            opened.load()
            image = opened.convert("RGB")
            fmt = (opened.format or "").upper()
        decoded_mime = _MIME_BY_FORMAT.get(fmt) or sniffed
    except (UnidentifiedImageError, OSError, ValueError):
        issues.append("not_a_decodable_image")
        return ValidationResult(usable=False, issues=issues or ["invalid_image"])

    mime = sniffed or decoded_mime or (
        declared if declared in settings.allowed_mime_types else None
    )
    if mime is None or mime not in settings.allowed_mime_types:
        issues.append("unsupported_image_format")
        return ValidationResult(usable=False, issues=issues, mime_type=mime)

    width, height = image.size
    if width < 32 or height < 32:
        issues.append("image_too_small")
        return ValidationResult(usable=False, issues=issues, image=image, mime_type=mime)

    return ValidationResult(usable=True, issues=[], image=image, mime_type=mime)
