# Interface e roteiro adaptativo Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow.

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/interface-lente-operacional/design.md`
**Status**: Approved

---

## Test Coverage Matrix

> Generated from `AGENTS.md`, existing JUnit/MockMvc/Testcontainers tests, Vitest/Testing Library suites, `app/pom.xml` and `app/frontend/package.json`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Domain/service | unit | Todas as regras e ramos dos ACs; todos os limites do roteiro | `app/src/test/java/**/*Test.java` | `.\mvnw.cmd test` |
| Controller/contract | MockMvc | Rotas alteradas: happy path, validação, ownership e conflito | `app/src/test/java/**/*ControllerTest.java` | `.\mvnw.cmd test` |
| Migration/persistence | integration PostgreSQL | Schema, FK, ordem e leitura legada | `app/src/test/java/**/*IntegrationTest.java` | `.\mvnw.cmd test` |
| React stateful | unit + Testing Library | Happy path, busy, validação e erro documentado | `app/frontend/src/**/*.test.tsx` | `npm test` |
| CSS/assets | build + visual QA | Sem ativo ausente, overflow, colisão ou contraste material | `app/design-qa.md` | `npm run lint && npm test && npm run build` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick backend | Domínio ou caso de uso focado | `.\mvnw.cmd "-Dtest=<Teste>" test` em `app` |
| Quick frontend | Componente ou feature focada | `npm test -- <teste>` em `app/frontend` |
| Full | Contrato ou jornada transversal | `.\mvnw.cmd test` em `app`, depois `npm test` em `app/frontend` |
| Build | Fechamento de fase | `.\mvnw.cmd test`; `npm run lint`; `npm test`; `npm run build` |

---

## Execution Plan

### Phase 1: Structured route

```text
T1 → T2 → T3
```

### Phase 2: Product interface

```text
T3 → T4 → T5 → T6 → T7
```

### Phase 3: Integration

```text
T7 → T8
```

---

## Task Breakdown

### T1: Modelar roteiro persistido por ambiente

**What**: Criar enums, agregado de ambiente, relacionamento de evidência e migration V8 compatível com dados legados.
**Where**: `app/src/main/`
**Depends on**: None
**Reuses**: `Vistoria`, `ImagemVistoria`, Flyway e Testcontainers existentes.
**Requirement**: ROTEIRO-01, CAPTURA-01
**Status**: Complete

**Tools**:

- Skill: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] O agregado aceita de 1 a 30 ambientes ordenados e rejeita nomes duplicados.
- [x] Imagem nova referencia ambiente e categoria; leitura legada continua possível.
- [x] Migration executa no PostgreSQL de integração sem editar migrations antigas.
- [x] Gate full passa com todos os testes anteriores e ao menos 6 novos cenários.

**Tests**: unit + integration
**Gate**: full

**Commit**: `feat(dominio): modela roteiro adaptativo da vistoria`

### T2: Expor criação e edição do roteiro

**What**: Alterar criação e adicionar atualização de roteiro com validação, ownership e conflito seguro.
**Where**: `app/src/main/java/br/com/vistoriapredial/vistoria/`
**Depends on**: T1
**Reuses**: `VistoriaService`, `VistoriaController`, `ProblemDetail` e `@Version`.
**Requirement**: ROTEIRO-01, ROTEIRO-02
**Status**: Complete

**Tools**:

- Skill: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] `POST /api/vistorias` persiste tipo e ambientes atomicamente.
- [x] `PUT /api/vistorias/{id}/roteiro` adiciona, renomeia, reordena e remove somente ambientes sem evidência.
- [x] Validação retorna `422`; estado ou remoção insegura retorna `409`.
- [x] Gate full passa com todos os testes anteriores e ao menos 8 novos cenários.

**Tests**: unit + MockMvc
**Gate**: full

**Commit**: `feat(api): permite configurar o roteiro da vistoria`

### T3: Estruturar upload e regra de completude

**What**: Associar upload a ambiente/categoria e impedir envio enquanto faltar visão geral.
**Where**: `app/src/main/java/br/com/vistoriapredial/vistoria/`
**Depends on**: T2
**Reuses**: validação de arquivo, compensação de storage e máquina de estados.
**Requirement**: CAPTURA-01, ENVIO-01

**Tools**:

- Skill: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] Upload inválido falha antes de gravar arquivo.
- [x] Upload válido persiste ambiente e `VISAO_GERAL` ou `DETALHE`.
- [x] Envio incompleto retorna `422` com nomes ausentes; completo inicia a IA.
- [x] Gate full passa com todos os testes anteriores e ao menos 8 novos cenários.

**Tests**: unit + MockMvc
**Gate**: full

**Commit**: `feat(captura): estrutura evidencias por ambiente`

### T4: Criar fundações visuais e contratos do frontend

**What**: Aplicar paleta revisada, marca sem moldura e tipos/API do roteiro adaptativo.
**Where**: `app/frontend/src/`
**Depends on**: T3
**Reuses**: `next/image`, `next/font`, Lucide e cliente HTTP existente.
**Requirement**: UI-01, ROTEIRO-01

**Tools**:

- Skill: `impeccable`, `product-design:image-to-code`, `ui-ux-pro-max`

**Done when**:

- [x] `BrandMark` possui nome acessível e não contém moldura externa.
- [x] Tokens usam somente grafite-petróleo, mineral, verdigris, âmbar e cores semânticas.
- [x] Tipos e funções da API representam ambientes e categorias estruturados.
- [x] Gate quick passa com todos os testes anteriores e ao menos 4 novos cenários.

**Tests**: unit
**Gate**: quick frontend

**Commit**: `feat(identidade): aplica a lente operacional revisada`

### T5: Redesenhar shell, acesso e início

**What**: Aplicar a nova identidade à navegação, autenticação e dashboard IA-first.
**Where**: `app/frontend/src/components/`
**Depends on**: T4
**Reuses**: sessão, rotas, logout e listagem existentes.
**Requirement**: UI-01

**Tools**:

- Skill: `product-design:image-to-code`, `ui-ux-pro-max`

**Done when**:

- [x] Desktop usa cabeçalho e celular usa navegação inferior com alvos de 44 px.
- [x] Acesso explica captura, IA revisável e relatório sem engenharia.
- [x] Início prioriza próxima ação e estados reais.
- [x] Gate full passa com todos os testes anteriores e ao menos 5 novos cenários.

**Tests**: unit
**Gate**: full

**Commit**: `feat(frontend): redesenha acesso e navegacao do cliente`

### T6: Implementar configuração e captura flexível

**What**: Criar o construtor de roteiro e substituir o protocolo fixo pelo workspace adaptativo.
**Where**: `app/frontend/src/features/inspections/client/`
**Depends on**: T5
**Reuses**: create, upload, polling, retry e `EvidenceImage` existentes.
**Requirement**: ROTEIRO-01, ROTEIRO-02, CAPTURA-01, ENVIO-01

**Tools**:

- Skill: `superpowers:test-driven-development`, `product-design:image-to-code`

**Done when**:

- [x] Tipo sugere ambientes sem impô-los; nome personalizado e validação são acessíveis.
- [x] A criação envia endereço e roteiro em uma operação.
- [x] Captura usa `x de y ambientes`, editar roteiro, visão geral, detalhe e pular por agora.
- [x] Envio incompleto leva aos ambientes ausentes.
- [x] Gate full passa com todos os testes anteriores e ao menos 10 novos cenários.

**Tests**: unit
**Gate**: full

**Commit**: `feat(vistoria): cria roteiro flexivel de ambientes`

### T7: Contextualizar revisão e relatório

**What**: Exibir ambiente e categoria na revisão, resultado e relatório imprimível.
**Where**: `app/frontend/src/features/inspections/client/`
**Depends on**: T6
**Reuses**: decisões, reconciliação, compartilhamento e impressão existentes.
**Requirement**: RELATORIO-01, UI-01

**Tools**:

- Skill: `superpowers:test-driven-development`, `product-design:image-to-code`

**Done when**:

- [x] Foto, ambiente, IA e decisão humana permanecem juntos.
- [x] Relatório agrupa evidências por ambiente e mantém ressalva de escopo.
- [x] Evidência legada usa fallback legível.
- [x] Gate full passa com todos os testes anteriores e ao menos 6 novos cenários.

**Tests**: unit
**Gate**: full

**Commit**: `feat(relatorio): preserva contexto dos ambientes`

### T8: Validar integração, responsividade e acabamento

**What**: Executar gates, detector, capturas, QA de fluxo e documentação final.
**Where**: `app/design-qa.md`
**Depends on**: T7
**Reuses**: suítes existentes e conceitos aprovados.
**Requirement**: ROTEIRO-01, ROTEIRO-02, CAPTURA-01, ENVIO-01, RELATORIO-01, UI-01
**Status**: Complete

**Tools**:

- Skill: `impeccable`, `hm-designer`, `hm-ux-flow`, `superpowers:verification-before-completion`

**Done when**:

- [x] Backend, lint, frontend tests e build passam com contagem registrada.
- [x] Casa, apartamento e comercial completam fluxos com roteiros diferentes.
- [x] Capturas em 390, 768 e 1440 px não têm overflow ou colisão.
- [x] `app/design-qa.md` contém `final result: passed`.
- [x] Verificador independente não deixa P0, P1 ou P2 aberto.

**Tests**: build + visual QA
**Gate**: build

**Commit**: `docs(design): registra validacao do roteiro adaptativo`

---

## Phase Execution Map

```text
Phase 1 → Phase 2 → Phase 3

