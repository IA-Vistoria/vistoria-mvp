"""Batch-analyze all images in exemplos/ using the loaded ModelService."""

from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.config import get_settings
from app.model_service import model_service


def main() -> int:
    settings = get_settings()
    settings.model_device = "cuda"
    print(f"Loading {settings.model_id} on cuda…", flush=True)
    model_service.load(settings)
    print(f"Ready on {model_service.device}", flush=True)

    exemplos = ROOT / "exemplos"
    images = sorted(exemplos.glob("*"))
    images = [p for p in images if p.suffix.lower() in {".jpg", ".jpeg", ".png", ".webp", ".bmp"}]
    out_dir = ROOT / "exemplos" / "resultados"
    out_dir.mkdir(exist_ok=True)

    for path in images:
        print(f"\n=== {path.name} ===", flush=True)
        with Image.open(path) as im:
            image = im.convert("RGB")
        result = model_service.analyze(image, settings)
        payload = result.model_dump()
        out_file = out_dir / f"{path.stem}.json"
        out_file.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(payload, ensure_ascii=False, indent=2), flush=True)
        print(f"saved -> {out_file}", flush=True)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
