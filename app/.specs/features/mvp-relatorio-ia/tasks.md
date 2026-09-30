# MVP Relatório de vistoria por IA — Plano de implementação

> **Execução obrigatória:** usar `tlc-spec-driven` e TDD em cada comportamento. Cada tarefa termina em um commit lógico e só recebe `Complete` após seus gates passarem.

**Goal:** Entregar a jornada cliente → evidências → análise assíncrona → revisão → Relatório de vistoria por IA, sem engenheiro no fluxo principal.

**Architecture:** O Spring Boot permanece autoridade de estado, ownership e validação do JSON não confiável da IA. O Next.js consome somente DTOs tipados e implementa o sistema visual `Clareza Técnica` do protótipo aprovado.

**Tech Stack:** Java 21, Spring Boot 3.2.3, JPA/Flyway, PostgreSQL, Next.js 16.3.5, React 19, TypeScript, Tailwind 4, Vitest e Testing Library.

**Spec:** `.specs/features/mvp-relatorio-ia/spec.md`
**Design:** `.specs/features/mvp-relatorio-ia/design.md`
**Status:** Approved

## Baseline atual

- Backend em Java 21: 113 testes executados, 112 verdes e uma falha preexistente em `VistoriaControllerTest.shouldUploadImagem`, que ainda espera o campo interno `storagePath` já ausente da resposta.
- Frontend: 7 arquivos de teste, 52 testes verdes.
- O gate backend sempre deve fixar `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`; Java 25 não é suportado pelo Byte Buddy usado nesta versão.

## Test Coverage Matrix

| Camada | Tipo | Cobertura exigida | Comando |
| --- | --- | --- | --- |
| Domínio/application | JUnit 5 + Mockito | transições, idempotência, ownership, contrato inválido, falha externa e revisão completa/incompleta | `.\mvnw.cmd "-Dtest=VistoriaServiceTest,*Analysis*Test,*Review*Test" test` |
| Web/Security | MockMvc | sucesso, validação, 401/403/404/409/422, 202 e ProblemDetail | `.\mvnw.cmd "-Dtest=VistoriaControllerTest" test` |
| Persistência/migrations | JPA + Testcontainers PostgreSQL | V7, JSON de revisão, optimistic locking e schema completo | `.\mvnw.cmd "-Dtest=PostgreSqlMigrationIntegrationTest,VistoriaRepositoryTest" test` |
| Helpers TypeScript | Vitest | protocolo, estado, ordenação, relatório e compartilhamento | `npm test -- --run src/features src/lib` |
| Componentes React | RTL/user-event | loading, empty, error, busy, teclado e cada decisão da revisão | `npm test -- --run` |
| Build | compilação/lint | tipos, rotas, metadata, CSS e produção | `npm run lint` e `npm run build` |
| UAT visual | navegador | 390, 768 e 1440 px; captura, revisão, relatório, foco e overflow | registrar em `validation.md` |

