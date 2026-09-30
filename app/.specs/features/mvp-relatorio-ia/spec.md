# MVP Relatório de vistoria por IA Specification

## Problem Statement

O produto atual coleta imagens e exibe o retorno bruto da IA, mas a experiência
e os contratos ainda carregam a antiga homologação por engenheiro e não permitem
que o usuário confirme o contexto dos achados antes do documento final. O MVP
deve transformar essa PoC em uma jornada IA-first rastreável: capturar evidências,
processar sem bloquear a navegação, revisar cada achado e gerar um Relatório de
vistoria por IA sem prometer laudo técnico ou diagnóstico profissional.

## Goals

- [ ] Permitir que o usuário conclua a jornada principal sem entrar em fila de engenheiro.
- [ ] Preservar o vínculo entre foto, achado da IA, contexto confirmado e trecho do relatório.
- [ ] Processar a análise fora da requisição HTTP e representar falha, retomada e concorrência reais.
- [ ] Entregar uma interface autoral, responsiva e acessível baseada no protótipo aprovado.
- [ ] Manter frontend, API e persistência alinhados por contratos tipados e testes derivados desta especificação.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Laudo técnico, ART, assinatura profissional ou conformidade NBR | O produto registra indícios visuais e contexto do usuário, sem assumir responsabilidade técnica. |
| Remoção destrutiva das colunas, papéis e endpoints legados de engenharia | Exige estratégia separada de migração de dados e compatibilidade. O fluxo legado fica sem destaque, mas não será apagado nesta entrega. |
| Integração real paga com OCI ou outro modelo | A porta de IA existente será preservada e os testes usarão mock/fake. |
| Geração binária de PDF no servidor | O MVP oferece impressão/salvamento pelo navegador e compartilhamento quando a plataforma suportar. |
| Editor administrativo de roteiro | O roteiro continua versionado no código nesta entrega. |
| Sincronização offline completa | Evidências confirmadas pelo servidor são preservadas; fila local durável entre reinicializações fica para evolução posterior. |
| Diagnóstico automático, percentual de precisão ou recomendação prescritiva | Não há base técnica validada para essas promessas. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Papel principal do MVP | O usuário proprietário conduz captura, revisão de contexto e geração do relatório; engenheiro não aparece na jornada principal. | Decisão explícita do usuário: a função central do MVP é a IA produzir o relatório. | y |
| Direção visual | Aplicar o sistema `Clareza Técnica` e o conceito `Evidência Viva` do protótipo em `../docs/design/prototype/index.html`, com cobalto, jade, superfícies claras e modo de campo escuro. | Direção visual já aprovada e materializada em artefato navegável. | y |
| Registro do contexto | Persistir revisões como JSON tipado na própria vistoria, identificado por `imagemId + indiceAchado`. | Mantém a análise original imutável e evita uma modelagem relacional prematura no MVP. | n |
| Legado de engenharia | Preservar código e schema legados por compatibilidade, mas remover referências de engenharia da navegação, cadastro público, metadata e fluxo do cliente. | Evita uma remoção destrutiva enquanto alinha a experiência ao produto atual. | n |
| Roteiro inicial | Usar cinco grupos fixos e doze itens já definidos no frontend, expandindo a validação do backend para os mesmos códigos. | O schema já aceita código textual; a expansão entrega ambientes sem nova entidade administrativa. | n |
| Processamento assíncrono | Publicar evento após o commit da submissão e processar a IA em executor Spring separado. | A requisição não deve manter conexão HTTP nem transação de banco durante a chamada externa. | n |
| Achados antigos sem revisão | Vistorias legadas `CONCLUIDA` continuam legíveis como resultado histórico; somente novas análises entram em `REVISAO_PENDENTE`. | Mantém compatibilidade sem fabricar decisões do usuário. | n |
| Documento final | Derivar o relatório do JSON da IA mais as revisões persistidas e usar impressão do navegador para salvar em PDF. | Evita dependência nova e entrega um documento útil dentro do escopo do MVP. | n |
| Compartilhamento | Usar `navigator.share` quando disponível e copiar o link como fallback. | É progressivo, sem serviço externo ou permissão adicional. | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Acesso e início IA-first ⭐ MVP

