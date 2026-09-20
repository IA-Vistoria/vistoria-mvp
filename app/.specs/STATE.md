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
- **Status**: active

### AD-003
- **Decision**: Empregar um único modelo multimodal via OCI Generative AI diretamente para visão e texto, numa chamada só.
- **Reason**: Elimina a necessidade e o custo de treinar um modelo de visão computacional customizado do zero, recebendo foto e prompt técnico na mesma chamada.
- **Trade-off**: Dependência de conectividade, latência e disponibilidade do modelo no catálogo da OCI.
- **Scope**: Módulo de geração de laudos técnicos.
- **Date**: 2026-09-18
- **Status**: active
- **Nota de revalidação (2026-09-19)**: o modelo citado originalmente (Gemini) nunca chegou a ser usado; a spec do time (repositório `infra`) mudou para `meta.llama-3.2-90b-vision-instruct` e, depois, esse também foi descontinuado pela OCI. Modelo vigente a confirmar antes de implementar: `meta.llama-4-scout-17b-16e-instruct` — ver `.specs/features/integracao-oci/spec.md`. Não fixar nome de modelo em código; a OCI aposenta modelos com frequência.

### AD-004
- **Decision**: Arquitetura Human-in-the-Loop com papéis distintos (ROLE_CLIENTE e ROLE_ENGENHEIRO).
- **Reason**: A IA atua gerando um pré-laudo preliminar, mas a homologação final e responsabilidade técnica perante o cliente pertencem ao engenheiro civil.
- **Trade-off**: Necessidade de fluxo em duas etapas (análise preliminar vs homologação final).
- **Scope**: Segurança, domínio de vistorias e frontend.
- **Date**: 2026-09-18
- **Status**: active

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

## Handoff

- **Feature**: .specs/features/integracao-oci
- **Phase / Task**: Specify — spec, design e tasks criados nesta revisão; nenhuma tarefa (T1–T7) iniciada
- **Completed**: `.specs/features/gestao-vistorias` revalidada por completo (spec, design e tasks agora batem com o código); `.specs/features/autenticacao-e-acesso/spec.md` com a tabela de rastreabilidade sincronizada com `tasks.md`
- **In-progress**: nenhuma tarefa de `integracao-oci` em andamento
- **Next step**: T1 de `integracao-oci/tasks.md` — validar contra o ambiente real (scripts de `infra/scripts/smoke-tests/`) o modelo vigente e o formato de payload multimodal, antes de escrever `OciGenAiIntegrationService`
- **Blockers**: Generative AI bloqueado por limite da conta trial da OCI (ver `infra/README.md`); não bloqueia T2/T3/T6, só a validação real de T4
- **Uncommitted files**: ver `git status` no momento da leitura — este documento não substitui a checagem real
- **Branch**: ver `git branch` no momento da leitura
