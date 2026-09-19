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

## Handoff

- **Feature**: .specs/features/gestao-vistorias
- **Phase / Task**: Phase 1 - Specify
- **Completed**: spec.md gerado e validado
- **In-progress**: Aguardando aprovação do spec.md pelo usuário
- **Next step**: Discutir open questions ou mover para Design/Tasks
- **Blockers**: none
- **Uncommitted files**: yes (spec.md)
- **Branch**: main