**Requirement ID:** AIR-01

**User Story**: Como responsável por um imóvel, quero acessar um início simples e orientado à próxima ação para retomar ou iniciar uma vistoria sem escolher um papel técnico.

**Why P1**: A proposta do MVP precisa estar clara antes da captura.

**Acceptance Criteria**:

1. The frontend SHALL exibir `Vistor.IA` e descrever o produto como organização de evidências e análise visual por IA, sem engenheiro, homologação ou parecer profissional na navegação, cadastro público, metadata e fluxo do cliente.
2. WHEN um visitante criar uma conta pela interface THEN the frontend SHALL enviar `ROLE_CLIENTE` sem apresentar seletor de perfil, CREA ou código de convite.
3. WHEN um cliente autenticado abrir `/client` THEN the frontend SHALL destacar uma única próxima ação compatível com a vistoria mais recente e SHALL listar relatórios depois da tarefa principal.
4. IF o cliente não possuir vistorias THEN the frontend SHALL explicar o resultado esperado e apresentar uma única ação para iniciar a primeira vistoria.
5. IF a leitura inicial falhar THEN the frontend SHALL preservar a sessão, informar que os dados permanecem seguros e oferecer nova tentativa no mesmo contexto.

**Independent Test**: Cadastrar um cliente, abrir o início vazio e com dados, e comprovar que não há referência nem escolha de engenheiro no fluxo principal.

---

### P1: Captura guiada e análise assíncrona ⭐ MVP

**Requirement ID:** AIR-02

**User Story**: Como usuário em campo, quero registrar fotos por ambiente com orientação objetiva e poder sair durante a análise para não ficar preso à tela.

**Why P1**: Evidências utilizáveis e processamento confiável são a entrada do relatório.

**Acceptance Criteria**:

1. WHEN o cliente criar uma vistoria com endereço entre 1 e 255 caracteres THEN the backend SHALL persistir um rascunho `EM_RASCUNHO` pertencente ao cliente autenticado.
2. WHILE a vistoria estiver `EM_RASCUNHO` ou `FALHA_IA` the frontend SHALL apresentar os doze itens do roteiro agrupados em Sala, Cozinha, Banheiro, Quarto e Instalações, com progresso derivado somente das evidências retornadas pela API.
3. WHEN o cliente selecionar um item THEN the frontend SHALL permitir câmera ou galeria para enviar JPEG, PNG ou WebP de até 10 MB com o código exato do roteiro.
4. IF o arquivo estiver vazio, exceder 10 MB, tiver MIME não permitido ou assinatura incompatível THEN the integrated system SHALL rejeitá-lo sem apagar evidências válidas e SHALL exibir a causa junto à captura.
5. WHEN o servidor confirmar um upload THEN the frontend SHALL atualizar miniatura, contagem e próximo item sem criar outra vistoria.
6. WHEN o cliente submeter uma vistoria com ao menos uma evidência THEN the backend SHALL responder HTTP 202 com status `AGUARDANDO_IA` antes de iniciar a chamada externa.
7. WHILE a vistoria estiver `AGUARDANDO_IA` the frontend SHALL informar que é seguro sair e retornar e SHALL consultar o estado sem fabricar porcentagem de progresso.
8. WHEN a IA concluir com JSON válido THEN the backend SHALL persistir a análise original e transicionar a vistoria para `REVISAO_PENDENTE`.
9. IF a IA falhar, expirar ou retornar documento inválido THEN the backend SHALL preservar as evidências, registrar `FALHA_IA` e não SHALL persistir relatório fabricado.
10. WHEN uma submissão for repetida enquanto a vistoria já estiver `AGUARDANDO_IA` THEN the backend SHALL retornar o estado atual sem agendar outra análise.

