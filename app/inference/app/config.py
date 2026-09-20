"""Application settings loaded from environment variables."""

from __future__ import annotations

import os
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


def _env_bool(name: str, default: bool = False) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


class Settings:
    model_id: str
    model_device: str
    max_image_size_mb: float
    port: int
    load_in_4bit: bool
    max_new_tokens: int
    allowed_mime_types: frozenset[str]
    api_key: str
    require_api_key: bool
    cors_origins: list[str]

    def __init__(self) -> None:
        self.model_id = os.getenv("MODEL_ID", "Qwen/Qwen3-VL-2B-Instruct")
        self.model_device = os.getenv("MODEL_DEVICE", "cuda").strip().lower()
        self.max_image_size_mb = float(os.getenv("MAX_IMAGE_SIZE_MB", "15"))
        self.port = int(os.getenv("PORT", "8001"))
        self.load_in_4bit = _env_bool("LOAD_IN_4BIT", False)
        self.max_new_tokens = int(os.getenv("MAX_NEW_TOKENS", "1024"))
        self.api_key = os.getenv("API_KEY", "").strip()
        # When API_KEY is set, auth is always on. REQUIRE_API_KEY forces it even if empty (fail closed).
        self.require_api_key = _env_bool("REQUIRE_API_KEY", False) or bool(self.api_key)
        origins_raw = os.getenv("CORS_ORIGINS", "*").strip()
        if origins_raw == "*":
            self.cors_origins = ["*"]
        else:
            self.cors_origins = [o.strip() for o in origins_raw.split(",") if o.strip()]
        self.allowed_mime_types = frozenset(
            {
                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/webp",
                "image/bmp",
            }
        )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
