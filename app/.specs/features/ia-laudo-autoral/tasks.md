# IA autoral e relatório verificável — Tarefas

## Execution Protocol (MANDATORY -- do not skip)

Implementar estas tarefas com a skill `tlc-spec-driven`, seguindo seu fluxo
Execute e as Critical Rules. Usar `superpowers:test-driven-development` em todo
comportamento novo, `superpowers:systematic-debugging` quando um gate falhar e
`superpowers:verification-before-completion` antes de declarar conclusão.

As tarefas de interface usam `product-design:audit` e
`browser:control-in-app-browser` para a inspeção final. A verificação da feature
é feita por um Verifier independente após a última tarefa.

**Design**: `.specs/features/ia-laudo-autoral/design.md`  
**Status**: In Progress — approved by user on 30 September 2026

## Baseline verificado

- Backend: 168 testes passando com JDK 21.0.12.1 e `mvnw.cmd test`.
- Frontend: 110 testes passando em 14 arquivos com `npm test`.
- O JDK 25 instalado como default não é aceito pelo Byte Buddy atual; todos os
  gates backend desta feature fixam `JAVA_HOME` no JDK 21 já instalado.
- Nenhum teste automatizado chama OCI ou envia imagem a serviço externo.

## Test Coverage Matrix

> Gerada a partir de `AGENTS.md`, `pom.xml`, `frontend/package.json`,
> `frontend-redesign/tasks.md`, 8 testes backend/frontend amostrados e da spec.
> `AGENTS.md` e a spec definem o alvo de cobertura; os testes existentes definem
> estilo e localização.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Contratos e cálculo da análise | unit | Todos os ramos, quatro resultados, precedência, evidência insuficiente e edge cases IAR-02/IAR-03 | `src/test/java/**/analysis/*Test.java` | `mvnw.cmd -Dtest=PreLaudoParserTest,ResultadoAnaliseCalculatorTest test` |
| Integração OCI | unit com fake | Mensagem multimodal, contexto, schema, limite/tipo de imagem, resposta e 401/403/404/429/5xx sem rede real | `src/test/java/**/integration/oci/**/*Test.java` | `mvnw.cmd -Dtest=OciGenAi*Test test` |
| Orquestração e domínio | unit com Mockito ou puro | Estado, idempotência, contexto por ambiente, ausência de fallback e transação curta | `src/test/java/**/vistoria/application/**/*Test.java`, `src/test/java/**/vistoria/domain/*Test.java` | `mvnw.cmd -Dtest=VistoriaAnalysisProcessorTest,VistoriaServiceTest,VistoriaRoteiroTest test` |
| API e segurança | integração MockMvc | Rotas alteradas: sucesso, ownership, 403, 409, 422 e ProblemDetail | `src/test/java/**/web/*Test.java` | `mvnw.cmd -Dtest=VistoriaControllerTest test` |
| Upload e configuração | unit + contexto | Limite de 7 MB, tipos aceitos, provider permitido, mock proibido fora de test/demo e contexto Spring | `src/test/java/**/*Evidence*Test.java`, `src/test/java/**/*ApplicationTests.java` | `mvnw.cmd -Dtest=EvidenceFileValidatorTest,VistoriaPredialApplicationTests test` |
| Tipos e helpers frontend | unit | Contrato v2, taxonomia em português, agrupamento por ambiente e compatibilidade v1 | `frontend/src/**/*.test.ts` | `npm run test -- src/features/inspections` |
| Componentes React | component | Conteúdo, navegação entre ambientes, feedback opcional, falhas, teclado e todos os estados IAR-04/IAR-05 | `frontend/src/**/*.test.tsx` | `npm run test -- src/features/inspections` |
| CSS e configuração frontend | build/lint | TypeScript, ESLint, build e ausência de overflow em 375/768/1440 px | `frontend/src/app/`, `frontend/src/features/` | `npm run lint` e `npm run build` |
| Integração real opt-in | smoke manual controlado | Navegador → Spring → OCI → relatório; nunca na suíte padrão; PASS ou bloqueio externo comprovado | `.specs/features/ia-laudo-autoral/validation.md` | Script opt-in somente com autorização, credencial e cota |

## Gate Check Commands

