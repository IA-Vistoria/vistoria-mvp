# Project Memory & State

## Decisions

### AD-001
- **Decision**: Adotar arquitetura de Monólito Modular em vez de Microserviços.
- **Reason**: Reduzir complexidade operacional, viabilizar execução limpa em container único na VM Ampere A1 (Always Free) e focar em excelência de camadas e regras de negócio com Spring Boot 4.1.1.
- **Trade-off**: Impossibilidade de escalar independentemente módulos específicos sem escalar a aplicação inteira.
- **Scope**: Toda a aplicação backend.
- **Date**: 2026-09-18
- **Status**: active
- **Nota de revalidação (2026-09-19)**: `pom.xml` usa Spring Boot 3.2.3, não 4.1.1. A decisão de monólito modular continua válida; só o número de versão citado estava errado (ver `AGENTS.md`, seção 3).

### AD-002
- **Decision**: Utilizar Spring RestClient nativo para integração HTTP com a API da Oracle (OCI).
- **Reason**: Dispensa dependências externas pesadas do Spring Cloud (como OpenFeign), mantendo código síncrono, fluente e moderno nativo do Spring Framework 6.
- **Trade-off**: Configuração manual de DTOs e URLs de integração.
- **Scope**: Pacote integration/oci.
- **Date**: 2026-09-18
- **Status**: superseded for Generative AI only; active for direct HTTP integrations
- **Nota de revalidação (2026-09-30)**: a integração com OCI Generative AI usa o SDK oficial `oci-java-sdk-generativeaiinference`, necessário para assinatura, autenticação por `config_file`/`instance_principal`, serving mode e retries do serviço. Esta substituição não altera a preferência por `RestClient` nas integrações HTTP que não possuem adaptador SDK aprovado.

### AD-003
- **Decision**: Empregar um único modelo multimodal via OCI Generative AI diretamente para visão e texto, numa chamada só.
- **Reason**: Elimina a necessidade e o custo de treinar um modelo de visão computacional customizado do zero, recebendo foto e prompt técnico na mesma chamada.
- **Trade-off**: Dependência de conectividade, latência e disponibilidade do modelo no catálogo da OCI.
- **Scope**: Módulo de geração de laudos técnicos.
- **Date**: 2026-09-18
- **Status**: active
- **Nota de revalidação (2026-09-30)**: o adaptador usa Gemini por padrão no OCI Generative AI, mas o identificador permanece configurável por `OCI_GENAI_MODEL_ID`. Disponibilidade, região e cota devem ser confirmadas no smoke real; nenhum modelo alternativo ou mock é selecionado silenciosamente.

### AD-004
- **Decision**: Arquitetura Human-in-the-Loop com papéis distintos (ROLE_CLIENTE e ROLE_ENGENHEIRO).
- **Reason**: A IA atua gerando um pré-laudo preliminar, mas a homologação final e responsabilidade técnica perante o cliente pertencem ao engenheiro civil.
- **Trade-off**: Necessidade de fluxo em duas etapas (análise preliminar vs homologação final).
- **Scope**: Segurança, domínio de vistorias e frontend.
- **Date**: 2026-09-18
- **Status**: superseded by AD-008 for the primary MVP flow; retained only as legacy compatibility

### AD-005
- **Decision**: Decompor o monólito em entregas de valor vertical (features).
- **Reason**: Evitar tarefas massivas e não-atômicas; focar em fluxos completos e testáveis (Backend + Integrações + Validações).
- **Trade-off**: Overhead inicial de criação de documentação e pacotes isolados por sub-módulo.
- **Scope**: Planejamento e estrutura de features.
- **Date**: 2026-09-19
- **Status**: active