Phase 1: T1 → T2 → T3
Phase 2: T4 → T5 → T6 → T7
Phase 3: T8
```

Execution is strictly sequential - there is no intra-phase parallelism.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1 | Um modelo de domínio coeso | Granular |
| T2 | Um contrato de roteiro vertical | Granular |
| T3 | Um fluxo de upload/submissão | Granular |
| T4 | Uma fundação compartilhada | Granular |
| T5 | Um shell de entrada | Granular |
| T6 | Uma jornada de configuração/captura | Granular |
| T7 | Uma saída documental | Granular |
| T8 | Um gate de integração | Granular |

## Diagram-Definition Cross-Check

| Task | Depends On | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | None | Match |
| T2 | T1 | T1 → T2 | Match |
| T3 | T2 | T2 → T3 | Match |
| T4 | T3 | T3 → T4 | Match |
| T5 | T4 | T4 → T5 | Match |
| T6 | T5 | T5 → T6 | Match |
| T7 | T6 | T6 → T7 | Match |
| T8 | T7 | T7 → T8 | Match |

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Domínio + schema | unit + integration | unit + integration | OK |
| T2 | Serviço + controller | unit + MockMvc | unit + MockMvc | OK |
| T3 | Serviço + controller | unit + MockMvc | unit + MockMvc | OK |
| T4 | Contratos + componente | unit | unit | OK |
| T5 | React stateful | unit | unit | OK |
| T6 | React stateful | unit | unit | OK |
| T7 | React stateful | unit | unit | OK |
| T8 | Integração visual | build + visual QA | build + visual QA | OK |