> Comandos extraídos do repositório. Backend deve usar o JDK 21 instalado. Os
> comandos listados na mesma célula são executados separadamente, nesta ordem.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Backend quick | Depois de tarefa backend unitária | definir `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`; `mvnw.cmd -Dtest=<testes focados> test` |
| Backend full | Depois de integração, web ou final de fase backend | definir JDK 21; `mvnw.cmd test` — baseline mínimo 168 |
| Frontend focused | Durante RED/GREEN de helper ou componente | `npm run test -- src/features/inspections` |
| Frontend full | Depois de tarefa frontend e final de fase | `npm test`; `npm run lint`; `npm run build` — baseline mínimo 110 |
| Build | Configuração/dependência ou final de fase | backend full; frontend full quando afetado |
| Integrated | Depois de T19 | backend full; frontend full; smoke local sem OCI pago; UAT 375/768/1440 px |
| TLC completion | Depois do relatório PASS | `python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\validate_state.py ia-laudo-autoral --root app` |

## Requirement Traceability

| Requirement | Tasks |
| --- | --- |
| IAR-01 — OCI real sem sucesso simulado | T1, T7, T8, T9, T10, T14, T19 |
| IAR-02 — contrato por ambiente | T1, T2, T4, T5, T9, T10, T15 |
| IAR-03 — resultado automatizado | T2, T3, T4, T6, T10, T12, T15, T17 |
| IAR-04 — manifestação separada | T11, T12, T15, T16, T17 |
| IAR-05 — relatório completo | T5, T6, T12, T15, T17, T18 |
| IAR-06 — validação e observabilidade | T8, T9, T10, T14, T19 |

## Execution Plan

As fases e suas tarefas executam sequencialmente.

### Phase 1: Contrato e regra de análise

```text
T1 -> T2 -> T3 -> T4 -> T5 -> T6
```

### Phase 2: Adaptador OCI e orquestração

```text
T7 -> T8 -> T9 -> T10
```

### Phase 3: Autoridade e contrato HTTP

```text
T11 -> T12 -> T13 -> T14
```

### Phase 4: Experiência do resultado

```text
T15 -> T16 -> T17 -> T18
```

### Phase 5: Integração e documentação operacional

```text
T19
```

Dependências entre fases:

```text
T6 -> T7
T10 -> T11
T14 -> T15
T18 -> T19
```

## Task Breakdown

### T1: Introduzir a solicitação de análise contextual

**Status**: Complete  
**What**: Criar o contrato imutável que transporta vistoria, imagem, ambiente,
categoria, caminho e tipo de conteúdo até a porta de IA.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/ia/`  
**Depends on**: None  
**Reuses**: `ImagemVistoria`, `AmbienteVistoria`, `CategoriaEvidencia`.  
**Requirement**: IAR-01, IAR-02

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [ ] A solicitação rejeita lista nula/vazia e evidência sem identidade ou contexto obrigatório.
- [ ] O contrato preserva IDs e nome do ambiente fornecidos pelo domínio.
- [ ] Pelo menos 4 testes unitários novos passam e os 168 testes backend não diminuem.

**Tests**: unit  
**Gate**: Backend quick  
**Commit**: `feat(ia): adiciona contexto das evidências à análise`

### T2: Modelar o documento canônico v2

**Status**: Complete  
**What**: Evoluir `AnaliseVistoria` para representar execução, imagem, ambiente,
qualidade, achado descritivo e resultados v2 sem quebrar a leitura v1.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/analysis/AnaliseVistoria.java`  
**Depends on**: T1  
**Reuses**: record atual e enums de domínio.  
**Requirement**: IAR-02, IAR-03

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [ ] Os quatro resultados e enums de qualidade, gravidade e confiança são fechados.
- [ ] O achado contém critério, descrição, evidência, impacto e recomendação.
- [ ] Contratos v1 ainda podem ser projetados sem inventar metadados v2.
- [ ] Pelo menos 4 testes unitários de contrato passam.

**Tests**: unit  
**Gate**: Backend quick  
**Commit**: `feat(ia): modela análise visual canônica v2`

### T3: Calcular resultados de forma determinística

