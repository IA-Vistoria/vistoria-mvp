# Integração OCI Tasks

**Design**: `.specs/features/integracao-oci/design.md`
**Status**: Draft — nenhuma tarefa iniciada nesta revisão

---

## Status Tracker

- [ ] **T1**: Validar formato real de request/response multimodal da OCI Generative AI (script isolado, fora do Spring Boot).
- [ ] **T2**: Implementar `OciAuthConfiguration`.
- [ ] **T3**: Implementar `OciObjectStorageService implements StorageService`.
- [ ] **T4**: Implementar `OciGenAiIntegrationService implements IaIntegrationService`.
- [ ] **T5**: Adicionar propriedades de configuração e alternância mock/real (`app.storage.provider`, `app.ia.provider`).
- [ ] **T6**: Migrar de PostgreSQL para Oracle Autonomous Database (driver, Flyway, tipos de coluna).
- [ ] **T7**: Atualizar `docker-compose.yml`/`Dockerfile` do backend para os dois ambientes.

---

## Task Breakdown

### T1: Validar contrato real da OCI Generative AI
**What**: Rodar os scripts de `infra/scripts/smoke-tests/` (`list_available_models.py`, `test_genai_chat.py`) contra o ambiente real para confirmar o modelo vigente e o formato de payload multimodal.
**Where**: Fora do código Java — só levantamento, sem alterar `vistoria-predial`.
**Depends on**: None
**Requirement**: OCI-02

**Done when**:
- [ ] Modelo vigente confirmado (não presumir `meta.llama-4-scout-17b-16e-instruct` sem checar de novo — a OCI aposenta modelos com frequência).
- [ ] Formato de request/response documentado em `design.md` desta feature.

**Tests**: none (levantamento manual)
**Gate**: nenhum — pré-requisito de T4.

---

### T2: `OciAuthConfiguration`
**What**: `@Configuration` decidindo entre `InstancePrincipalsAuthenticationDetailsProvider` e `ConfigFileAuthenticationDetailsProvider` a partir de `OCI_AUTH_MODE`.
**Where**: `src/main/java/br/com/vistoriapredial/integration/oci/`
**Depends on**: None
**Requirement**: OCI-03

**Done when**:
- [ ] Bean único de provider, injetado nos dois adaptadores reais.
- [ ] Teste unitário cobre os dois ramos (com e sem `OCI_AUTH_MODE`).

**Tests**: unit
**Gate**: Quick

---

### T3: `OciObjectStorageService`
**What**: Implementar `StorageService` contra o bucket real.
**Where**: `src/main/java/br/com/vistoriapredial/integration/oci/`
**Depends on**: T2
**Requirement**: OCI-01

**Done when**:
- [ ] `store`/`load`/`delete` funcionam contra um bucket de teste (ou fake do SDK).
- [ ] Falhas traduzidas para `StorageException`, preservando o contrato já usado por `VistoriaService`.

**Tests**: unit com fake/stub do cliente OCI; nenhum teste automatizado chama o Object Storage real (`AGENTS.md`, seção 10).
**Gate**: Quick

---

### T4: `OciGenAiIntegrationService`
**What**: Implementar `IaIntegrationService` chamando o modelo multimodal vigente.
**Where**: `src/main/java/br/com/vistoriapredial/integration/oci/`
**Depends on**: T1, T2
**Requirement**: OCI-02

**Done when**:
- [ ] Uma chamada por imagem, texto final composto em código a partir do JSON estruturado.
- [ ] `429`/`404`/timeout tratados sem fabricar pré-laudo.

**Tests**: unit com servidor mock/fake para payload, timeout e respostas inválidas (nenhuma chamada real em teste automatizado).
**Gate**: Quick

---

### T5: Alternância mock/real por configuração
**What**: Propriedades `app.storage.provider` (`local`|`oci`) e `app.ia.provider` (`mock`|`oci`) resolvendo o bean ativo de cada porta.
**Where**: `config/` ou `@ConditionalOnProperty` nos próprios adaptadores.
**Depends on**: T3, T4
**Requirement**: OCI-01, OCI-02

**Done when**:
- [ ] Com os valores default (`local`, `mock`), nenhum comportamento muda em relação ao estado atual.
- [ ] Teste de contexto confirma que o bean correto sobe para cada combinação de propriedade.

**Tests**: integração leve (`@SpringBootTest` com propriedades sobrescritas)
**Gate**: Full

---

### T6: Migração para Oracle Autonomous Database
**What**: Driver JDBC Oracle, módulo Oracle do Flyway, colunas `TEXT` → `CLOB`.
**Where**: `pom.xml`, `src/main/resources/db/migration/`
**Depends on**: None (independente dos adaptadores OCI)
**Requirement**: OCI-04

**Done when**:
- [ ] Migrations aplicam sem erro contra Oracle (Testcontainers ou Autonomous DB real).
- [ ] Suíte de testes que valida SQL específico de dialeto roda contra Oracle; o restante pode continuar em H2.

**Tests**: integração (Testcontainers Oracle ou instância real)
**Gate**: Full — mudança de banco é sensível; exige confirmação explícita antes de aplicar em ambiente compartilhado (`AGENTS.md`, seção 15).

---

### T7: Empacotamento para os dois ambientes
**What**: Ajustar `docker-compose.yml`/`Dockerfile` do backend para montar `~/.oci/config` localmente e usar `OCI_AUTH_MODE=instance_principal` na VM, sem publicar porta do backend diretamente (só o nginx).
**Where**: raiz do repositório
**Depends on**: T2, T5
**Requirement**: OCI-03

**Done when**:
- [ ] Mesmo `docker-compose.yml` sobe local e via Ansible na VM, sem edição manual entre os dois.

**Tests**: none (validado manualmente via critério de pronto do spec)
**Gate**: Build

---

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Depois de tarefas só com teste unitário | `./mvnw test` |
| Full | Depois de tarefas com teste de integração | `./mvnw clean test` |
| Build | Depois de fechar a fase de empacotamento | `./mvnw clean test` e validar `docker compose up` local |