## Gate Check Commands

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
cd frontend
npm test -- --run
npm run lint
npm run build
```

## Execution Plan

```text
T1 -> T2 -> T3 -> T4 -> T5 -> T6 -> T7 -> T8
```

## Task Breakdown

### T1: Consolidar o contrato seguro de análise

**Status:** Pending  
**Requirement:** AIR-02, AIR-05  
**Where:** `vistoria/application/analysis`, DTOs/mapper web, protocolo e testes correspondentes.

- [ ] Escrever RED para os doze códigos de protocolo e para parser válido, malformado, sem versão, imagem desconhecida e mais de 100 achados.
- [ ] Implementar `PreLaudoParser` e DTOs tipados ligados por `imagemId`, sem `storagePath` ou JSON bruto no contrato público.
- [ ] Corrigir o teste legado que ainda espera `storagePath` e provar ausência explícita do campo.
- [ ] Executar testes focados e backend completo em Java 21.

**Done when:** os doze códigos são aceitos, a análise inválida é recusada e a API expõe apenas o contrato tipado.  
**Tests:** unit + MockMvc  
**Gate:** backend focused + backend full  
**Commit:** `feat(vistoria): tipa contrato seguro da análise por IA`

### T2: Tornar a submissão realmente assíncrona

**Status:** Pending  
**Depends on:** T1  
**Requirement:** AIR-02, AIR-05  
**Where:** estado de `Vistoria`, evento/listener/processador e configuração assíncrona.

- [ ] Escrever RED para resposta imediata em `AGUARDANDO_IA`, evento pós-commit, chamada única, sucesso, falha e repetição concorrente.
- [ ] Criar métodos de intenção no domínio e estados `REVISAO_PENDENTE` e `RELATORIO_DISPONIVEL`.
- [ ] Publicar evento na submissão e processar IA fora da requisição/transação em listener `@Async` pós-commit.
- [ ] Persistir `FALHA_IA` para erro, timeout ou contrato inválido, sem fabricar análise.
- [ ] Executar gates backend focado e completo.

**Done when:** `POST /submeter` retorna 202 antes da IA e uma repetição em processamento não agenda trabalho duplicado.  
**Tests:** domain + application + MockMvc  
**Gate:** backend focused + backend full  
**Commit:** `feat(vistoria): processa análise por IA de forma assíncrona`

### T3: Persistir revisão e disponibilizar o relatório

**Status:** Pending  
**Depends on:** T2  
**Requirement:** AIR-03, AIR-04, AIR-05  
**Where:** migration V7, domínio/application, endpoints e ProblemDetail.

- [ ] Escrever RED para V7, upsert por `imagemId + indiceAchado`, limites, ownership, estado inválido e índice inexistente.
- [ ] Adicionar `revisao_usuario` por migration aditiva e store JSON versionado com no máximo 100 revisões.
- [ ] Implementar `PUT /api/vistorias/{id}/revisao` para confirmar, corrigir e rejeitar.
- [ ] Implementar `POST /api/vistorias/{id}/relatorio`, bloqueando cobertura incompleta e mantendo conclusão idempotente.
- [ ] Mapear conflitos e optimistic locking para RFC 9457; validar PostgreSQL e suíte completa.

**Done when:** toda conclusão é rastreável, análise original não muda e somente revisão completa produz `RELATORIO_DISPONIVEL`.  
**Tests:** unit + MockMvc + PostgreSQL integration  
**Gate:** backend focused + PostgreSQL + backend full  
**Commit:** `feat(vistoria): persiste revisão e conclui relatório por IA`

### T4: Aplicar identidade IA-first e simplificar o acesso

**Status:** Pending  
**Depends on:** T3  
**Requirement:** AIR-01, AIR-06  
**Where:** metadata, autenticação pública, `DashboardShell`, tokens globais e componentes UI.

- [ ] Escrever RED para cadastro fixo de cliente, ausência de engenharia, shell e estados acessíveis.
- [ ] Aplicar tokens, Instrument Sans, IBM Plex Mono para metadados, foco visível, movimento reduzido e superfícies do protótipo.
- [ ] Remover seletor de papel/CREA/convite e atalhos de engenharia da jornada pública sem apagar o legado backend.
- [ ] Implementar navegação lateral desktop e inferior móvel; executar testes, lint e build.

**Done when:** a proposta `Relatório de vistoria por IA` é inequívoca e não há engenharia na entrada nem navegação principal.  
**Tests:** unit + React component  
**Gate:** frontend full  
**Commit:** `feat(frontend): aplica identidade IA-first ao acesso`

### T5: Reconstruir início e captura guiada

**Status:** Pending  
**Depends on:** T4  
**Requirement:** AIR-01, AIR-02, AIR-06  
**Where:** dashboard, nova vistoria, workflow, protocolo e upload.

- [ ] Escrever RED para vazio/erro/retomada, doze itens agrupados, progresso derivado e mutação única.
- [ ] Implementar início orientado à próxima ação e listagem secundária de relatórios.
- [ ] Implementar captura mobile-first com câmera/galeria, instrução contextual, miniatura, erro local e próximo item.
- [ ] Implementar estado de análise sem percentual inventado, polling cancelável e tentativa novamente após falha.
- [ ] Executar suíte frontend, lint e build.

**Done when:** atualizar a página reconstrói o progresso pela API e a submissão conduz ao estado assíncrono real.  
**Tests:** unit + React component  
**Gate:** frontend full  
**Commit:** `feat(frontend): reconstrói captura guiada de evidências`

### T6: Implementar revisão rastreável

**Status:** Pending  
**Depends on:** T5  
**Requirement:** AIR-03, AIR-06  
**Where:** `InspectionReview`, serviços/tipos e integração no workflow.

- [ ] Escrever RED para foto dominante, uma decisão por vez, validações, upsert, falhas e conclusão bloqueada.
- [ ] Implementar confirmar/corrigir/rejeitar com contexto obrigatório e tipo obrigatório na correção.
- [ ] Persistir antes de avançar, reconciliar 409/401 sem repetir mutação e anunciar mudanças em região viva.
- [ ] Executar suíte frontend, lint e build.

**Done when:** cada achado pode ser retomado após refresh e permanece ligado à evidência correta.  
**Tests:** unit + React component  
**Gate:** frontend full  
**Commit:** `feat(frontend): implementa revisão rastreável dos achados`

### T7: Entregar o Relatório de vistoria por IA

**Status:** Pending  
**Depends on:** T6  
**Requirement:** AIR-04, AIR-06  
**Where:** `InspectionReport`, CSS de impressão, share/clipboard e documentação de entrada.

- [ ] Escrever RED para inclusão de confirmados/corrigidos, exclusão de rejeitados, evidências preservadas, impressão e fallbacks.
- [ ] Implementar documento responsivo com vínculo explícito entre foto, achado, contexto e ID.
- [ ] Adicionar aviso de limite, `window.print`, Web Share e Clipboard sem sucesso falso.
- [ ] Atualizar README/arquitetura que ainda trate engenheiro como destino do MVP.
- [ ] Executar frontend full e backend full.

**Done when:** o relatório é útil, imprimível e compartilhável, sem se apresentar como laudo técnico.  
**Tests:** unit + React component + print/share  
**Gate:** frontend full + backend full  
**Commit:** `feat(relatorio): entrega documento rastreável gerado por IA`

### T8: Validar o MVP integrado e preparar publicação

**Status:** Pending  
**Depends on:** T7  
**Requirement:** AIR-01, AIR-02, AIR-03, AIR-04, AIR-05, AIR-06  
**Where:** `.specs/features/mvp-relatorio-ia/validation.md` e correções estritamente necessárias.

- [ ] Executar backend Java 21, PostgreSQL, frontend tests/lint/build em estado fresco e registrar contagens.
- [ ] Executar UAT real do caminho cadastro → captura → análise → revisão → relatório, incluindo falha e retomada.
- [ ] Auditar 390, 768 e 1440 px, teclado, foco, toque, movimento reduzido e overflow.
- [ ] Obter verificação independente exigida pelo TLC e corrigir achados confirmados.
- [ ] Produzir `validation.md` com evidência por AC, atualizar estados, validar TLC e revisar diff/commits antes do push.

**Done when:** todos os critérios têm evidência atual, a verificação independente retorna PASS e a árvore está limpa.  
**Tests:** full integration + UAT + independent verification  
**Gate:** all gates + TLC completion  
**Commit:** `test(mvp): valida jornada completa do relatório por IA`

## Requirement Traceability

| Requirement | Tasks |
| --- | --- |
| AIR-01 | T4, T5, T8 |
| AIR-02 | T1, T2, T5, T8 |
| AIR-03 | T3, T6, T8 |
| AIR-04 | T3, T7, T8 |
| AIR-05 | T1, T2, T3, T8 |
| AIR-06 | T4, T5, T6, T7, T8 |

## Granularity and co-location check

| Task | Entrega revisável | Testes co-localizados |
| --- | --- | --- |
| T1 | contrato tipado seguro | parser + service + MockMvc |
| T2 | processamento assíncrono | domain + application + web |
| T3 | revisão/conclusão persistida | domain + service + web + PostgreSQL |
| T4 | fundação visual e acesso | unit + component + build |
| T5 | dashboard/captura | unit + component |
| T6 | revisão | unit + component |
| T7 | relatório | unit + component + print |
| T8 | aceite integrado | suítes + UAT + verifier |