**Independent Test**: Criar rascunho, enviar evidência válida, rejeitar arquivo inválido, submeter duas vezes e observar uma única análise terminando em revisão ou falha real.

---

### P1: Revisão rastreável dos achados ⭐ MVP

**Requirement ID:** AIR-03

**User Story**: Como usuário, quero confirmar, corrigir ou rejeitar cada achado ao lado da foto de origem para que o relatório reflita o contexto que eu realmente observei.

**Why P1**: A IA não pode transformar uma inferência visual em conclusão silenciosa.

**Acceptance Criteria**:

1. WHILE a vistoria estiver `REVISAO_PENDENTE` the frontend SHALL apresentar um achado por vez com foto dominante, ambiente, marcador, observação da IA, limitações e pergunta de contexto.
2. The integrated system SHALL identificar uma revisão pelo par imutável `imagemId + indiceAchado` e SHALL manter a análise original inalterada.
3. WHEN o cliente confirmar um achado THEN the backend SHALL persistir decisão `CONFIRMADO` e contexto não vazio de até 1000 caracteres.
4. WHEN o cliente corrigir um achado THEN the backend SHALL persistir decisão `CORRIGIDO`, contexto não vazio e tipo corrigido entre 1 e 80 caracteres.
5. WHEN o cliente rejeitar um achado THEN the backend SHALL persistir decisão `REJEITADO` e justificativa não vazia de até 1000 caracteres sem apagar a evidência.
6. IF `imagemId` não pertencer à vistoria, `indiceAchado` não existir ou a vistoria não estiver em revisão THEN the backend SHALL responder com `404` ou `409` em `application/problem+json` sem alterar revisões existentes.
7. IF outro usuário tentar revisar a vistoria THEN the backend SHALL responder 403 em `application/problem+json`.
8. WHEN o mesmo achado for revisado novamente pelo proprietário THEN the backend SHALL substituir somente a revisão desse par, sem duplicá-la.
9. IF houver mais de 100 achados na análise THEN the backend SHALL bloquear a revisão com erro de contrato e não SHALL truncar silenciosamente os dados.

**Independent Test**: Revisar o mesmo achado como confirmado e depois corrigido, verificar o upsert, rejeitar outro, e provar ownership e índices inválidos.

---

### P1: Relatório de vistoria por IA ⭐ MVP

**Requirement ID:** AIR-04

**User Story**: Como usuário, quero gerar um documento legível a partir dos achados revisados para baixar, imprimir ou compartilhar o registro da vistoria.

**Why P1**: O relatório é o resultado principal e nomeado do produto.

**Acceptance Criteria**:

1. WHEN todos os achados tiverem uma revisão persistida THEN o cliente SHALL poder concluir a revisão por `POST /api/vistorias/{id}/relatorio`.
2. IF existir achado sem revisão THEN the backend SHALL responder 409 em `application/problem+json` e SHALL manter `REVISAO_PENDENTE`.
3. WHEN a conclusão for aceita THEN the backend SHALL registrar `RELATORIO_DISPONIVEL`, data de conclusão e responder o estado atualizado.
4. WHEN a conclusão for repetida em `RELATORIO_DISPONIVEL` THEN the backend SHALL retornar o mesmo recurso sem criar outro relatório.
5. WHILE o relatório estiver disponível the frontend SHALL ligar cada achado confirmado ou corrigido à foto, ao contexto e ao identificador da evidência.
6. WHILE o relatório estiver disponível the frontend SHALL excluir achados rejeitados das conclusões e SHALL manter suas fotos no conjunto de evidências.
7. The frontend SHALL nomear o documento `Relatório de vistoria por IA` e SHALL exibir que ele não constitui laudo técnico, diagnóstico estrutural ou certificação de conformidade.
8. WHEN o usuário solicitar download THEN the frontend SHALL abrir uma versão própria para impressão que possa ser salva como PDF pelo navegador.
9. WHERE `navigator.share` estiver disponível, WHEN o usuário solicitar compartilhamento THEN the frontend SHALL abrir o compartilhamento nativo; caso contrário SHALL copiar o link atual e informar o resultado.

