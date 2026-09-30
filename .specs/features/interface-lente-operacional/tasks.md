# Interface Lente Operacional Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/interface-lente-operacional/design.md`
**Status**: Approved

---

## Test Coverage Matrix

| Layer | Required test | Coverage expectation |
| --- | --- | --- |
| Presentational React component | Unit with Testing Library | Accessible name, visible product meaning and variants used by consumers. |
| Stateful React feature | Unit with Testing Library + user-event | Happy path, busy state and documented errors affected by the task. |
| CSS/foundations and assets | Build + visual QA | Compilation, no missing assets, responsive captures and detector. |
| Navigation shell | Unit with mocked Next navigation | Destinations, active state, logout and absence de engenharia no cliente. |
| End-to-end visual journey | Existing integration-oriented component suite + browser inspection | Capture, review and report states remain operable. |
| Documentation | none | Structural validation and read-back. |

## Gate Check Commands

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Component or feature unit change | `npm test -- <test-file>` |
| Full | Cross-screen behavior change | `npm test` |
| Build | Phase close or visual integration | `npm run lint`, then `npm test`, then `npm run build` |

---

## Execution Plan

### Phase 1: Foundation

```text
T1 → T2 → T3
```

### Phase 2: Core Journey

```text
T3 → T4 → T5 → T6 → T7
```

### Phase 3: Integration

```text
T7 → T8
```

---

## Task Breakdown

### T1: Criar fundações e marca

**What**: Criar tokens, folhas de estilo modulares, ativos fotográficos e `BrandMark` reutilizável.
**Where**: `app/frontend/src/components/brand/`
**Depends on**: None
**Reuses**: `next/image`, `next/font` e conceito visual aprovado.
**Requirement**: UI-02, UI-04

**Tools**:

- Plugin: Product Design Image-to-Code
- Skill: `imagegen`, `impeccable`, `ui-ux-pro-max`

**Done when**:

- [ ] A marca possui nome acessível e variantes completa/compacta.
- [ ] Tokens cobrem cor, tipografia, espaço, raio, elevação, foco e movimento reduzido.
- [ ] Imagens de demonstração estão no projeto, identificadas como ilustrativas e com proveniência.
- [ ] Gate quick passa com ao menos 1 novo teste e nenhum teste removido.

**Tests**: unit
**Gate**: quick

**Commit**: `feat(identidade): cria fundações da lente operacional`

### T2: Reconstruir navegação do cliente

**What**: Transformar o shell lateral em cabeçalho operacional no desktop e navegação inferior no celular.
**Where**: `app/frontend/src/components/DashboardShell.tsx`
**Depends on**: T1
**Reuses**: sessão, rotas e logout existentes.
**Requirement**: UI-02, UI-04

**Tools**:

- Skill: `product-design:image-to-code`, `ui-ux-pro-max`

**Done when**:

- [ ] Início, Nova vistoria e Relatórios têm ícone, texto e estado ativo.
- [ ] A marca aprovada substitui o lockup genérico.
- [ ] A navegação do cliente não exibe engenharia e os alvos móveis têm 44 px.
- [ ] Gate quick passa com os testes do shell e nenhum teste removido.

**Tests**: unit
**Gate**: quick

**Commit**: `feat(navegacao): aplica shell operacional ao cliente`

### T3: Redesenhar acesso

**What**: Aplicar a identidade fotográfica e a promessa IA-first às telas de login e cadastro.
**Where**: `app/frontend/src/features/auth/`
**Depends on**: T2
**Reuses**: `AuthForm`, serviços e tratamento de sessão atuais.
**Requirement**: UI-02, UI-04

**Tools**:

- Skill: `product-design:image-to-code`, `ui-ux-pro-max`

**Done when**:

- [ ] Login e cadastro comunicam captura, IA revisável e relatório sem engenharia.
- [ ] Erros, labels, busy state e alternância de senha permanecem acessíveis.
- [ ] Gate quick passa com a suíte de autenticação e nenhum teste removido.

**Tests**: unit
**Gate**: quick

**Commit**: `feat(auth): redesenha acesso com identidade operacional`

### T4: Corrigir e redesenhar nova vistoria

**What**: Implementar a composição fotográfica aprovada e eliminar a colisão do título com o formulário.
**Where**: `app/frontend/src/features/inspections/client/new-inspection-form.tsx`
**Depends on**: T3
**Reuses**: submissão, bloqueio de duplicidade e `ProblemDetail` existentes.
**Requirement**: UI-01, UI-04

**Tools**:

- Skill: `superpowers:test-driven-development`, `product-design:image-to-code`

**Done when**:

- [ ] Fotografia, progresso, título e formulário seguem o comp em 1440 px.
- [ ] Endereço vazio produz erro anunciado e associado ao campo.
- [ ] O texto informa que a prévia é ilustrativa e nada foi enviado à IA.
- [ ] Gate quick passa com ao menos 2 novos cenários e nenhum teste removido.

**Tests**: unit
**Gate**: quick

**Commit**: `fix(vistoria): corrige composição da etapa inicial`

### T5: Redesenhar início e estados compartilhados

