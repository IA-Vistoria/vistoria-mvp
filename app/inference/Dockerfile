# Base image: PyTorch 2.6.0 + CUDA 12.4.1 + cuDNN 9 (exact tag, never latest).
# Includes Python 3.11 from the official pytorch conda env.
FROM pytorch/pytorch:2.6.0-cuda12.4-cudnn9-runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    HF_HOME=/models/hf_cache \
    TRANSFORMERS_CACHE=/models/hf_cache \
    PORT=8001

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY static ./static
COPY .env.example ./.env.example

EXPOSE 8001

# Model is loaded once at process start (FastAPI lifespan).
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