**Independent Test**: Tentar concluir com revisão incompleta, revisar tudo, concluir duas vezes e conferir documento, rastreabilidade, impressão e fallback de compartilhamento.

---

### P1: Contratos seguros e estados íntegros ⭐ MVP

**Requirement ID:** AIR-05

**User Story**: Como usuário, quero que o sistema preserve meus dados e represente o estado real para confiar no que aparece na interface.

**Why P1**: O fluxo cruza banco, arquivos e IA externa e não pode produzir sucesso parcial invisível.

**Acceptance Criteria**:

1. The backend SHALL expor DTOs imutáveis e não SHALL expor caminho físico, segredo, stack trace ou entidade JPA nos contratos HTTP.
2. The backend SHALL aceitar somente as transições `EM_RASCUNHO|FALHA_IA → AGUARDANDO_IA → REVISAO_PENDENTE → RELATORIO_DISPONIVEL`, preservando estados legados apenas para leitura e compatibilidade.
3. IF duas requisições concorrentes alterarem a mesma vistoria THEN the backend SHALL transformar a falha de versão otimista em HTTP 409.
4. IF o JSON da IA estiver ausente, malformado ou fora do contrato esperado THEN the backend SHALL bloquear revisão e conclusão sem interpretar texto livre como achado.
5. The backend SHALL registrar somente o identificador da vistoria e a categoria da falha operacional, sem conteúdo integral de imagem, token, contexto do usuário ou payload completo da IA.
6. The frontend SHALL usar o cliente HTTP compartilhado, manter sessão em 401 somente até emitir o evento de expiração e SHALL exibir `ProblemDetail.detail` sem depender de logs do navegador.
7. The system SHALL NOT chamar provedor real de IA em testes automatizados.

**Independent Test**: Exercitar transições permitidas e proibidas, concorrência, JSON inválido, ownership e falha externa usando mocks sem credenciais.

---

### P1: Sistema visual responsivo e acessível ⭐ MVP

**Requirement ID:** AIR-06

**User Story**: Como usuário, quero uma interface legível no celular e expansível no desktop para concluir a vistoria em campo e revisar o relatório com clareza.

**Why P1**: O produto é usado com atenção dividida e a interface precisa ensinar o fluxo.

**Acceptance Criteria**:

1. The frontend SHALL aplicar os tokens definidos no protótipo: fundo `#F6F8FC`, superfície `#FFFFFF`, texto `#172033`, texto secundário `#596579`, primária `#2F5BEA`, evidência `#08705F`, atenção `#B86700`, erro `#B83B4B` e borda `#D8DFEA`.
2. The frontend SHALL usar tipografia sem serifa em toda a experiência e SHALL restringir tipografia monoespaçada a IDs, datas, horários e metadados.
3. WHEN a viewport tiver 390 px, 768 px ou 1440 px THEN the frontend SHALL preservar captura, revisão e relatório sem rolagem horizontal da página.
4. WHILE a viewport for móvel the frontend SHALL manter ações primárias com alvo mínimo de 44 por 44 px e SHALL priorizar fotografia e uma decisão por tela.
5. The frontend SHALL oferecer labels persistentes, foco visível, nomes acessíveis em botões de ícone e estados comunicados por texto ou ícone além da cor.
6. IF o usuário preferir movimento reduzido THEN the frontend SHALL remover animações não essenciais.
7. WHILE conteúdo assíncrono estiver carregando the frontend SHALL usar skeleton ou estrutura estável e SHALL anunciar mudanças relevantes em região viva.
8. IF um estado estiver vazio ou falhar THEN the frontend SHALL explicar o que aconteceu e apresentar somente uma próxima ação funcional.

