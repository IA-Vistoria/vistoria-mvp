"""Response contract schemas for the vistoria IA MVP."""

from __future__ import annotations

from typing import Literal
from uuid import uuid4

from pydantic import BaseModel, Field


Severity = Literal["baixa", "media", "alta"]
Confidence = Literal["baixa", "media", "alta"]


class ImageQuality(BaseModel):
    usable: bool
    issues: list[str] = Field(default_factory=list)


class AreaFinding(BaseModel):
    area: str
    issue_type: str
    description: str
    evidence: str
    severity: Severity
    confidence: Confidence
    recommendation: str
    location: str | None = None


class AnalysisResponse(BaseModel):
    analysis_id: str = Field(default_factory=lambda: str(uuid4()))
    image_quality: ImageQuality
    areas: list[AreaFinding] = Field(default_factory=list)
    overall_summary: str
    limitations: list[str] = Field(default_factory=list)


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_id: str
    device: str
