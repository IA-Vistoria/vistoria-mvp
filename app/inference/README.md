# inference — serviço de inferência visual

Módulo autocontido de análise de imagem por VLM. No protótipo Vistor.IA ele sobe como **container separado** no [`docker-compose.yml`](../docker-compose.yml) da raiz do repositório.

Fluxo da plataforma:

```
Cliente → Frontend → Backend (auth / DB) → inference (GPU)
```

O cliente final não acessa a VLM. O consumo é exclusivo do backend, via `VLM_URL` e `VLM_API_KEY` (a mesma chave vira `API_KEY` neste container).

```
vistoria-predial/
├── docker-compose.yml         # backend + frontend + inference
├── inference/                 # este módulo
│   ├── deploy/
│   ├── app/
│   └── README.md
├── frontend/
└── src/                       # backend Spring
```

**Subir tudo (recomendado):** na raiz do repositório, `docker compose up --build -d`, depois `curl http://127.0.0.1:8001/health`.

**Só a IA (opcional):** `docker compose up --build -d` neste diretório (`inference/docker-compose.yml`).

**Documentação de deploy (Oracle Cloud):** [`deploy/oracle-cloud.md`](deploy/oracle-cloud.md)

**Contexto do produto:** [`../AGENTS.md`](../AGENTS.md) · [`../README.md`](../README.md)

**Testes unitários (sem GPU):**

```bash
cd inference
pip install Pillow==12.3.0 pydantic==2.13.5 python-dotenv==1.2.3
python -m unittest tests.test_validation -v
```

Alterações de contrato/escopo devem atualizar a documentação do produto na raiz.
---

## O que este serviço faz

| Endpoint | Auth | Função |
|---|---|---|
| `GET /health` | pública | Modelo carregado? Pronto? |
| `POST /analyze` | **API key** | `multipart/form-data` campo `image` → JSON |

Contrato: [`docs/contrato-api-vistoria-ia.json`](docs/contrato-api-vistoria-ia.json)

---

## Modelo e versões fixas

| Critério | Valor |
|---|---|
| Modelo | `Qwen/Qwen3-VL-2B-Instruct` (Apache 2.0) |
| Inferência | Transformers (carga **uma vez** na subida) |
| VRAM tipica | ~4–6 GB BF16 |
| Shape OCI recomendado | **`VM.GPU.A10.1`** (1× A10, 24 GB VRAM) |
| Docker base | `pytorch/pytorch:2.6.0-cuda12.4-cudnn9-runtime` |
| transformers / FastAPI | `4.57.6` / `0.141.1` |

---

## Resumo rápido — Oracle Cloud (SaaS)

1. **VCN** com subnet **privada** para a VLM + NAT Gateway (baixar modelo/Docker).
2. Instância **`VM.GPU.A10.1`**, Ubuntu 22.04/24.04, disco ≥ 100 GB, **sem** abrir 8001 para a internet.
3. NSG: porta **8001 só do CIDR do backend**; SSH via Bastion.
4. Na VM:

```bash
cd inference
sudo bash deploy/vm-bootstrap.sh   # reboot se instalar driver
cp .env.example .env               # API_KEY forte + REQUIRE_API_KEY=true
docker compose up --build -d
curl http://127.0.0.1:8001/health
```

5. No backend (mesma VCN):

```env
VLM_URL=http://10.0.2.X:8001
VLM_API_KEY=<mesma API_KEY do .env da VLM>
```

Detalhes de rede, security list, Bastion e checklist multi-tenant: **[`deploy/oracle-cloud.md`](deploy/oracle-cloud.md)**.

### Demo com link HTTPS público (opcional)

Só para demonstração — não é o desenho SaaS de produção:

```bash
# TUNNEL_TOKEN no .env (Cloudflare Zero Trust)
docker compose --profile tunnel up -d
```

Ou Caddy/TLS: [`deploy/Caddyfile.example`](deploy/Caddyfile.example).

---

## Configuração (`.env`)

```env
MODEL_ID=Qwen/Qwen3-VL-2B-Instruct
MODEL_DEVICE=cuda
MAX_IMAGE_SIZE_MB=15
PORT=8001
LOAD_IN_4BIT=false
MAX_NEW_TOKENS=1024

API_KEY=<segredo-forte>          # gere: python3 -c "import secrets; print(secrets.token_urlsafe(32))"
REQUIRE_API_KEY=true
CORS_ORIGINS=*                   # SaaS: backend server-to-server; restrinja se o browser chamar
```

---

## Testes

```bash
# Na VM / local
curl http://127.0.0.1:8001/health
curl -X POST http://127.0.0.1:8001/analyze \
  -H "X-API-Key: $API_KEY" \
  -F "image=@foto.jpg"

python scripts/test_upload.py foto.jpg --api-key "$API_KEY"
python -m unittest tests.test_validation -v
```

Página local: http://localhost:8001/static/upload.html

---

## Segurança SaaS (checklist)

- [ ] Frontend **nunca** tem `VLM_API_KEY`
- [ ] Backend autentica o tenant **antes** de chamar `/analyze`
- [ ] VLM em subnet privada; 8001 só para o backend
- [ ] `REQUIRE_API_KEY=true` + chave longa
- [ ] Rate limit / fila no backend (protege a GPU)
- [ ] Um único worker Uvicorn (não multiplique processos na mesma GPU)

---

## Linguagem da IA

Sempre **indício visual**, nunca diagnóstico técnico definitivo.

- ❌ "Existe infiltração estrutural."
- ✅ "Há sinais visuais compatíveis com possível umidade."
