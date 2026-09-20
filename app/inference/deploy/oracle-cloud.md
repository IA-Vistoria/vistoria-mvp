# Deploy na Oracle Cloud (OCI) — SaaS

Este guia assume um produto **SaaS de vistoria**: o cliente final **nunca** fala direto com a VLM. Só o **backend** da plataforma chama esta API.

```
Clientes (browser/app)
        │
        ▼
   Frontend (HTTPS)
        │
        ▼
   Backend (auth, tenants, DB)     ← colega sobe depois
        │  VLM_URL + VLM_API_KEY
        ▼
   inference (esta pasta)             ← GPU na Oracle
```

---

## Arquitetura recomendada na OCI

| Camada | Onde sobe | Rede |
|---|---|---|
| Frontend | Object Storage + CDN, ou VM barata / Always Free | Pública (HTTPS) |
| Backend | VM Always Free (Ampere) ou `VM.Standard.E*` | Pública (API) ou atrás de Load Balancer |
| **VLM (IA)** | **VM com GPU** | **Privada** (só backend alcança) |

### Por que a VLM fica privada?

- É um serviço caro (GPU) e sem login de usuário final.
- Em SaaS multi-tenant, quem autentica o cliente é o **backend**; a VLM só confia na `API_KEY` do backend.
- Reduz ataque, abuso de GPU e custo.

**Exposição pública da VLM** (Cloudflare Tunnel / IP aberto) só faz sentido em demo. Em SaaS de produção, use **IP privado** na mesma VCN.

---

## Shape recomendado (GPU)

| Shape | GPU | VRAM | Adequação |
|---|---|---|---|
| **`VM.GPU.A10.1`** (recomendado) | 1× NVIDIA A10 | **24 GB** | Folga para Qwen3-VL-2B em BF16; sobra margem |
| `VM.GPU.A10.2` | 2× A10 | 48 GB | Overkill neste MVP |
| Sem GPU (CPU) | — | — | Só teste; lento demais para SaaS |

> Always Free da Oracle **não inclui GPU**. A VM de IA é recurso pago. Backend/frontend podem ficar no Always Free.

Disco (Block Volume): **≥ 100 GB** boot (imagem Docker + pesos HF + logs).

Imagem OS: **Ubuntu 22.04** ou **24.04** (Canonical).

---

## Passo a passo OCI

### 1) Rede (VCN)

No Console OCI → **Networking → Virtual Cloud Networks**:

1. Crie (ou use) uma **VCN** com CIDR, ex.: `10.0.0.0/16`.
2. Subnets:
   - `public-subnet` (`10.0.1.0/24`) — backend / load balancer (com Internet Gateway).
   - `private-subnet` (`10.0.2.0/24`) — **VM da VLM** (com NAT Gateway para baixar imagens/modelo).
3. **NAT Gateway** na VCN (a VM privada precisa sair na internet para Docker Hub / Hugging Face).
4. **Service Gateway** (opcional, para Object Storage sem internet).

### 2) Security List / Network Security Group

Na subnet **privada** da VLM, permita **entrada**:

| Fonte | Porta | Motivo |
|---|---|---|
| CIDR do backend (ex. `10.0.1.0/24`) | **8001** | Só o backend chama `/analyze` |
| Seu IP (temporário) | **22** | SSH de administração |

**Não** abra 8001 para `0.0.0.0/0`.

Saída: permitir (NAT) HTTPS `443` para puxar imagens e pesos.

### 3) Criar a instância GPU

**Compute → Instances → Create**:

1. Name: `vistoria-vlm-prod`
2. Image: Ubuntu 22.04/24.04
3. Shape: **VM.GPU.A10.1** (Availability Domain onde a shape existe na sua região)
4. Networking: **private-subnet** (sem IP público)
5. SSH key: a sua chave pública
6. Boot volume: 100+ GB

Para SSH sem IP público, use uma destas:

- **Bastion** OCI (recomendado), ou
- Backend como jump host: `ssh -J ubuntu@BACKEND_IP ubuntu@VLM_PRIVATE_IP`

### 4) Bootstrap na VM

```bash
# via Bastion / jump
ssh ubuntu@<IP_PRIVADO_VLM>

git clone <URL_DO_REPO>.git
cd inference
sudo bash deploy/vm-bootstrap.sh
# se instalou driver NVIDIA: sudo reboot && reconecte
```

Confirme:

```bash
nvidia-smi
docker run --rm --gpus all nvidia/cuda:12.4.1-base-ubuntu22.04 nvidia-smi
```

### 5) Configurar `.env` (SaaS)

```bash
cp .env.example .env
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
nano .env
```

```env
MODEL_ID=Qwen/Qwen3-VL-2B-Instruct
MODEL_DEVICE=cuda
LOAD_IN_4BIT=false
PORT=8001

# Obrigatório: só o backend conhece esta chave
API_KEY=<cole_o_segredo>
REQUIRE_API_KEY=true

# Em SaaS a VLM é chamada server-to-server; CORS pode ficar restrito ou *
# Se algum dia o browser chamar a VLM (não recomendado), liste o domínio do app
CORS_ORIGINS=*
```

### 6) Subir o serviço

```bash
docker compose up --build -d
docker compose logs -f vistoria-ia

curl http://127.0.0.1:8001/health
```

Na mesma VCN, o backend usa:

```env
VLM_URL=http://10.0.2.X:8001
VLM_API_KEY=<mesma API_KEY>
```

Teste a partir do host do **backend** (ou Bastion), se a subnet da VLM for privada:

```bash
curl http://10.0.2.X:8001/health
curl -X POST http://10.0.2.X:8001/analyze \
  -H "X-API-Key: $VLM_API_KEY" \
  -F "image=@foto.jpg"
```

---

## Opções de “link público” (quando precisa)

| Cenário | Como |
|---|---|
| **SaaS produção** | Sem link público da VLM. Backend chama IP privado. |
| Demo / investidor | Cloudflare Tunnel (`docker compose --profile tunnel up -d`) |
| MVP rápido com IP público | Coloque a VM na public-subnet + NSG só do IP do backend + `API_KEY` |

O “link” que o SaaS mostra ao cliente é o do **frontend/backend**, não o da GPU.

---

## Checklist SaaS multi-tenant

- [ ] Frontend **nunca** recebe `VLM_API_KEY`
- [ ] Backend valida usuário/tenant **antes** de chamar `/analyze`
- [ ] Backend envia `X-API-Key` e trata `image_quality.usable=false`
- [ ] VLM sem banco/usuários — só imagem → JSON (já é o desenho atual)
- [ ] Rate limit / fila no **backend** (protege a GPU de abuso)
- [ ] Monitorar `/health` e disco do volume `hf_cache`
- [ ] Backup: a VLM é **stateless** (pesos re-baixam); segredo está no `.env` / Vault OCI

---

## Custos (ordem de grandeza)

- `VM.GPU.A10.1` é o principal custo (GPU sob demanda / reserved).
- Para economizar em horário morto: **stop** da instância (cobrança de GPU para).
- Preemptible/spot GPU: barato, mas a VM pode ser interrompida — ruim para SaaS 24/7.

---

## Troubleshooting OCI

| Problema | O que checar |
|---|---|
| Shape GPU indisponível | Outra AD/região; pedir limit increase |
| Sem `nvidia-smi` | Driver + reboot; imagem Ubuntu oficial |
| Não baixa o modelo | NAT Gateway na subnet privada; DNS |
| Backend não alcança `:8001` | NSG/Security List; mesma VCN; IP privado correto |
| 401 no `/analyze` | `API_KEY` igual nos dois lados; header `X-API-Key` |