**Status**: Complete  
**What**: Implementar `ResultadoAnaliseCalculator` com resultado por ambiente e
geral conforme qualidade e maior gravidade.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/analysis/ResultadoAnaliseCalculator.java`  
**Depends on**: T2  
**Reuses**: enums do documento canônico v2.  
**Requirement**: IAR-03

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [ ] Evidência insuficiente sem alternativa válida resulta em `INCONCLUSIVO`.
- [ ] Gravidade alta/crítica resulta em `NAO_APROVADO`.
- [ ] Gravidade baixa/média resulta em `APROVADO_COM_RESSALVAS`.
- [ ] Evidência suficiente sem achado resulta em `APROVADO`.
- [ ] Agregação geral usa a precedência definida e ao menos 10 testes cobrem todos os ramos.

**Tests**: unit  
**Gate**: Backend quick  
**Commit**: `feat(ia): calcula resultado visual por ambiente`

### T4: Validar e projetar o JSON v2

**Status**: Complete  
**What**: Evoluir `PreLaudoParser` para validar integralmente o JSON v2,
associar IDs conhecidos e manter compatibilidade de leitura v1.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/analysis/PreLaudoParser.java`  
**Depends on**: T3  
**Reuses**: `PreLaudoParserTest`, limites atuais e `ResultadoAnaliseCalculator`.  
**Requirement**: IAR-02, IAR-03

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [ ] Documento v2 válido expõe execução, imagem, ambiente, achados e resultados.
- [ ] Referência desconhecida, enum inválido, campo obrigatório vazio, texto excedente e 101º achado rejeitam o documento integral.
- [ ] Documento v1 válido continua legível.
- [ ] Pelo menos 12 cenários novos/atualizados passam.

**Tests**: unit  
**Gate**: Backend full  
**Commit**: `feat(ia): valida contrato canônico da análise v2`

### T5: Serializar a resposta canônica auditável

**Status**: Complete  
**What**: Criar o montador que combina observações válidas, identidades locais,
metadados da execução e resultados calculados em JSON v2.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/analysis/AnaliseVistoriaDocumentFactory.java`  
**Depends on**: T4  
**Reuses**: `ObjectMapper`, solicitação T1, modelo T2 e calculador T3.  
**Requirement**: IAR-02, IAR-03, IAR-05

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [ ] Cada resposta conserva imagem, ambiente e categoria originais.
- [ ] Metadados registram provider, modelo, prompt, analysisId e instante.
- [ ] Resultados são derivados pelo calculador, não aceitos do texto livre.
- [ ] Pelo menos 6 testes de payload verificam valores de todos os campos nomeados.

**Tests**: unit  
**Gate**: Backend quick  
**Commit**: `feat(ia): monta documento auditável da análise`

### T6: Disponibilizar relatório após análise válida

**Status**: Complete  
**What**: Alterar a máquina de estados para que uma análise válida conclua em
`RELATORIO_DISPONIVEL` com data de conclusão, sem revisão obrigatória.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/domain/Vistoria.java`  
**Depends on**: T5  
**Reuses**: controle otimista e estados existentes.  
**Requirement**: IAR-03, IAR-05

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [ ] `AGUARDANDO_IA` só vira `RELATORIO_DISPONIVEL` com documento válido.
- [ ] `dataConclusao` usa instante recebido pelo caso de uso.
- [ ] Falha preserva `FALHA_IA` e não cria relatório.
- [ ] Pelo menos 5 testes de domínio cobrem sucesso, falha e estados inválidos.

**Tests**: unit  
**Gate**: Backend full  
**Commit**: `feat(vistoria): disponibiliza relatório após análise válida`

### T7: Adicionar o OCI Java SDK

**Status**: Complete  
**What**: Adicionar BOM, módulo Generative AI Inference e cliente Jersey 3 do
OCI SDK em versões compatíveis, sem alterar a stack Spring.  
**Where**: `pom.xml`  
**Depends on**: T6  
**Reuses**: Maven Wrapper e Java 21.  
**Requirement**: IAR-01

**Tools**:

- MCP: terminal local com acesso ao Maven Central
- Skills: `tlc-spec-driven`, `superpowers:systematic-debugging`

**Done when**:

- [ ] A árvore resolve uma única versão coerente do OCI SDK.
- [ ] O projeto compila em Java 21 sem conflito de Jackson/Jersey.
- [ ] Os 168 testes backend permanecem verdes.

**Tests**: none — build/config conforme matriz  
**Gate**: Build  
**Commit**: `chore(config): adiciona SDK do OCI Generative AI`

### T8: Configurar autenticação e cliente OCI

