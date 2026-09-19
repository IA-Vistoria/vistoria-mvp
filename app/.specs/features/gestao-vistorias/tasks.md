# Gestão de Vistorias Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill.

**Design**: `.specs/features/gestao-vistorias/design.md`
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase (strong defaults applied, Spring Boot structure).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Service | unit | All branches; 1:1 to spec ACs; all listed edge cases | `src/test/java/**/*ServiceTest.java` | `./mvnw clean test` |
| Controller | integration | All routes: happy + edge + error | `src/test/java/**/*ControllerTest.java` | `./mvnw clean test` |
| Repository | integration | Key query paths | `src/test/java/**/*RepositoryTest.java` | `./mvnw clean test` |
| Entity / Config | none | - (build gate only) | - | `./mvnw clean test` |

## Gate Check Commands

> Generated from codebase.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After tasks with unit tests only | `./mvnw test` |
| Full | After tasks with e2e/integration tests | `./mvnw clean test` |
| Build | After phase completion | `./mvnw clean test` |

---

## Execution Plan

### Phase 1: Foundation (Entities & Repo)
```
T1 → T2
T1 → T3
```

### Phase 2: Core Business Logic (Service & IA Integration)
```
T2 → T4
T3 → T4
T4 → T5
```

### Phase 3: API endpoints (Controller)
```
T4 → T6
T5 → T7
T6 → T7
```

---

## Task Breakdown

### T1: [Criar Entidades Vistoria, VistoriaStatus e ImagemVistoria]
**What**: Definir as entidades JPA, Enum de status e mapeamento com o Usuário.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/domain/*`
**Depends on**: None
**Requirement**: VISTORIA-01

**Tools**: `filesystem`
**Done when**:
- [ ] Entidades mapeadas corretamente com `@Entity`.
- [ ] Relacionamentos configurados (`@ManyToOne` com `Usuario`).

**Tests**: none
**Gate**: Build

---

### T2: [Criar VistoriaRepository e ImagemVistoriaRepository]
**What**: Criar as interfaces do Spring Data JPA.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/persistence/*`
**Depends on**: T1
**Requirement**: VISTORIA-01

**Tools**: `filesystem`
**Done when**:
- [ ] Repositórios criados extendendo `JpaRepository`.
- [ ] Teste de persistência de Vistoria roda com sucesso no H2.

**Tests**: integration
**Gate**: Full

---

### T3: [Criar IaIntegrationService e DTOs de IA]
**What**: Serviço que abstrai a chamada (mock) da IA.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/IaIntegrationService.java`
**Depends on**: T1
**Requirement**: VISTORIA-02

**Tools**: `filesystem`
**Done when**:
- [ ] Interface e implementação (Mock/Dummy inicial) que recebe URLs de imagem e retorna uma string simulando o "Pré-laudo".
- [ ] Retorna falha condicional ou timeout para testar o cenário de erro.

**Tests**: unit
**Gate**: Quick

---

### T4: [Implementar VistoriaService - Fluxo do Cliente]
**What**: Lógica de criação, upload de imagens (usando `StorageService`) e submissão à IA.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java`
**Depends on**: T2, T3
**Requirement**: VISTORIA-01, VISTORIA-02

**Tools**: `filesystem`
**Done when**:
- [ ] Testes unitários validam a máquina de estados (`EM_RASCUNHO` -> `AGUARDANDO_IA` ou `FALHA_IA`).
- [ ] Faz uso do `StorageService` para salvar as imagens.

**Tests**: unit
**Gate**: Quick

---

### T5: [Implementar VistoriaService - Fluxo do Engenheiro]
**What**: Lógica para buscar vistorias pendentes, aprovar e assinar (adicionando ID do engenheiro).
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java`
**Depends on**: T4
**Requirement**: VISTORIA-03

**Tools**: `filesystem`
**Done when**:
- [ ] Testes unitários validam a transição `AGUARDANDO_ENGENHEIRO` -> `CONCLUIDA` ou `DEVOLVIDA_CLIENTE`.

**Tests**: unit
**Gate**: Quick

---

### T6: [Implementar Endpoints do Cliente em VistoriaController]
**What**: Criar endpoints REST, DTOs de requisição/resposta para criação, upload (Multipart) e submissão.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/web/`
**Depends on**: T4
**Requirement**: VISTORIA-01

**Tools**: `filesystem`
**Done when**:
- [ ] `VistoriaControllerTest` valida HTTP 201 e 200 com MockMvc.
- [ ] Segurança permite apenas perfil `ROLE_CLIENTE` acessar essas rotas.

**Tests**: integration
**Gate**: Full

---

### T7: [Implementar Endpoints do Engenheiro em VistoriaController]
**What**: Criar endpoints REST para listagem de pendentes e aprovação.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/web/`
**Depends on**: T5, T6
**Requirement**: VISTORIA-03

**Tools**: `filesystem`
**Done when**:
- [ ] `VistoriaControllerTest` valida HTTP 200 com MockMvc.
- [ ] Segurança permite apenas perfil `ROLE_ENGENHEIRO` acessar essas rotas.

**Tests**: integration
**Gate**: Full
