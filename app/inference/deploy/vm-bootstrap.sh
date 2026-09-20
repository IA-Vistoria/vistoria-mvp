#!/usr/bin/env bash
# Bootstrap mínimo de VM Ubuntu 22.04/24.04 com GPU NVIDIA para o serviço VLM.
# Rode como root (ou com sudo) na VM nova.
set -euo pipefail

echo "==> Atualizando pacotes"
apt-get update -y
apt-get install -y ca-certificates curl gnupg lsb-release git

echo "==> Instalando Docker Engine"
if ! command -v docker >/dev/null 2>&1; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
fi

echo "==> NVIDIA driver (se ainda não instalado)"
if ! command -v nvidia-smi >/dev/null 2>&1; then
  ubuntu-drivers autoinstall || apt-get install -y nvidia-driver-550
  echo "Reinicie a VM após instalar o driver: sudo reboot"
fi

echo "==> NVIDIA Container Toolkit"
if ! dpkg -l | grep -q nvidia-container-toolkit; then
  curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey \
    | gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
  curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list \
    | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' \
    > /etc/apt/sources.list.d/nvidia-container-toolkit.list
  apt-get update -y
  apt-get install -y nvidia-container-toolkit
  nvidia-ctk runtime configure --runtime=docker
  systemctl restart docker
fi

echo "==> Checagens"
docker --version
nvidia-smi || echo "AVISO: nvidia-smi falhou — reinicie a VM e rode de novo"
docker run --rm --gpus all nvidia/cuda:12.4.1-base-ubuntu22.04 nvidia-smi \
  || echo "AVISO: teste GPU no Docker falhou — confira driver + toolkit"

echo "Pronto. Clone o repo, entre em inference/, configure .env e rode: docker compose up --build -d"