**Independent Test**: Percorrer cadastro, captura, revisão e relatório por teclado e nos três breakpoints, conferindo foco, toque, estado e ausência de overflow.

---

## Edge Cases

- IF o usuário atualizar a página durante um rascunho THEN the frontend SHALL reconstruir o progresso pelas evidências persistidas.
- IF a sessão expirar durante upload, revisão ou conclusão THEN the frontend SHALL encerrar a sessão e não SHALL repetir automaticamente a mutação após novo login.
- IF uma evidência não puder ser carregada THEN the frontend SHALL identificar somente aquela foto como indisponível sem esconder os demais achados.
- IF a análise não contiver achados THEN the frontend SHALL permitir concluir a revisão e SHALL gerar relatório com a seção `Nenhum indício visual registrado`.
- IF o compartilhamento nativo e a área de transferência estiverem indisponíveis THEN the frontend SHALL informar que o compartilhamento não está disponível sem marcar sucesso.
- IF o navegador negar câmera THEN the frontend SHALL manter a galeria como alternativa e informar como liberar a permissão.

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Endereço de 1–255 caracteres; imagem JPEG/PNG/WebP de até 10 MB com assinatura; contexto de 1–1000; tipo corrigido de 1–80; máximo de 100 achados. |
| Failure / partial-failure states | Upload isolado, `FALHA_IA`, evidência indisponível, revisão incompleta e compartilhamento indisponível preservam todo dado previamente confirmado. |
| Idempotency / retry / duplicate handling | Submissão em `AGUARDANDO_IA` não reage agenda; revisão faz upsert por imagem+índice; conclusão em `RELATORIO_DISPONIVEL` retorna o mesmo recurso. |
| Auth boundaries & rate limits | Todas as operações da vistoria exigem `ROLE_CLIENTE` e ownership. Rate limiting é N/A porque não será introduzido neste processo local; proteção de borda pertence ao deploy. |
| Concurrency / ordering | `@Version` protege alterações e conflitos viram 409; evento de IA só é publicado depois do commit que registra `AGUARDANDO_IA`. |
| Data lifecycle / expiry | N/A nesta feature porque não haverá exclusão, expiração ou arquivamento; dados legados serão preservados. |
| Observability | Logs registram id da vistoria e categoria da falha, sem dados pessoais, imagens, revisão ou payload integral. |
| External-dependency failure | Timeout ou erro da porta `IaIntegrationService` resulta em `FALHA_IA`, sem relatório parcial ou sucesso fabricado. |
| State-transition integrity | Somente rascunho/falha submete; somente revisão pendente aceita revisão; somente revisão completa conclui; relatório disponível é terminal e idempotente. |

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| AIR-01 | P1: Acesso e início IA-first | Tasks T4, T5, T8 | Planned |
| AIR-02 | P1: Captura guiada e análise assíncrona | Tasks T1, T2, T5, T8 | Planned |
| AIR-03 | P1: Revisão rastreável dos achados | Tasks T3, T6, T8 | Planned |
| AIR-04 | P1: Relatório de vistoria por IA | Tasks T3, T7, T8 | Planned |
| AIR-05 | P1: Contratos seguros e estados íntegros | Tasks T1, T2, T3, T8 | Planned |
| AIR-06 | P1: Sistema visual responsivo e acessível | Tasks T4, T5, T6, T7, T8 | Planned |

**Coverage:** 6 total, 6 mapped to tasks, 0 unmapped.

## Success Criteria

- [ ] Um cliente conclui o caminho cadastro → captura → análise → revisão → relatório sem contato com engenheiro.
- [ ] Toda conclusão do relatório aponta para uma evidência e uma revisão persistida.
- [ ] Nenhuma chamada HTTP de submissão permanece aberta durante o processamento da IA.
- [ ] As suítes backend e frontend, lint, build e migration PostgreSQL passam em execução atual.
- [ ] Captura, revisão e relatório passam pela auditoria em 390, 768 e 1440 px sem overflow horizontal.
