# Vistor.IA

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-16.3.5-000000?style=for-the-badge&logo=nextdotjs)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Licença](https://img.shields.io/badge/licen%C3%A7a-n%C3%A3o%20definida-lightgrey?style=for-the-badge)

> Documentação técnica alinhada ao item **3.5** do edital Tech4Change 2026 (repositório e README).

## 1. Descrição da solução

**Vistor.IA** (VistoriA) é um assistente que conduz a vistoria de apartamento **cômodo por cômodo**, em conversa com o usuário: a IA orienta o que fotografar, comenta a qualidade das fotos, destaca possíveis defeitos e monta um checklist para conferência no local — com formalização em relatório.

### Jornada proposta (visão do produto)

| Etapa | O que acontece |
| --- | --- |
| **1. Fotografar** | Checklist guiado por cômodo indica o que fotografar em cada ambiente. A IA conversa com o usuário (ex.: “sem pressa; aviso se a foto sair escura ou tremida”) e pede ângulos críticos (cantos, rodapés, tomadas, batentes). |
| **2. Detectar** | A IA analisa as fotos e identifica indícios de rachadura, mofo, infiltração, falta de acabamento e outros pontos de atenção, já anotando no checklist. |
| **3. Verificar** | Conferência ponto a ponto ainda no imóvel: itens OK na foto, a checar ou com alerta; o que exige inspeção especializada (ex.: elétrica/hidráulica) fica marcado como fora do alcance da análise por imagem. |
| **4. Formalizar** | Relatório com fotos e descrições (PDF), pronto para envio à construtora em um clique. |

Na demonstração do MVP conceitual, o fluxo aparece em três momentos: **onboarding** (explica o processo antes da primeira foto), **captura guiada** (assistente pede foto a foto e comenta o que encontrou) e **checklist / relatório** (itens derivados das evidências, com status e próximos passos no local).

A decisão técnica e a homologação final permanecem *Human-in-the-Loop*: a IA sugere; o usuário (e, no fluxo técnico, o engenheiro) confirma.

### Escopo desta entrega (prova de conceito)

O repositório atual é uma **validação técnica / PoC**, não o produto final com a interface conversacional das demos acima. O código demonstra cadastro, protocolo de evidências, upload, pré-análise por IA e revisão humana — o suficiente para provar o conceito ponta a ponta.

O programa completo (conversa guiada cômodo a cômodo, tema visual e experiência das telas de demonstração) será desenvolvido **após a infraestrutura no ambiente Oracle**. O tema da interface também será revisto. A IA de produção prevista é 100% Oracle (**OCI Vision** + **LLM / OCI Generative AI**); na PoC local usa-se VLM self-hosted ou mock.

### O que a PoC técnica já cobre

- Cadastro/login (cliente e engenheiro) e proteção por perfil
- Protocolo de evidências com upload validado de imagens
- Pré-laudo preliminar por IA (VLM local ou mock) e fila de revisão humana
- Devolução / aprovação com parecer e reenvio pelo cliente
- Execução local via Docker Compose (frontend, backend e `inference`)

### Interface atual da PoC (referência técnica)

As telas abaixo são da implementação técnica atual — **não** representam ainda o tema nem a timeline conversacional da solução proposta.

**Cliente — protocolo de evidências**

![Protocolo guiado do cliente](.specs/features/frontend-redesign/evidence/02-cliente-protocolo-1440.png)

**Engenheiro — revisão do pré-laudo**

![Área de revisão técnica](.specs/features/frontend-redesign/evidence/04-engenheiro-revisao-1440.png)

## 2. Tecnologias, linguagens e frameworks utilizados

| Tecnologia | Versão | Responsabilidade |
| --- | --- | --- |
| Java | 21 | Linguagem do backend |
| Spring Boot | 3.2.3 | API REST, injeção de dependências e configuração |
| Spring Security + JJWT | 6.x / 0.12.6 | Autenticação stateless e autorização por perfil |
| Spring Data JPA | 3.2.x | Persistência e consultas |
| Flyway | 9.22.x + módulo PostgreSQL 10.8.1 | Evolução versionada do schema |
| PostgreSQL | 16 | Banco-alvo (Testcontainers / destino Oracle) |
| H2 | gerenciado pelo Spring Boot | Execução local rápida e parte dos testes |
| Next.js | 16.3.5 | Frontend React com App Router |
| React | 19.2.8 | Componentes e estado da interface |
| TypeScript | 5.x | Contratos e segurança de tipos no frontend |
| Python / FastAPI | 3.x / stack do `inference` | Serviço de IA da PoC (VLM self-hosted) |
| Docker Compose | — | Orquestração local de frontend, backend e inference |
| Vitest | 4.1.11 | Testes unitários e de componentes do frontend |
| JUnit 5, Mockito e Testcontainers | gerenciados pelo Maven | Testes unitários e integrados do backend |

## 3. Arquitetura geral do sistema

O backend segue um monólito modular por feature. A borda HTTP delega para casos de uso, entidades não são expostas nos contratos e integrações de infraestrutura ficam atrás de abstrações. No protótipo, frontend, API e IA sobem no mesmo `docker-compose.yml`, em containers separados.

```text
Cliente / Engenheiro
        │
        ▼
   Next.js (:3000)
        │  REST + JWT
        ▼
 Spring Boot (:8080)
   ├── H2 / PostgreSQL
   ├── StorageService (arquivos locais)
   └── IaIntegrationService
         ├── mock  → MockIaIntegrationService
         └── vlm   → VlmIntegrationService → inference VLM (:8001)
```

**Destino Oracle Cloud:** a porta de IA passa a consumir **OCI Vision** + **LLM da Oracle (OCI Generative AI)**; persistência e arquivos migram para PostgreSQL gerenciado e OCI Object Storage.

```text
src/main/java/br/com/vistoriapredial/
├── usuario/          # Cadastro, login, perfis
├── vistoria/         # Protocolo, evidências, fila Human-in-the-Loop
├── storage/          # Porta e adaptador de armazenamento
├── integration/vlm/  # Cliente da VLM (PoC)
├── config/security/  # JWT, CORS e Spring Security
└── shared/web/error/ # ProblemDetail (RFC 9457)

frontend/src/
├── app/                        # Rotas públicas e áreas cliente/engenharia
├── features/auth/              # Sessão e proteção de perfil
└── features/inspections/       # Jornadas de cliente e engenheiro

inference/                      # PoC: FastAPI + VLM (substituída pela OCI no destino)
```

Detalhamento: [`docs/architecture.md`](docs/architecture.md).

## 4. APIs, modelos de Inteligência Artificial e bases de dados utilizadas

| Camada | Nesta PoC | Destino (Oracle Cloud) |
| --- | --- | --- |
| API da aplicação | REST própria sob `/api` | Mesma API, hospedada no ambiente OCI |
| Modelos / serviços de IA | VLM `Qwen/Qwen3-VL-2B-Instruct` no container `inference` (ou mock) | **OCI Vision** + **LLM da Oracle / OCI Generative AI** (IA 100% Oracle) |
| Bases de dados | H2 em memória no perfil local/Docker | PostgreSQL gerenciado no ambiente Oracle |
| Armazenamento de imagens | Sistema de arquivos local (`StorageService`) | OCI Object Storage |

### Endpoints principais da API

Todas as rotas usam o prefixo `/api`.

| Método | Endpoint | Perfil | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Público | Cadastra cliente; engenheiro exige convite válido |
| `POST` | `/api/auth/login` | Público | Autentica e retorna JWT |
| `POST` | `/api/vistorias` | Cliente | Cria rascunho com endereço |
| `GET` | `/api/vistorias/minhas` | Cliente | Lista as vistorias do usuário (`?page=&size=`) |
| `GET` | `/api/vistorias/{id}` | Cliente/Engenheiro | Busca uma vistoria específica |
| `POST` | `/api/vistorias/{id}/imagens` | Cliente | Envia evidência multipart |
| `POST` | `/api/vistorias/{id}/submeter` | Cliente | Envia ou reenvia para análise |
| `GET` | `/api/vistorias/{id}/imagens/{imagemId}/conteudo` | Cliente/Engenheiro | Entrega evidência |
| `GET` | `/api/vistorias/pendentes` | Engenheiro | Lista a fila técnica |
| `POST` | `/api/vistorias/{id}/analisar` | Engenheiro | Aprova ou devolve com parecer |

Erros usam `application/problem+json` (RFC 9457).

## 5. Instruções para instalação ou execução

### Pré-requisitos

- Java 21 e Node.js 20+ (desenvolvimento local sem Docker)
- Docker (compose do protótipo: frontend + backend + inference)
- NVIDIA Container Toolkit para a VLM com GPU; sem GPU use `APP_IA_PROVIDER=mock` ou `MODEL_DEVICE=cpu`

### Desenvolvimento local, sem Docker

O perfil padrão usa H2 em memória e armazenamento local em `uploads/`.

```powershell
# terminal 1 — backend em http://localhost:8080
$env:JWT_SECRET = "defina-um-segredo-local-com-pelo-menos-32-bytes"
$env:ENGINEER_REGISTRATION_CODE = "defina-um-convite-para-engenheiros"
.\mvnw.cmd spring-boot:run

# terminal 2 — frontend em http://localhost:3000
cd frontend
npm ci
npm run dev
```

O frontend usa `http://localhost:8080/api` por padrão.

### App + IA em Docker (PoC)

O compose sobe `inference` (`:8001`), `backend` (`:8080`) e `frontend` (`:3000`).

```powershell
copy .env.example .env
docker compose up --build -d
curl http://127.0.0.1:8001/health
```

- UI: http://localhost:3000
- API: http://localhost:8080/api
- IA: http://localhost:8001/health (`model_loaded=true` antes de submeter vistoria)

Jornada de teste: cadastrar cliente → criar vistoria → enviar fotos → submeter → cadastrar engenheiro com `ENGINEER_REGISTRATION_CODE` → abrir a fila e conferir o pré-laudo.

Detalhes da VLM local: [`inference/README.md`](inference/README.md). O backend exige `JWT_SECRET` com pelo menos 32 bytes.

### Testes

```powershell
# backend
.\mvnw.cmd test

# frontend
cd frontend
npm test -- --run
npm run lint
npm run build
```

A suíte de backend usa Testcontainers; o teste PostgreSQL requer Docker.

## 6. Integrantes da equipe e suas respectivas contribuições

| RM | Nome | Contribuição |
| --- | --- | --- |
| rm376917 | Cleivin de Moura Lauermann | Ambiente de IA do MVP e treinamento |
| rm371636 | Vinícius de Oliveira Gonçalves | Frontend e backend |
| rm376236 | João Batista Santana de Moraes | Infraestrutura e idealização do produto |

## 7. Limitações conhecidas e próximos passos

### Limitações conhecidas

- Esta entrega é uma **prova de conceito**. A IA em produção prevista é da Oracle (**OCI Vision** + **LLM / OCI Generative AI**); o container `inference` com VLM self-hosted existe só para demonstrar o fluxo Human-in-the-Loop localmente.
- O ambiente Oracle Cloud ainda **não está configurado**; não há conexão ativa com Vision, LLM, Object Storage nem banco gerenciado da OCI.
- O armazenamento ativo é local. A abstração `StorageService` permite trocar o adaptador, mas a integração com OCI Object Storage ainda não existe nesta versão.
- O convite de engenharia é um controle administrativo do MVP, não uma validação automática do CREA.
- O repositório não declara uma licença de uso.

### Próximos passos

- Configurar o ambiente Oracle Cloud (rede, secrets, Object Storage e banco) — ainda não provisionado
- Substituir a VLM local pela stack de IA da Oracle: **OCI Vision** (análise de imagens) e **LLM da Oracle / OCI Generative AI** (texto do pré-laudo)
- Conectar o backend aos serviços OCI (credenciais, endpoints e adaptadores no lugar de `inference` / `APP_IA_PROVIDER=vlm`)
- Validar o fluxo ponta a ponta no cloud (submissão → pré-laudo Oracle → revisão do engenheiro) fora do compose local
