# Vistor.IA (VistorIA)

![Demonstração do protótipo navegável](prototipo/screenshots/onboarding-captura-checklist.png)

**Vistor.IA** conduz o registro de um imóvel ambiente por ambiente e transforma
fotos em um **Relatório de vistoria por IA**. A análise sugere indícios, o
usuário confirma o contexto e o sistema mantém cada constatação ligada à
evidência original.

🔗 **[Protótipo navegável](https://ia-vistoria.github.io/vistoria-mvp/prototipo/VistorIA-prototipo-navegavel.html)**

## Status atual do projeto

- ✅ Jornada principal implementada em `app/`: acesso, imóvel, captura guiada, análise assíncrona, revisão e relatório.
- ✅ Interface responsiva e relatório preparado para impressão ou PDF pelo navegador.
- ✅ Uso da IA para avaliação de fotos validado localmente (prova de conceito técnica).
- ✅ Infraestrutura de nuvem provisionada na Oracle (rede, banco de dados, armazenamento de imagens).
- ⏳ Generative AI (IA/Vision) da Oracle: ainda em validação — pendência de cota da conta.
- ⏳ Instância (VM) da aplicação: aguardando liberação de capacidade da Oracle (limite do provedor).

## Estrutura do repositório

```
.
├── prototipo/   # Referência histórica do discovery
├── app/         # MVP executável: frontend, backend, persistência e integração de IA
└── infra/       # Terraform, Ansible e o ambiente real na OCI
```

`prototipo/` preserva o material de discovery. A fonte atual do produto é
`app/`, com Next.js no frontend e Java/Spring Boot no backend. O fluxo antigo
de engenharia continua isolado apenas para compatibilidade; ele não é o
destino da jornada principal do MVP.

## 1. Descrição da solução

Vistorias de imóveis frequentemente ficam dispersas entre fotos e anotações.
O Vistor.IA organiza a captura em 12 itens, usa um modelo multimodal para
sugerir indícios visuais e pede que o usuário confirme, corrija ou rejeite
cada achado com contexto. O resultado é um registro rastreável; ele não é
laudo técnico, diagnóstico estrutural ou certificação profissional.

## 2. Tecnologias, linguagens e frameworks utilizados

| Camada | Tecnologia | Onde |
| --- | --- | --- |
| Protótipo de produto | HTML, CSS, JavaScript | `prototipo/` |
| Backend do MVP | Java 21, Spring Boot, Spring Security, JPA, Flyway | `app/` |
| Frontend do MVP | Next.js, React, TypeScript | `app/frontend/` |
| Análise visual | Porta de IA com mock local ou VLM `Qwen/Qwen3-VL-2B-Instruct` | `app/` |
| Persistência | PostgreSQL como alvo e H2 para desenvolvimento local | `app/` |
| Infraestrutura real | Terraform (`oracle/oci`), Ansible | `infra/` |
| Nuvem | OCI: Object Storage, Autonomous Database, Generative AI | `infra/` |

## 3. Arquitetura geral do sistema

### Infraestrutura provisionada na OCI

```mermaid
flowchart TD
    User["Usuário (navegador)"] --> VMBox

    subgraph VMBox["VM Ampere A1 — aguardando capacidade da Oracle"]
        Nginx["Nginx"]
        Frontend["Frontend<br/>(stack a definir)"]
        Backend["Backend<br/>(stack a definir)"]
        Nginx --> Frontend
        Nginx --> Backend
    end

    Backend --> ObjStorage["Object Storage"]
    Backend --> GenAI["Generative AI<br/>(multimodal)"]
    Backend --> ADB["Autonomous DB"]
```

### Rede e identidade (OCI)

```mermaid
flowchart TD
    A["Internet"] --> B["Internet Gateway"]
    B --> C["Tabela de rotas"]
    C --> D["Subnet pública"]
    D --> E["NSG — libera só 80, 443 e 22"]
    E --> F["VM"]
```

```mermaid
flowchart TD
    Dev["Dev (API key)"] --> GroupDevs["Grupo: vistoria-mvp-devs"]
    VM["VM (instance principal)"] --> DG["Dynamic group: vistoria-mvp-vm-dg"]
    GroupDevs --> Compartment["Compartment: vistoria-mvp-dev<br/>bucket, Autonomous DB, Generative AI"]
    DG --> Compartment
```

Detalhes: [`infra/README.md`](infra/README.md) · [`app/docs/architecture.md`](app/docs/architecture.md).

## 4. APIs, modelos de IA e bases de dados utilizadas

| Camada | No MVP (`app/`) | Ambiente OCI (`infra/`) |
| --- | --- | --- |
| Modelo de IA | VLM `Qwen/Qwen3-VL-2B-Instruct` self-hospedado | **OCI Generative AI** (`meta.llama-4-scout-17b-16e-instruct`) |
| Banco de dados | PostgreSQL / H2 | **Oracle Autonomous Database** (Always Free, 26ai) |
| Armazenamento | Sistema de arquivos local | **OCI Object Storage** |

Detalhes da validação do modelo de IA e scripts de teste isolado:
[`infra/docs/onboarding-backend-dev.md`](infra/docs/onboarding-backend-dev.md).

## 5. Instruções para instalação ou execução

> As instruções abaixo executam o MVP local com análise simulada. O protótipo
> histórico continua disponível no
> [link navegável](https://ia-vistoria.github.io/vistoria-mvp/prototipo/VistorIA-prototipo-navegavel.html).

**Sem Docker** (H2 em memória e análise mock):

```powershell
cd app
$env:JWT_SECRET = "defina-um-segredo-local-com-pelo-menos-32-bytes"
.\mvnw.cmd spring-boot:run    # backend em :8080

cd app/frontend
npm ci && npm run dev         # frontend em :3000
```

**Com Docker, IA real ligada** (requer GPU NVIDIA):

```powershell
cd app
copy .env.example .env
docker compose up --build -d
curl http://127.0.0.1:8001/health
```

Jornada de teste: criar conta → cadastrar imóvel → enviar fotos pelo roteiro →
submeter à IA → revisar cada achado → gerar, imprimir ou compartilhar o
Relatório de vistoria por IA. Detalhes da integração visual:
[`app/inference/README.md`](app/inference/README.md).

**Infraestrutura na OCI:**

```bash
cd infra/terraform/environments/dev
cp terraform.tfvars.example terraform.tfvars
terraform init && terraform plan && terraform apply
```

Passo a passo completo: [`infra/docs/spec-ambiente-dev.md`](infra/docs/spec-ambiente-dev.md).

## 6. Integrantes da equipe e suas respectivas contribuições

| RM | Nome | Contribuição |
| --- | --- | --- |
| rm376917 | Cleivin de Moura Lauermann | Ambiente de IA da prova de conceito técnica e treinamento |
| rm371636 | Vinícius de Oliveira Gonçalves | Frontend e backend da prova de conceito técnica |
| rm376236 | João Batista Santana de Moraes | Infraestrutura, idealização e discovery do produto |

## 7. Limitações conhecidas e próximos passos

- `infra/` e `app/` ainda não estão conectados.
- VM ainda não criada (capacidade Ampere A1 indisponível na OCI no momento).
- Generative AI: limite de conta trial resolvido após upgrade, revalidar antes de considerar pronto.
- Modelo de IA sem fine-tuning ou dataset próprio de defeitos de vistoria.
- PoC com IA local exige GPU NVIDIA.
- A área de engenharia é um fluxo legado e não integra a jornada principal do MVP.
- O relatório organiza evidências assistidas por IA e não substitui avaliação profissional quando ela for necessária.

**Próximos passos:** conectar o MVP à infraestrutura provisionada, validar a
integração de IA no ambiente-alvo e definir a estratégia de persistência na OCI.
