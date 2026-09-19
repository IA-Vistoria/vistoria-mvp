# Project Memory & State

## Decisions

### AD-001
- **Decision**: Adotar arquitetura de Monólito Modular em vez de Microserviços.
- **Reason**: Reduzir complexidade operacional, viabilizar execução limpa em container único na VM Ampere A1 (Always Free) e focar em excelência de camadas e regras de negócio com Spring Boot 4.1.1.
- **Trade-off**: Impossibilidade de escalar independentemente módulos específicos sem escalar a aplicação inteira.
- **Scope**: Toda a aplicação backend.
- **Date**: 2026-09-18
- **Status**: active

### AD-002
- **Decision**: Utilizar Spring RestClient nativo para integração HTTP com a API da Oracle (OCI).
- **Reason**: Dispensa dependências externas pesadas do Spring Cloud (como OpenFeign), mantendo código síncrono, fluente e moderno nativo do Spring Framework 6.
- **Trade-off**: Configuração manual de DTOs e URLs de integração.
- **Scope**: Pacote integration/oci.
- **Date**: 2026-09-18
- **Status**: active

### AD-003
- **Decision**: Empregar modelo multimodal (Gemini 2.5 Pro via OCI Generative AI) diretamente para visão e texto.
- **Reason**: Elimina a necessidade e o custo de treinar um modelo de visão computacional customizado do zero, recebendo foto e prompt técnico na mesma chamada.
- **Trade-off**: Dependência de conectividade e latência do serviço externo da Oracle/Google.
- **Scope**: Módulo de geração de laudos técnicos.
- **Date**: 2026-09-18
- **Status**: active

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

## Handoff

- **Feature**: .specs/features/gestao-vistorias
- **Phase / Task**: Implementing — T1 a T7 concluídas e testadas; ver `tasks.md` para o detalhamento revalidado
- **Completed**: Entidades, persistência, casos de uso (cliente e engenheiro), endpoints HTTP e migrations V1–V5; `spec.md` e `tasks.md` sincronizados com o código nesta revisão
- **In-progress**: nenhuma tarefa desta feature em andamento no momento
- **Next step**: avaliar os itens fora do escopo original de T1–T7 registrados em `tasks.md` (paginação das listagens; índices e transação curta na IA já corrigidos nesta revisão)
- **Blockers**: none
- **Uncommitted files**: ver `git status` no momento da leitura — este documento não substitui a checagem real
- **Branch**: ver `git branch` no momento da leitura (este campo ficou desatualizado antes por apontar para `main` enquanto o trabalho ocorria em uma branch dedicada)
