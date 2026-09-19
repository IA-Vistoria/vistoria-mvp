# Onboarding: conectando à infra real do VistoApto (ambiente dev)

Este documento complementa `spec-ambiente-dev.md` com os valores reais do
ambiente já provisionado (região, OCIDs, nomes) e com scripts prontos pra
testar cada serviço da OCI isoladamente, **antes** de rodar a aplicação
inteira. Segue o mesmo princípio da spec: o dev nunca precisa da VM pra
desenvolver, só do próprio `docker compose up` autenticado contra o ambiente
compartilhado.

## 1. Acesso ao grupo OCI (feito pelo admin)

O administrador do projeto adiciona o usuário OCI da pessoa ao grupo
`vistoria-mvp-devs` (Console → Identity & Security → Domains → Groups →
`vistoria-mvp-devs` → Add User). Sem isso, nenhuma chamada abaixo funciona,
mesmo com a chave de API certa.

## 2. Gerar sua chave de API pessoal

Console → ícone de perfil (canto superior direito) → **User settings** →
**API keys** → **Add API key** → **Generate API Key Pair**. Baixe a chave
privada e mova pra fora de qualquer pasta de projeto:

```bash
mkdir -p ~/.oci
mv ~/Downloads/<sua-chave>.pem ~/.oci/vistoria-mvp-api-key.pem
chmod 600 ~/.oci/vistoria-mvp-api-key.pem
```

A OCI mostra um bloco `[DEFAULT] user=... fingerprint=... tenancy=...
region=...` — copie pra `~/.oci/config`, ajustando `key_file` pro caminho
acima. **Esse arquivo nunca vai pro git** (já está no `.gitignore` do
projeto).

## 3. Valores do ambiente compartilhado (não sensíveis)

Vêm do output do Terraform (`terraform output` em `terraform/environments/dev`)
e não mudam por pessoa:

| Variável | Valor |
|---|---|
| Região | `sa-saopaulo-1` |
| Compartment OCID | `ocid1.compartment.oc1..aaaaaaaaogefvhjugrkowz3p5bbtqjvypatdq4c7qtqebn2ef4bciyi44xuq` |
| Bucket de fotos | `vistoria-fotos` |
| Autonomous DB (TNS aliases) | `vistoriadb_high`, `vistoriadb_medium`, `vistoriadb_low` |

## 4. ⚠️ Atualização importante: o modelo da spec-backend.md foi descontinuado

`docs/spec-backend.md` especifica `meta.llama-3.2-90b-vision-instruct`. Ao
testar de verdade, esse modelo **não aparece mais no Playground** do console
(só no catálogo antigo da API `ListModels`, que lista modelos já fora de
serviço). Chamadas de chat contra ele retornam `404 Entity not found`.

**Modelo vigente a usar**: `meta.llama-4-scout-17b-16e-instruct` — é
nativamente multimodal (recebe imagem + texto, não precisa mais de um
`-vision` separado) e aparece ativo no Playground. Antes de codificar
`OciGenAiIntegrationService`, rode `list_available_models.py` (abaixo) pra
confirmar o modelo vigente no momento, já que a OCI aposenta modelos com
frequência.

## 5. Testando cada serviço isoladamente

Scripts em `scripts/smoke-tests/` (nenhum segredo hardcoded, tudo via
variável de ambiente):

```bash
pip install -r scripts/smoke-tests/requirements.txt
```

### Object Storage

```bash
OCI_COMPARTMENT_ID=ocid1.compartment.oc1..aaaaaaaaogefvhjugrkowz3p5bbtqjvypatdq4c7qtqebn2ef4bciyi44xuq \
OCI_BUCKET_NAME=vistoria-fotos \
python scripts/smoke-tests/test_object_storage.py
```
Sobe um objeto pequeno, lê de volta, apaga. Validado ✅ em 2026-09-19.

### Generative AI

```bash
# 1. Descobrir o OCID do modelo vigente (muda com o tempo)
OCI_COMPARTMENT_ID=ocid1.compartment.oc1..aaaaaaaaogefvhjugrkowz3p5bbtqjvypatdq4c7qtqebn2ef4bciyi44xuq \
python scripts/smoke-tests/list_available_models.py

# 2. Testar o chat com o OCID encontrado
OCI_COMPARTMENT_ID=ocid1.compartment.oc1..aaaaaaaaogefvhjugrkowz3p5bbtqjvypatdq4c7qtqebn2ef4bciyi44xuq \
GENAI_MODEL_ID=<ocid-do-modelo> \
python scripts/smoke-tests/test_genai_chat.py
```

Se der `429 throttled`: é limite de requisições da conta trial, não erro de
config — espere um pouco e tente de novo. Se der `404 not found`: o modelo
que você escolheu foi descontinuado, rode `list_available_models.py` de novo
e confira contra o Playground do console.

### Autonomous Database (wallet)

Peça ao administrador, por um canal seguro (gerenciador de senhas — **nunca**
chat ou e-mail):
- o arquivo do wallet (`vistoria-mvp-db-wallet.zip`)
- a senha do wallet
- usuário e senha do banco

Descompacte o wallet (não aponte pro `.zip` direto) e rode:

```bash
unzip vistoria-mvp-db-wallet.zip -d ./wallet   # ./wallet já está no .gitignore

WALLET_DIR=./wallet \
DB_USER=ADMIN \
DB_PASSWORD=<senha-do-banco> \
WALLET_PASSWORD=<senha-do-wallet> \
DB_DSN=vistoriadb_high \
python scripts/smoke-tests/test_autonomous_db.py
```
Validado ✅ em 2026-09-19.

## 6. Por que a conexão com o banco não muda entre laptop e VM

A conexão com a Autonomous Database é sempre via wallet (usuário/senha +
mTLS) — isso é **igual** no laptop do dev e na VM de produção do MVP, não
depende de instance principal nem de API key. Só a autenticação com Object
Storage e Generative AI muda entre os dois ambientes, e isso já é resolvido
pelo `OCI_AUTH_MODE` no próprio código (ver `spec-backend.md`, seção de
autenticação) — nenhum código muda, só a variável de ambiente.

## 7. Rodando a aplicação de verdade

Depois dos testes acima passarem, siga `spec-ambiente-dev.md` a partir do
passo 3 (clonar `vistoria-predial`, copiar `.env.example` → `.env` com os
valores da seção 3 deste documento, `docker compose up`).