**Status**: Complete  
**What**: Criar properties validadas e cliente OCI para config file local ou
Instance Principal na VM, com região, compartment, modelo, timeouts e retry
limitado.  
**Where**: `src/main/java/br/com/vistoriapredial/integration/oci/genai/config/`  
**Depends on**: T7  
**Reuses**: variáveis e policies documentadas em `infra/`.  
**Requirement**: IAR-01, IAR-06

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] `config_file` e `instance_principal` constroem o provider correto.
- [x] Região default é São Paulo; compartment e modelo são obrigatórios no provider OCI.
- [x] Retry não inclui 401/403/404 ou resposta inválida.
- [x] Health/config check não realiza chamada cobrada.
- [x] Pelo menos 8 testes unitários/contexto passam sem ler credenciais reais.

**Tests**: unit + contexto  
**Gate**: Backend full  
**Commit**: `feat(oci): configura cliente e autenticação do GenAI`

### T9: Implementar a chamada multimodal ao Gemini via OCI

**Status**: Complete  
**What**: Implementar `OciGenAiIntegrationService` com uma chamada por imagem,
contexto do ambiente, data URI, schema JSON e falhas tipadas.  
**Where**: `src/main/java/br/com/vistoriapredial/integration/oci/genai/OciGenAiIntegrationService.java`  
**Depends on**: T8  
**Reuses**: `StorageService`, factory T5 e cliente T8.  
**Requirement**: IAR-01, IAR-02, IAR-06

**Tools**:

- MCP: terminal e editor local; documentação oficial Oracle já validada
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `superpowers:systematic-debugging`

**Done when**:

- [x] A mensagem contém ambiente, categoria, instrução em português e imagem.
- [x] O modelo default é `google.gemini-2.5-flash` via On-Demand Serving Mode.
- [x] JPEG, PNG e WebP até 7 MB são aceitos; demais entradas falham antes da rede.
- [x] 401/403/404/429/5xx, timeout, corpo vazio e schema inválido recebem categoria explícita.
- [x] Logs contêm IDs/modelo/duração, nunca imagem, data URI ou payload integral.
- [x] Pelo menos 14 testes com cliente fake passam e nenhuma chamada real ocorre.

**Tests**: unit com fake  
**Gate**: Backend full  
**Commit**: `feat(oci): integra análise multimodal pelo Gemini`

### T10: Orquestrar contexto, validação e persistência

**Status**: Complete  
**What**: Fazer `VistoriaAnalysisProcessor` enviar a solicitação contextual,
validar a resposta, registrar o documento v2 e concluir o relatório de forma
idempotente e sem transação durante a rede.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/analysis/VistoriaAnalysisProcessor.java`  
**Depends on**: T9  
**Reuses**: `TransactionTemplate`, parser T4, porta T1 e estado T6.  
**Requirement**: IAR-01, IAR-02, IAR-03, IAR-06

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] Cada evidência enviada conserva imagem, ambiente e categoria corretos.
- [x] A chamada externa ocorre fora de transação de banco.
- [x] Somente JSON validado é persistido e disponibiliza relatório.
- [x] Falha real termina em `FALHA_IA`; não há fallback nem documento parcial.
- [x] Execução duplicada não duplica ou sobrescreve relatório concluído.
- [x] Pelo menos 9 testes unitários cobrem os ramos e a suíte completa passa.

**Tests**: unit  
**Gate**: Backend full  
**Commit**: `feat(ia): orquestra análise contextual e idempotente`

### T11: Transformar revisão em manifestação opcional

**Status**: Complete
**What**: Alterar o caso de uso de revisão para concordância, contestação ou
contexto opcional, permitido após o relatório e sem editar o achado da IA.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java`  
**Depends on**: T10  
**Reuses**: JSON de revisão, ownership, `RevisaoAchadoStore` e valores legados.  
**Requirement**: IAR-04

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] Novos valores representam concordância, contestação e contexto adicional.
- [x] Contestação exige 1–1000 caracteres; concordância pode ser breve.
- [x] Nenhuma ação altera tipo, gravidade, confiança ou resultado original.
- [x] Valores legados continuam legíveis.
- [x] Ownership e 403 continuam preservados.
- [x] Pelo menos 9 testes de aplicação passam.

**Tests**: unit  
**Gate**: Backend quick  
**Commit**: `feat(vistoria): registra manifestação sem reescrever a IA`

### T12: Expor análise v2 e manifestação na API

