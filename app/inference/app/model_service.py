"""VLM load-once service and structured analysis."""

from __future__ import annotations

import json
import logging
import re
import threading
from typing import Any

from PIL import Image

from app.config import Settings, get_settings
from app.prompts import SYSTEM_PROMPT, USER_PROMPT
from app.schemas import AnalysisResponse, AreaFinding, ImageQuality

logger = logging.getLogger(__name__)

_JSON_BLOCK_RE = re.compile(r"\{[\s\S]*\}")


class ModelNotReadyError(RuntimeError):
    pass


class ModelService:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._model: Any | None = None
        self._processor: Any | None = None
        self._device: str = "cpu"
        self._loaded = False
        self._load_error: str | None = None
        self._model_id: str = ""

    @property
    def loaded(self) -> bool:
        return self._loaded

    @property
    def device(self) -> str:
        return self._device

    @property
    def model_id(self) -> str:
        return self._model_id

    @property
    def load_error(self) -> str | None:
        return self._load_error

    def load(self, settings: Settings | None = None) -> None:
        import torch
        from transformers import AutoProcessor, Qwen3VLForConditionalGeneration

        settings = settings or get_settings()
        with self._lock:
            if self._loaded:
                return
            self._model_id = settings.model_id
            try:
                device = self._resolve_device(settings.model_device)
                self._device = device
                logger.info("Loading model %s on %s", settings.model_id, device)

                dtype = torch.bfloat16 if device == "cuda" else torch.float32
                load_kwargs: dict[str, Any] = {
                    "dtype": dtype,
                    "trust_remote_code": False,
                }

                if device == "cuda":
                    load_kwargs["device_map"] = "auto"
                    if settings.load_in_4bit:
                        from transformers import BitsAndBytesConfig

                        load_kwargs["quantization_config"] = BitsAndBytesConfig(
                            load_in_4bit=True,
                            bnb_4bit_compute_dtype=torch.bfloat16,
                        )
                        load_kwargs.pop("dtype", None)
                else:
                    load_kwargs["device_map"] = None

                self._processor = AutoProcessor.from_pretrained(settings.model_id)
                self._model = Qwen3VLForConditionalGeneration.from_pretrained(
                    settings.model_id,
                    **load_kwargs,
                )
                if device == "cpu":
                    self._model = self._model.to("cpu")
                self._model.eval()
                self._loaded = True
                self._load_error = None
                logger.info("Model loaded successfully")
            except Exception as exc:  # noqa: BLE001 — keep process alive for /health
                self._loaded = False
                self._load_error = str(exc)
                logger.exception("Failed to load model")
                raise

    @staticmethod
    def _resolve_device(requested: str) -> str:
        import torch

        requested = requested.lower().strip()
        if requested == "cpu":
            return "cpu"
        if requested in {"cuda", "gpu"}:
            if torch.cuda.is_available():
                return "cuda"
            logger.warning("CUDA requested but unavailable; falling back to CPU")
            return "cpu"
        if requested == "auto":
            return "cuda" if torch.cuda.is_available() else "cpu"
        logger.warning("Unknown MODEL_DEVICE=%s; using auto", requested)
        return "cuda" if torch.cuda.is_available() else "cpu"

    def analyze(self, image: Image.Image, settings: Settings | None = None) -> AnalysisResponse:
        import torch
        from qwen_vl_utils import process_vision_info

        settings = settings or get_settings()
        if not self._loaded or self._model is None or self._processor is None:
            raise ModelNotReadyError("Model is not loaded")

        messages = [
            {
                "role": "system",
                "content": [{"type": "text", "text": SYSTEM_PROMPT}],
            },
            {
                "role": "user",
                "content": [
                    {"type": "image", "image": image},
                    {"type": "text", "text": USER_PROMPT},
                ],
            },
        ]

        with self._lock:
            try:
                text = self._processor.apply_chat_template(
                    messages,
                    tokenize=False,
                    add_generation_prompt=True,
                )
                images, videos, video_kwargs = process_vision_info(
                    messages,
                    image_patch_size=16,
                    return_video_kwargs=True,
                    return_video_metadata=True,
                )
                if videos is not None:
                    videos, video_metadatas = zip(*videos)
                    videos, video_metadatas = list(videos), list(video_metadatas)
                else:
                    video_metadatas = None

                inputs = self._processor(
                    text=[text],
                    images=images,
                    videos=videos,
                    video_metadata=video_metadatas,
                    return_tensors="pt",
                    do_resize=False,
                    **(video_kwargs or {}),
                )
                target = next(self._model.parameters()).device
                inputs = {
                    k: v.to(target) if hasattr(v, "to") else v
                    for k, v in inputs.items()
                }

                with torch.inference_mode():
                    generated_ids = self._model.generate(
                        **inputs,
                        max_new_tokens=settings.max_new_tokens,
                        do_sample=False,
                    )

                trimmed = [
                    out_ids[len(in_ids) :]
                    for in_ids, out_ids in zip(inputs["input_ids"], generated_ids)
                ]
                output_text = self._processor.batch_decode(
                    trimmed,
                    skip_special_tokens=True,
                    clean_up_tokenization_spaces=False,
                )[0]
            except Exception as exc:  # noqa: BLE001
                logger.exception("Inference failed")
                raise RuntimeError("model_inference_failed") from exc

        return self._parse_model_output(output_text)

    def _parse_model_output(self, raw: str) -> AnalysisResponse:
        payload = self._extract_json(raw)
        if payload is None:
            return AnalysisResponse(
                image_quality=ImageQuality(
                    usable=True,
                    issues=["model_output_not_json"],
                ),
                areas=[],
                overall_summary=(
                    "A análise visual não pôde ser estruturada de forma confiável a partir "
                    "da resposta do modelo."
                ),
                limitations=[
                    "Análise baseada apenas em imagem; não substitui vistoria técnica presencial.",
                    "Resposta do modelo não estava em JSON válido.",
                ],
            )

        try:
            areas_raw = payload.get("areas") or []
            areas: list[AreaFinding] = []
            for item in areas_raw:
                if not isinstance(item, dict):
                    continue
                areas.append(
                    AreaFinding(
                        area=str(item.get("area") or "outro"),
                        issue_type=str(item.get("issue_type") or "other_visual_issue"),
                        description=str(item.get("description") or ""),
                        evidence=str(item.get("evidence") or ""),
                        severity=self._normalize_level(item.get("severity")),  # type: ignore[arg-type]
                        confidence=self._normalize_level(item.get("confidence")),  # type: ignore[arg-type]
                        recommendation=str(
                            item.get("recommendation")
                            or "Recomenda-se avaliação presencial."
                        ),
                        location=(
                            str(item["location"])
                            if item.get("location") not in (None, "")
                            else None
                        ),
                    )
                )

            iq = payload.get("image_quality") or {}
            limitations = payload.get("limitations") or [
                "Análise baseada apenas em imagem; não substitui vistoria técnica presencial."
            ]
            if not isinstance(limitations, list):
                limitations = [str(limitations)]

            return AnalysisResponse(
                image_quality=ImageQuality(
                    usable=bool(iq.get("usable", True)),
                    issues=[str(x) for x in (iq.get("issues") or [])],
                ),
                areas=areas,
                overall_summary=str(
                    payload.get("overall_summary")
                    or "Observação visual concluída."
                ),
                limitations=[str(x) for x in limitations],
            )
        except Exception:  # noqa: BLE001
            logger.exception("Failed to map model JSON to schema")
            return AnalysisResponse(
                image_quality=ImageQuality(usable=True, issues=["schema_mapping_failed"]),
                areas=[],
                overall_summary="Não foi possível mapear a resposta do modelo ao contrato.",
                limitations=[
                    "Análise baseada apenas em imagem; não substitui vistoria técnica presencial.",
                ],
            )

    @staticmethod
    def _normalize_level(value: Any) -> str:
        text = str(value or "media").strip().lower()
        mapping = {
            "low": "baixa",
            "baixa": "baixa",
            "medium": "media",
            "média": "media",
            "media": "media",
            "high": "alta",
            "alta": "alta",
        }
        return mapping.get(text, "media")

    @staticmethod
    def _extract_json(raw: str) -> dict[str, Any] | None:
        text = raw.strip()
        if text.startswith("```"):
            text = re.sub(r"^```(?:json)?\s*", "", text)
            text = re.sub(r"\s*```$", "", text)
        try:
            data = json.loads(text)
            return data if isinstance(data, dict) else None
        except json.JSONDecodeError:
            match = _JSON_BLOCK_RE.search(raw)
            if not match:
                return None
            try:
                data = json.loads(match.group(0))
                return data if isinstance(data, dict) else None
            except json.JSONDecodeError:
                return None


model_service = ModelService()