### AD-006
- **Decision**: Corrigir divergências entre `.specs/features/gestao-vistorias` e o código já implementado, em vez de tratar a feature como não iniciada.
- **Reason**: `spec.md` e `tasks.md` descreviam a feature como "Phase 1 - Specify" com todos os requisitos `Pending`, mas `VistoriaService`, `VistoriaController`, as migrations e a suíte de testes correspondente já estavam implementados e passando. Tratar a documentação como fonte de verdade nesse estado levaria a reimplementar algo que já existe.
- **Trade-off**: A tabela de rastreabilidade e o tracker de tarefas passam a exigir revalidação manual periódica contra o código para não voltar a divergir.
- **Scope**: `.specs/features/gestao-vistorias/spec.md` e `tasks.md`.
- **Date**: 2026-09-19
- **Status**: active

### AD-007
- **Decision**: Criar `.specs/features/integracao-oci/` para registrar, dentro deste repositório, o trabalho de trocar `LocalStorageService`/`MockIaIntegrationService` pelos adaptadores reais da OCI.
- **Reason**: Essa informação existia só como uma spec (`docs/spec-backend.md`) no repositório `infra`, de outra pessoa do time. Sem uma spec correspondente aqui, o próximo passo real do projeto ficava invisível para quem só lê `vistoria-predial`.
- **Trade-off**: Duas specs (`infra/docs/spec-backend.md` e `.specs/features/integracao-oci/`) descrevem o mesmo trabalho de ângulos diferentes; precisam ser revalidadas juntas quando uma mudar (ex.: modelo de IA descontinuado, mudança de bucket).
- **Scope**: Novo pacote `integration/oci/`, `pom.xml` (driver e Flyway do Oracle), migrations.
- **Date**: 2026-09-19
- **Status**: active

### AD-008
- **Decision**: Tornar a revisão de contexto pelo proprietário o Human-in-the-Loop do MVP e encerrar a jornada principal no Relatório de vistoria por IA, sem fila ou homologação de engenheiro.
- **Reason**: O objetivo atual do produto, confirmado pelo usuário, é a IA organizar evidências e gerar um relatório revisável pelo próprio responsável pelo imóvel. A experiência anterior tratava o engenheiro como núcleo do produto e desviava desse valor.
- **Trade-off**: O documento deixa de ter responsabilidade técnica profissional e precisa declarar explicitamente que registra apenas indícios visuais. Código, papéis, endpoints e colunas legadas de engenharia serão preservados temporariamente até uma migração destrutiva própria.
- **Scope**: Jornada principal, contratos de vistoria, estados, frontend e linguagem de produto.
- **Date**: 2026-09-30
- **Status**: active

### AD-009
- **Decision**: A conclusão visual do MVP é derivada deterministicamente do documento validado da IA e não pode ser sobrescrita pelo responsável pelo imóvel.
- **Reason**: O valor central do produto é entregar um resultado automatizado, explicável e rastreável por ambiente. A manifestação humana continua necessária como direito de contestação e contexto, não como aprovação obrigatória nem como fonte do resultado.
- **Trade-off**: O produto precisa separar visualmente resultado automatizado, limitações da análise e manifestação do usuário, além de evitar prometer responsabilidade técnica ou validade jurídica automática.
- **Scope**: Contrato canônico de análise, máquina de estados, API, revisão, relatório e linguagem de produto.
- **Date**: 2026-09-30
- **Status**: active

## Handoff

- **Feature**: `.specs/features/ia-laudo-autoral`
- **Phase / Task**: Validate — T1 a T19 implementadas; verificação independente pendente
- **Completed**: contrato v2 autoral, adaptador OCI via SDK oficial, conclusão automatizada por ambiente, manifestação separada, relatório explicável e jornada responsiva; 281 testes backend e 130 frontend verdes; lint, build, PostgreSQL e UAT local aprovados
- **In-progress**: verificação independente da feature e registro final em `validation.md`
- **Next step**: concluir a verificação independente e solicitar autorização explícita antes do push
- **Blockers**: smoke real da OCI não executado; depende de credenciais, cota disponível e autorização explícita para uma chamada externa com possível custo
- **Uncommitted files**: ver `git status` no momento da leitura — este documento não substitui a checagem real
- **Branch**: `codex/especifica-interface-ia-first`