**Status**: Complete
**What**: Atualizar DTOs, mapper e endpoints para devolver o resultado completo
e aceitar manifestações opcionais mantendo ProblemDetail.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/web/`  
**Depends on**: T11  
**Reuses**: `VistoriaResponseMapper`, endpoint de revisão e segurança atuais.  
**Requirement**: IAR-03, IAR-04, IAR-05

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] O GET expõe execução, resultado geral, ambientes, achados e manifestações separadas.
- [x] O relatório é retornado sem revisão obrigatória.
- [x] Contestação válida persiste; outro usuário recebe 403; referência inválida recebe 404/422 conforme contrato.
- [x] Valores v1 continuam serializáveis.
- [x] Pelo menos 10 testes MockMvc novos/atualizados passam.

**Tests**: integração MockMvc  
**Gate**: Backend full  
**Commit**: `feat(api): expõe resultado visual e manifestações`

### T13: Alinhar o limite de evidência ao modelo

**Status**: Complete
**What**: Reduzir o limite da evidência para 7 MB em validação e multipart,
com erro claro e sem alterar os formatos aceitos.  
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/EvidenceFileValidator.java`  
**Depends on**: T12  
**Reuses**: validação de assinatura binária e ProblemDetail existentes.  
**Requirement**: IAR-02, IAR-05

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] Arquivo com 7 MB é aceito e com 7 MB + 1 byte é rejeitado.
- [x] Mensagens e configuração multipart informam 7 MB.
- [x] JPEG, PNG e WebP válidos continuam aceitos.
- [x] Pelo menos 5 testes de upload/erro passam.

**Tests**: unit + MockMvc  
**Gate**: Backend full  
**Commit**: `fix(upload): alinha evidências ao limite multimodal`

### T14: Impedir mock silencioso e documentar configuração runtime

**Status**: Complete
**What**: Tornar OCI o provider demonstrável, restringir mock a test/demo,
manter VLM opt-in e definir variáveis Oracle sem placeholders utilizáveis.  
**Where**: `src/main/resources/application.properties`  
**Depends on**: T13  
**Reuses**: perfis Spring, `MockIaIntegrationService` e configuração de teste.  
**Requirement**: IAR-01, IAR-06

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] Ambiente normal não inicia com mock implícito.
- [x] Testes continuam usando fake determinístico sem credencial OCI.
- [x] OCI sem compartment/configuração falha de forma clara antes de analisar.
- [x] Nenhum OCID, chave ou segredo real entra no repositório.
- [x] Testes de contexto cobrem `test`, `demo`, `oci` inválido e `vlm` explícito.

**Tests**: contexto  
**Gate**: Backend full  
**Commit**: `chore(config): torna o provedor de IA explícito`

### T15: Tipar e normalizar a análise v2 no frontend

**Status**: Complete
**What**: Atualizar os tipos e helpers para resultado geral, ambientes,
execução, novos campos de achado e compatibilidade v1 em português.  
**Where**: `frontend/src/features/inspections/types.ts`  
**Depends on**: T14  
**Reuses**: `ai-report.ts`, `evidence-context.ts` e contrato HTTP T12.  
**Requirement**: IAR-02, IAR-03, IAR-04, IAR-05

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

**Done when**:

- [x] Tipos fechados representam os quatro resultados e todos os metadados v2.
- [x] Helper localiza análise e resultado pelo ambiente/imagem corretos.
- [x] Taxonomia nunca exibe `stain` cru quando existe rótulo conhecido.
- [x] Dados v1 têm fallback legível, sem fabricar resultado aprovado.
- [x] Pelo menos 8 testes unitários passam.

**Tests**: unit TypeScript  
**Gate**: Frontend focused  
**Commit**: `feat(frontend): tipa resultado visual por ambiente`

### T16: Redesenhar a tela de resultado e manifestação

**Status**: Complete
**What**: Substituir a decisão obrigatória por navegação clara do resultado da
IA por ambiente, com feedback opcional que não altera a conclusão.  
**Where**: `frontend/src/features/inspections/client/inspection-review.tsx`  
**Depends on**: T15  
**Reuses**: evidência autenticada, contexto do ambiente e design system atual.  
**Requirement**: IAR-04

**Tools**:

- MCP: terminal, navegador controlado
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:audit`

**Done when**:

- [x] Cabeçalho e navegação deixam explícitos ambiente, evidência e resultado.
- [x] Observação mostra o que foi visto, motivo, impacto, confiança e recomendação.
- [x] Concordar, contestar e adicionar contexto são opcionais.
- [x] Contestação não remove nem renomeia o achado.
- [x] Teclado, foco, erro e conflito permanecem acessíveis.
- [x] Pelo menos 10 testes de componente novos/atualizados passam.

**Tests**: component  
**Gate**: Frontend full  
**Commit**: `feat(frontend): apresenta decisão da IA por ambiente`

### T17: Completar o relatório autoral da IA

**Status**: Complete

**What**: Reestruturar o relatório com resultado geral, motivo, resumo de
gravidades, ambientes, evidências, achados completos, limitações, manifestação e
metadados.  
**Where**: `frontend/src/features/inspections/client/inspection-report.tsx`  
**Depends on**: T16  
**Reuses**: agrupamento por ambiente, impressão e compartilhamento atuais.  
**Requirement**: IAR-03, IAR-04, IAR-05

**Tools**:

- MCP: terminal, navegador controlado
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:audit`

**Done when**:

- [x] Resultado geral e motivo aparecem antes dos detalhes.
- [x] Cada ambiente possui resultado e somente suas próprias evidências/observações.
- [x] Achados contestados continuam presentes com manifestação separada.
- [x] Inconclusivo orienta nova captura; ausência de achado usa texto explícito.
- [x] Impressão preserva identificação, resultados, evidências, metadados e aviso de escopo.
- [x] Pelo menos 12 testes de componente novos/atualizados passam.

**Tests**: component  
**Gate**: Frontend full  
**Commit**: `feat(relatorio): detalha conclusão visual por ambiente`

### T18: Alinhar captura, estilos e responsividade

**Status**: Complete
**What**: Atualizar limite visual para 7 MB e finalizar estilos dos novos
resultados, manifestações e impressão sem overflow desktop/mobile.  
**Where**: `frontend/src/features/inspections/client/inspection-workflow.tsx`  
**Depends on**: T17  
**Reuses**: `globals.css`, protocolo flexível e tokens Vistor.IA.  
**Requirement**: IAR-05

**Tools**:

- MCP: terminal, navegador controlado
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:audit`, `browser:control-in-app-browser`

**Done when**:

- [x] Arquivo com mais de 7 MB é bloqueado antes do upload com mensagem correta.
- [x] Interface informa JPEG/PNG/WebP até 7 MB.
- [x] Resultado, relatório e manifestação não têm overflow em 375, 768 e 1440 px.
- [x] Foco e contraste passam na auditoria visual; impressão mantém hierarquia.
- [x] Testes do workflow, lint e build passam; total frontend não fica abaixo de 110.

**Tests**: component + build + UAT visual  
**Gate**: Frontend full  
**Commit**: `fix(frontend): alinha captura e relatório responsivo`

### T19: Consolidar operação, rastreabilidade e aceite local

**Status**: Pending  
**What**: Atualizar decisão arquitetural, exemplos de ambiente, Docker e roteiro
de smoke opt-in; executar os gates e UAT local sem serviço pago.  
**Where**: `.specs/STATE.md`  
**Depends on**: T18  
**Reuses**: `infra/docs/onboarding-backend-dev.md`, Docker Compose, spec e design.  
**Requirement**: IAR-01, IAR-06

**Tools**:

- MCP: terminal e navegador controlado
- Skills: `tlc-spec-driven`, `superpowers:verification-before-completion`, `product-design:audit`, `browser:control-in-app-browser`

**Done when**:

- [ ] AD-002 é supersedida apenas para GenAI pelo SDK oficial; AD-003 permanece configurável.
- [ ] `.env.example`/Docker documentam auth mode, região, compartment e modelo sem segredo.
- [ ] Smoke OCI é explicitamente opt-in e usa fixture não pessoal.
- [ ] Backend tem pelo menos 168 testes, frontend pelo menos 110, lint e build passam.
- [ ] UAT local comprova falha real sem OCI, ausência de fallback, resultado por ambiente, manifestação opcional e relatório.
- [ ] Bloqueio de acesso/cota OCI, se existir, é registrado como bloqueio externo e não como PASS real.

**Tests**: integração + UAT local  
**Gate**: Integrated  
**Commit**: `docs(ia): documenta operação e aceite do OCI`

## Phase Execution Map

```text
Phase 1: T1 -> T2 -> T3 -> T4 -> T5 -> T6
Phase 2: T7 -> T8 -> T9 -> T10
Phase 3: T11 -> T12 -> T13 -> T14
Phase 4: T15 -> T16 -> T17 -> T18
Phase 5: T19
```

## Task Granularity Check

| Task | Reviewable deliverable | Status |
| --- | --- | --- |
| T1 | Um contrato de solicitação contextual | ✅ Granular |
| T2 | Um documento canônico v2 | ✅ Granular |
| T3 | Um calculador determinístico | ✅ Granular |
| T4 | Um parser v2 compatível | ✅ Granular |
| T5 | Um montador de documento | ✅ Granular |
| T6 | Uma transição de domínio | ✅ Granular |
| T7 | Uma configuração de dependências | ✅ Granular |
| T8 | Um cliente OCI configurado | ✅ Granular |
| T9 | Um adaptador multimodal | ✅ Granular |
| T10 | Um processador orquestrado | ✅ Granular |
| T11 | Um caso de uso de manifestação | ✅ Granular |
| T12 | Um contrato HTTP atualizado | ✅ Granular |
| T13 | Um limite de upload coerente | ✅ Granular |
| T14 | Uma seleção segura de provider | ✅ Granular |
| T15 | Um contrato frontend v2 | ✅ Granular |
| T16 | Uma tela de resultado e feedback | ✅ Granular |
| T17 | Um relatório completo | ✅ Granular |
| T18 | Um fluxo responsivo de captura/resultado | ✅ Granular |
| T19 | Um handoff operacional verificável | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | início da Phase 1 | ✅ Match |
| T2 | T1 | T1 -> T2 | ✅ Match |
| T3 | T2 | T2 -> T3 | ✅ Match |
| T4 | T3 | T3 -> T4 | ✅ Match |
| T5 | T4 | T4 -> T5 | ✅ Match |
| T6 | T5 | T5 -> T6 | ✅ Match |
| T7 | T6 | Phase 1 -> T7 | ✅ Match |
| T8 | T7 | T7 -> T8 | ✅ Match |
| T9 | T8 | T8 -> T9 | ✅ Match |
| T10 | T9 | T9 -> T10 | ✅ Match |
| T11 | T10 | Phase 2 -> T11 | ✅ Match |
| T12 | T11 | T11 -> T12 | ✅ Match |
| T13 | T12 | T12 -> T13 | ✅ Match |
| T14 | T13 | T13 -> T14 | ✅ Match |
| T15 | T14 | Phase 3 -> T15 | ✅ Match |
| T16 | T15 | T15 -> T16 | ✅ Match |
| T17 | T16 | T16 -> T17 | ✅ Match |
| T18 | T17 | T17 -> T18 | ✅ Match |
| T19 | T18 | Phase 4 -> T19 | ✅ Match |

## Test Co-location Validation

| Task | Code layer modified | Matrix requires | Task says | Status |
| --- | --- | --- | --- | --- |
| T1 | Application contract | unit | unit | ✅ OK |
| T2 | Analysis contract | unit | unit | ✅ OK |
| T3 | Business rule | unit all branches | unit | ✅ OK |
| T4 | Parser | unit edge cases | unit | ✅ OK |
| T5 | Payload factory | unit payload assertions | unit | ✅ OK |
| T6 | Domain state | unit | unit | ✅ OK |
| T7 | Build config | none/build | none/build | ✅ OK |
| T8 | OCI config | unit + context | unit + context | ✅ OK |
| T9 | External adapter | unit fake | unit fake | ✅ OK |
| T10 | Application processor | unit | unit | ✅ OK |
| T11 | Application service | unit | unit | ✅ OK |
| T12 | Web/API | MockMvc integration | MockMvc integration | ✅ OK |
| T13 | Upload/API | unit + MockMvc | unit + MockMvc | ✅ OK |
| T14 | Runtime config | context | context | ✅ OK |
| T15 | TypeScript helpers | unit | unit | ✅ OK |
| T16 | React result UI | component | component | ✅ OK |
| T17 | React report UI | component | component | ✅ OK |
| T18 | React/CSS/config | component + build + UAT | component + build + UAT | ✅ OK |
| T19 | Docs/integration | integration + UAT | integration + UAT | ✅ OK |