**What**: Tornar a próxima ação, relatórios e estados assíncronos coerentes com a Lente Operacional.
**Where**: `app/frontend/src/features/inspections/client/client-dashboard.tsx`
**Depends on**: T4
**Reuses**: ordenação, paginação, `AsyncState` e `StatusBadge` atuais.
**Requirement**: UI-02, UI-03, UI-04

**Tools**:

- Skill: `product-design:image-to-code`, `ui-ux-pro-max`

**Done when**:

- [ ] Próxima ação lidera a tela e estados reais permanecem legíveis sem depender só de cor.
- [ ] Empty, loading e error conservam uma ação inequívoca.
- [ ] Gate quick passa com a suíte do dashboard e nenhum teste removido.

**Tests**: unit
**Gate**: quick

**Commit**: `feat(inicio): prioriza a proxima acao da vistoria`

### T6: Redesenhar captura e processamento

**What**: Aplicar o workspace fotográfico à captura guiada e aos estados de processamento/falha.
**Where**: `app/frontend/src/features/inspections/client/inspection-workflow.tsx`
**Depends on**: T5
**Reuses**: protocolo, upload, retry, polling e máquina de estados atuais.
**Requirement**: UI-03, UI-04

**Tools**:

- Skill: `superpowers:test-driven-development`, `product-design:image-to-code`

**Done when**:

- [ ] Roteiro, evidência ativa, progresso e ações de captura formam um único workspace responsivo.
- [ ] Estados ocupado, erro, retry e processamento preservam a semântica existente.
- [ ] Gate full passa com toda a suíte de frontend e nenhum teste removido.

**Tests**: unit
**Gate**: full

**Commit**: `feat(captura): aplica workspace fotografico ao roteiro`

### T7: Redesenhar revisão e relatório

**What**: Unificar a relação evidência-IA-contexto na revisão, resultado e documento final.
**Where**: `app/frontend/src/features/inspections/client/inspection-review.tsx`
**Depends on**: T6
**Reuses**: decisões, reconciliação, impressão e compartilhamento atuais.
**Requirement**: UI-03, UI-04

**Tools**:

- Skill: `superpowers:test-driven-development`, `product-design:image-to-code`

**Done when**:

- [ ] Foto, observação da IA e decisão humana mantêm rastreabilidade visível.
- [ ] Relatório permanece claro, imprimível e sem linguagem de laudo técnico.
- [ ] Gate full passa com toda a suíte de frontend e nenhum teste removido.

**Tests**: unit
**Gate**: full

**Commit**: `feat(relatorio): unifica evidencia e contexto revisado`

### T8: Validar responsividade e acabamento

**What**: Executar lint, testes, build, detector, capturas, design QA e documentação final.
**Where**: `app/design-qa.md`
**Depends on**: T7
**Reuses**: scripts e suíte existentes.
**Requirement**: UI-01, UI-02, UI-03, UI-04

**Tools**:

- Skill: `impeccable`, `hm-designer`, `hm-ux-flow`, `superpowers:verification-before-completion`

**Done when**:

- [ ] Lint, testes e build passam com contagem conferida.
- [ ] Capturas válidas de 390, 768 e 1440 px não têm overflow ou colisão.
- [ ] `app/design-qa.md` contém `final result: passed`.
- [ ] Detector e revisores independentes não deixam P0, P1 ou P2 aberto.
- [ ] Gate build passa sem arquivo temporário ou credencial no diff.

**Tests**: none
**Gate**: build

**Commit**: `docs(design): registra validacao da lente operacional`

---

## Phase Execution Map

```text
Phase 1 → Phase 2 → Phase 3

Phase 1: T1 → T2 → T3
Phase 2: T3 → T4 → T5 → T6 → T7
Phase 3: T7 → T8
```

Execution is strictly sequential - there is no intra-phase parallelism.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1 | Fundações e um componente de marca | Granular |
| T2 | Um shell de navegação | Granular |
| T3 | Uma feature de autenticação | Granular |
| T4 | Uma tela/formulário | Granular |
| T5 | Uma tela de início | Granular |
| T6 | Um workspace de captura | Granular |
| T7 | Um fluxo documental coeso | Granular |
| T8 | Um gate de integração | Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | None | Match |
| T2 | T1 | T1 → T2 | Match |
| T3 | T2 | T2 → T3 | Match |
| T4 | T3 | T3 → T4 | Match |
| T5 | T4 | T4 → T5 | Match |
| T6 | T5 | T5 → T6 | Match |
| T7 | T6 | T6 → T7 | Match |
| T8 | T7 | T7 → T8 | Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Presentational component + assets | unit + visual | unit | OK |
| T2 | Navigation shell | unit | unit | OK |
| T3 | Stateful auth feature | unit | unit | OK |
| T4 | Stateful form | unit | unit | OK |
| T5 | Stateful dashboard | unit | unit | OK |
| T6 | Stateful workflow | unit + journey | unit | OK |
| T7 | Stateful review/report | unit + journey | unit | OK |
| T8 | Documentation and integration gate | none + build | none | OK |
