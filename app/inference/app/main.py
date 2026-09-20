"""HTTP service: image in, structured visual analysis JSON out."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from app.auth import ApiKeyMiddleware
from app.config import get_settings
from app.image_validation import validate_image
from app.model_service import ModelNotReadyError, model_service
from app.schemas import AnalysisResponse, HealthResponse, ImageQuality

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger(__name__)

STATIC_DIR = Path(__file__).resolve().parent.parent / "static"


@asynccontextmanager
async def lifespan(_app: FastAPI):
    settings = get_settings()
    if settings.require_api_key and not settings.api_key:
        logger.warning(
            "Auth is required but API_KEY is empty — /analyze will return 503 until configured"
        )
    try:
        model_service.load(settings)
    except Exception:
        # Keep the process up so /health can report the failure.
        logger.error("Startup model load failed; service will report unhealthy")
    yield


app = FastAPI(
    title="inference — IA Vistoria",
    description="Serviço isolado de análise visual de paredes/rodapés (VM de IA).",
    version="0.1.0",
    lifespan=lifespan,
)

_settings = get_settings()
app.add_middleware(
    CORSMiddleware,
    allow_origins=_settings.cors_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)
app.add_middleware(ApiKeyMiddleware)

if STATIC_DIR.is_dir():
    app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse | JSONResponse:
    settings = get_settings()
    if model_service.loaded:
        return HealthResponse(
            status="ok",
            model_loaded=True,
            model_id=model_service.model_id or settings.model_id,
            device=model_service.device,
        )
    return JSONResponse(
        status_code=503,
        content=HealthResponse(
            status="model_not_ready",
            model_loaded=False,
            model_id=settings.model_id,
            device=model_service.device or settings.model_device,
        ).model_dump(),
    )


@app.post("/analyze", response_model=AnalysisResponse)
async def analyze(image: UploadFile = File(...)) -> AnalysisResponse | JSONResponse:
    settings = get_settings()

    if not model_service.loaded:
        return JSONResponse(
            status_code=503,
            content={
                "detail": "Model is not loaded",
                "error": model_service.load_error or "model_not_ready",
            },
        )

    try:
        data = await image.read()
    except Exception:
        logger.exception("Failed to read upload")
        return JSONResponse(
            status_code=400,
            content={"detail": "Could not read uploaded file"},
        )

    validation = validate_image(data, image.content_type, settings)
    if not validation.usable or validation.image is None:
        return AnalysisResponse(
            image_quality=ImageQuality(usable=False, issues=validation.issues),
            areas=[],
            overall_summary="Imagem não pôde ser utilizada para análise visual.",
            limitations=[
                "Análise baseada apenas em imagem; não substitui vistoria técnica presencial.",
                "Imagem rejeitada na validação prévia.",
            ],
        )

    try:
        return model_service.analyze(validation.image, settings)
    except ModelNotReadyError:
        return JSONResponse(
            status_code=503,
            content={"detail": "Model is not loaded"},
        )
    except Exception:
        logger.exception("Unhandled analysis failure")
        return JSONResponse(
            status_code=500,
            content={"detail": "Analysis failed due to an internal error"},
        )
