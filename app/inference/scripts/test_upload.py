#!/usr/bin/env python3
"""Minimal CLI helper to POST an image to /analyze."""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request


def main() -> int:
    parser = argparse.ArgumentParser(description="Upload an image to the vistoria IA service")
    parser.add_argument("image_path", help="Path to a JPEG/PNG/WebP/BMP file")
    parser.add_argument(
        "--url",
        default=os.getenv("VLM_URL", "http://localhost:8001") + "/analyze",
        help="Analyze endpoint URL (or set VLM_URL base)",
    )
    parser.add_argument(
        "--api-key",
        default=os.getenv("API_KEY", ""),
        help="API key (header X-API-Key). Also reads API_KEY env.",
    )
    args = parser.parse_args()

    with open(args.image_path, "rb") as fh:
        raw = fh.read()

    boundary = "----vistoriaBoundary7MA4YWxkTrZu0gW"
    filename = args.image_path.replace("\\", "/").split("/")[-1]
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="image"; filename="{filename}"\r\n'
        f"Content-Type: application/octet-stream\r\n\r\n"
    ).encode() + raw + f"\r\n--{boundary}--\r\n".encode()

    headers = {"Content-Type": f"multipart/form-data; boundary={boundary}"}
    if args.api_key:
        headers["X-API-Key"] = args.api_key

    req = urllib.request.Request(
        args.url,
        data=body,
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            payload = resp.read().decode("utf-8")
            print(json.dumps(json.loads(payload), ensure_ascii=False, indent=2))
            return 0
    except urllib.error.HTTPError as exc:
        print(exc.read().decode("utf-8", errors="replace"), file=sys.stderr)
        return 1
    except Exception as exc:  # noqa: BLE001
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
