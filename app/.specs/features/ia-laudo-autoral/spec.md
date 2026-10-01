# IA autoral e relatório verificável — Especificação

## Problem Statement

O repositório possui uma prova de conceito visual com Qwen e portas Java
aproveitáveis, enquanto o repositório de infraestrutura definiu autenticação,
policies, região e scripts para OCI Generative AI. Porém, o aplicativo ainda
não possui o adaptador Oracle e a execução local padrão seleciona um mock que
devolve achados fixos. Além disso, a jornada atual permite que a manifestação
do responsável substitua ou exclua a conclusão da IA, o que contradiz a
proposta do produto: a IA deve produzir o resultado automatizado e o cliente
deve apenas acrescentar contexto ou registrar discordância.

Esta feature continua o caminho definido pelo grupo: integra a aplicação ao
OCI Generative AI por evidência e ambiente, produz resultado automatizado
padronizado, disponibiliza o relatório após a análise e registra a
rastreabilidade do modelo, sem apresentar o documento como laudo profissional,
ART ou diagnóstico definitivo.

## Goals

- [ ] Implementar o OCI Generative AI como provedor real, reutilizando autenticação, região, compartment e policies definidos no repositório `infra`.
- [ ] Padronizar achados, resultado por ambiente e resultado geral em contrato validado pelo backend.
- [ ] Tornar a IA autora do resultado automatizado e preservar manifestações do cliente como camada separada.
- [ ] Gerar um relatório completo, rastreável por ambiente, evidência e execução do modelo.
- [ ] Comprovar o caminho navegador → backend → OCI Generative AI → relatório com imagem controlada e estados de falha reais.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Laudo técnico legal, ART, assinatura profissional ou garantia de conformidade | A análise é visual e automatizada; validade profissional exige responsável técnico e processo próprio. |
| Treinamento ou fine-tuning de modelo | O MVP consome um modelo multimodal disponível no OCI Generative AI; o Qwen permanece apenas como referência técnica local. |
| Migração para Object Storage e Autonomous Database | A feature integra somente a IA; armazenamento e banco possuem especificação própria e credenciais distintas. |
| Diagnóstico de causa oculta ou recomendação estrutural definitiva | Fotografias não comprovam origem, extensão interna ou risco estrutural. |
| Remoção destrutiva de dados e valores legados de revisão | Compatibilidade será preservada; limpeza exige feature e migration próprias. |
| Aplicativo móvel nativo ou captura offline durável | A entrega permanece web responsiva. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Autoridade do resultado | A IA produz o resultado automatizado; o responsável pode concordar, contestar ou contextualizar sem sobrescrevê-lo. | Decisão explícita do usuário. | y |
| Provedor real | OCI Generative AI, atrás da porta `IaIntegrationService`, usando o SDK oficial e o mesmo modelo de autenticação da infraestrutura do grupo. | Decisão explícita do usuário e alinhamento com `infra/docs/spec-backend.md`. | y |
| Autenticação | `ConfigFileAuthenticationDetailsProvider` no desenvolvimento e `InstancePrincipalsAuthenticationDetailsProvider` na VM, selecionados por `OCI_AUTH_MODE`. | É o fluxo provisionado pelo grupo; evita credenciais na VM. | y |
| Região e compartment | `sa-saopaulo-1` e compartment configurável por `OCI_COMPARTMENT_ID`; nenhum OCID de usuário ou chave será versionado. | Preserva a infraestrutura compartilhada e segredos fora do código. | y |
| Modelo multimodal | Usar `google.gemini-2.5-flash` sob demanda via OCI como configuração inicial do MVP, mantendo o identificador do modelo configurável. | Decisão confirmada pelo usuário. O Llama 4 Scout citado pelo grupo está restrito a cluster dedicado em São Paulo; o Gemini está disponível sob demanda, mas usa processamento externo do Google no Brasil. | y |
| Resultado padronizado | `APROVADO`, `APROVADO_COM_RESSALVAS`, `NAO_APROVADO` ou `INCONCLUSIVO`, sempre rotulado como análise visual automatizada. | Entrega decisão clara sem prometer validade profissional. | y |
| Agregação do resultado | O backend deriva o resultado por ambiente e geral a partir da qualidade das evidências e da maior gravidade validada dos achados da IA. | Evita texto livre decidindo transição de estado e mantém regra auditável. | y |
| Regra de agregação | Evidência insuficiente sem outra evidência válida torna o ambiente `INCONCLUSIVO`; achado alto/crítico torna `NAO_APROVADO`; achado baixo/médio torna `APROVADO_COM_RESSALVAS`; ausência de achados em evidências válidas torna `APROVADO`. O resultado geral usa a condição mais restritiva dos ambientes. | Regra conservadora, determinística e compreensível no relatório. | y |
| Disponibilidade do relatório | Análise válida disponibiliza o relatório imediatamente; manifestação do cliente é opcional e pode ser adicionada depois. | O documento é produzido pela IA, não aprovado pelo cliente. | y |
| Uso do mock | Permitido apenas em testes automatizados e em perfil de demonstração explicitamente identificado; nunca como fallback. | Impede sucesso fabricado e repetição do problema observado. | y |
| Imagens de validação | Testes automatizados usam fixtures locais; o smoke test real envia somente uma imagem controlada e não pessoal ao OCI após acesso e custo autorizados. | Evita usar fotografia real de cliente na primeira validação da integração. | y |
| Histórico da execução | Registrar provedor, modelo, versão do prompt, identificador da análise e instante de conclusão; não registrar imagem, token ou payload integral. | Rastreabilidade com minimização de dados. | y |
| Compatibilidade de manifestações antigas | `CONFIRMADO`, `CORRIGIDO` e `REJEITADO` continuam legíveis, mas não alteram achado nem resultado da IA no relatório novo. | Preserva dados sem perpetuar autoridade incorreta. | y |

**Open questions:** none — decisões ainda não confirmadas usam defaults conservadores e reversíveis descritos acima.

---

## User Stories

### P1: OCI real sem sucesso simulado ⭐ MVP

**Requirement ID:** IAR-01

**User Story**: Como usuário, quero que minhas imagens sejam realmente analisadas para confiar que o resultado corresponde às evidências enviadas.

**Why P1**: Sem execução real, o produto apenas simula sua função central.

**Acceptance Criteria**:

1. WHEN o ambiente demonstrável iniciar THEN the system SHALL selecionar o provedor `oci`, validar região, compartment, modelo e modo de autenticação e SHALL construir o cliente oficial do OCI Generative AI antes de aceitar nova análise.
2. IF o provedor real estiver indisponível, expirar ou devolver resposta inválida THEN the backend SHALL registrar `FALHA_IA`, preservar as evidências e SHALL NOT produzir achado ou relatório simulado.
3. WHERE o perfil de teste automatizado estiver ativo, the system SHALL permitir um fake determinístico sem carregar o modelo real.
4. IF o provedor `mock` for selecionado fora dos perfis explicitamente permitidos THEN the backend SHALL falhar de forma explícita antes de processar uma vistoria.
5. The frontend SHALL identificar análises simuladas como demonstração e SHALL NOT exibi-las como resultado real.

6. WHERE `OCI_AUTH_MODE=instance_principal` estiver ativo, the system SHALL usar o dynamic group e as policies da VM sem exigir chave de API local.
7. WHERE o modo local estiver ativo, the system SHALL ler o profile configurado em `~/.oci/config` sem copiar a chave para o repositório ou imagem Docker.

**Independent Test**: Executar sucesso, indisponibilidade, autenticação inválida e resposta inválida do adaptador; comprovar que somente o sucesso OCI chega ao relatório e que nenhum erro cai no mock.

### P1: Contrato visual estruturado por ambiente ⭐ MVP

**Requirement ID:** IAR-02

**User Story**: Como usuário, quero entender o que foi observado em cada cômodo e em qual foto para avaliar o imóvel sem resultados genéricos.

**Why P1**: A utilidade do relatório depende da ligação inequívoca entre ambiente, imagem e observação.

**Acceptance Criteria**:

1. WHEN o modelo multimodal analisar uma evidência THEN the OCI adapter SHALL exigir identificador da análise, qualidade da imagem, achados em português, área, tipo, descrição, evidência visual, localização, gravidade, confiança, critério avaliado, motivo do resultado, impacto, recomendação e limitações.
2. The backend SHALL associar cada resposta à `imagemId`, ao código e ao nome do ambiente fornecidos pelo cadastro da vistoria, sem inferir o ambiente apenas pela imagem.
3. IF um achado usar enum desconhecido, campo obrigatório vazio, texto acima do limite ou referência de imagem inexistente THEN the backend SHALL rejeitar o documento integral como análise inválida.
4. WHEN um ambiente possuir múltiplas evidências válidas THEN the backend SHALL consolidar os achados sem mover observações entre imagens ou ambientes.
5. IF uma evidência não permitir avaliação confiável THEN the analysis SHALL marcá-la como insuficiente, SHALL explicar objetivamente a limitação e SHALL indicar qual nova captura é necessária.
6. The integrated system SHALL limitar uma análise a 100 achados e textos a limites documentados, sem truncamento silencioso.

**Independent Test**: Analisar evidências de dois ambientes, conferir a identidade de cada imagem e rejeitar um documento com referência ou enum inválido.

### P1: Resultado automatizado claro e auditável ⭐ MVP

**Requirement ID:** IAR-03

**User Story**: Como usuário, quero receber uma conclusão clara da IA por ambiente e para o imóvel para saber o resultado sem ter que decidir no lugar do sistema.

**Why P1**: A decisão automatizada é o valor central solicitado para o MVP.

**Acceptance Criteria**:

1. WHEN a análise de um ambiente terminar com evidências visualmente suficientes THEN the backend SHALL derivar exatamente um resultado `APROVADO`, `APROVADO_COM_RESSALVAS` ou `NAO_APROVADO` pela regra de agregação documentada.
2. IF nenhuma evidência de um ambiente for suficiente THEN the backend SHALL definir o resultado do ambiente como `INCONCLUSIVO` e SHALL NOT convertê-lo em aprovação.
3. WHEN todos os ambientes forem consolidados THEN the backend SHALL derivar o resultado geral pela condição mais restritiva na ordem `NAO_APROVADO`, `INCONCLUSIVO`, `APROVADO_COM_RESSALVAS`, `APROVADO`.
4. The system SHALL persistir provedor, modelo, versão do prompt, identificador da análise e instante da conclusão junto ao documento original validado.
5. The frontend SHALL exibir o resultado como `Resultado da análise visual por IA` e SHALL explicar em linguagem simples o que passou ou não passou, o critério avaliado e o motivo da decisão.
6. The system SHALL NOT apresentar o resultado como diagnóstico estrutural, conformidade normativa ou documento com responsabilidade técnica.

**Independent Test**: Exercitar cada gravidade e qualidade de evidência e conferir os quatro resultados possíveis por ambiente e no agregado.

### P1: Manifestação do responsável sem reescrever a IA ⭐ MVP

**Requirement ID:** IAR-04

**User Story**: Como responsável pelo imóvel, quero registrar concordância, discordância ou contexto adicional sem apagar o que a IA concluiu.

**Why P1**: O contexto humano agrega informação, mas não pode fabricar ou ocultar o resultado automatizado.

**Acceptance Criteria**:

1. WHILE um relatório estiver disponível the frontend SHALL oferecer `Concordo`, `Contestar análise` e `Adicionar contexto` junto ao achado correspondente.
2. WHEN o responsável contestar um achado THEN the backend SHALL exigir justificativa de 1 a 1000 caracteres e SHALL preservar tipo, gravidade, confiança, descrição e resultado originais.
3. WHEN o responsável adicionar contexto THEN the backend SHALL persistir o texto como manifestação separada vinculada a `imagemId + indiceAchado`.
4. WHEN uma manifestação for atualizada THEN the backend SHALL substituir somente a manifestação daquele par e SHALL manter o histórico da execução da IA inalterado.
5. IF outro usuário tentar manifestar-se sobre a vistoria THEN the backend SHALL responder 403 em `application/problem+json` sem alteração de estado.
6. WHERE existirem decisões legadas THEN the frontend SHALL apresentá-las como manifestação histórica e SHALL NOT usá-las para remover achados do relatório.

**Independent Test**: Contestar um achado, atualizar seu contexto e comprovar que a conclusão e o resultado da IA permanecem idênticos.

### P1: Relatório completo por cômodo ⭐ MVP

**Requirement ID:** IAR-05

**User Story**: Como usuário, quero um relatório que mostre claramente o resultado geral, os resultados por cômodo e as evidências correspondentes.

**Why P1**: O relatório é a entrega principal do produto.

**Acceptance Criteria**:

1. WHEN uma análise válida for persistida THEN the backend SHALL disponibilizar o relatório sem exigir concordância ou contestação do responsável.
2. WHILE o relatório estiver disponível the frontend SHALL exibir resumo executivo, resultado geral, motivo do resultado, contagem por gravidade, limitações, ambientes analisados e metadados da execução.
3. FOR EACH ambiente analisado the frontend SHALL exibir resultado do ambiente, motivo da aprovação, reprovação ou inconclusão, critérios avaliados, evidências, achados, impacto, gravidade, confiança, recomendação e manifestações do responsável em seção separada.
4. IF um ambiente estiver `INCONCLUSIVO` THEN the relatório SHALL identificar as evidências insuficientes e a orientação objetiva para nova captura.
5. IF não houver achados em evidências suficientes THEN the relatório SHALL declarar `Nenhum indício visual identificado nas evidências analisadas` e SHALL manter as limitações da inspeção visual.
6. WHEN o usuário imprimir ou salvar como PDF THEN the frontend SHALL preservar identificação, resultados, seções por ambiente, evidências, metadados e aviso de escopo.
7. The relatório SHALL manter todos os achados da IA, inclusive os contestados, e SHALL apresentar a contestação ao lado sem alterar a conclusão automatizada.

**Independent Test**: Gerar relatório com múltiplos ambientes, um achado contestado e uma evidência insuficiente; conferir tela e impressão.

### P1: Validação real e observabilidade segura ⭐ MVP

**Requirement ID:** IAR-06

**User Story**: Como equipe do produto, quero comprovar e diagnosticar a execução da IA sem expor imagens ou segredos.

**Why P1**: Sem evidência reproduzível, o time não sabe distinguir modelo, integração, mock e falha de infraestrutura.

**Acceptance Criteria**:

1. WHEN uma análise iniciar, concluir ou falhar THEN the backend SHALL registrar identificador da vistoria, identificador da análise quando disponível, provedor, modelo, duração e categoria do resultado.
2. The system SHALL NOT registrar conteúdo de imagem, chave, token, contexto integral do usuário ou payload integral do modelo.
3. WHEN o health check do provedor for executado THEN the resposta SHALL informar configuração válida e disponibilidade de autenticação sem realizar chamada cobrada nem expor configuração sensível.
4. IF o OCI responder 401, 403, 404, 429 ou 5xx THEN the backend SHALL classificar a falha, registrar somente dados operacionais seguros e SHALL NOT aceitar a execução como sucesso.
5. WHEN a suíte controlada executar THEN it SHALL incluir imagem sem achado visível, imagem com umidade/mofo, imagem com fissura e imagem insuficiente, com resultado esperado documentado por categoria.
6. The automated tests SHALL usar fake do cliente OCI e SHALL NOT chamar OCI ou enviar imagens para serviço externo.

**Independent Test**: Executar testes automatizados isolados e, quando houver acesso, um smoke test opt-in com imagem controlada no OCI, conferindo resultados, logs minimizados e ausência de fallback.

---

## Edge Cases

- IF a permissão, cota ou modelo OCI mudar depois que o backend iniciar THEN uma nova submissão SHALL representar a falha real e SHALL NOT reutilizar resultado anterior como sucesso.
- IF a mesma análise for entregue duas vezes THEN the backend SHALL reconhecer o identificador já persistido e SHALL NOT duplicar achados ou manifestações.
- IF duas imagens do mesmo ambiente tiverem conclusões diferentes THEN o resultado do ambiente SHALL usar a condição mais restritiva e SHALL preservar ambas as evidências.
- IF um achado possuir confiança baixa THEN o relatório SHALL sinalizar baixa confiança e SHALL manter a gravidade separada da confiança.
- IF a manifestação do usuário alegar correção do problema THEN o relatório SHALL registrar a alegação sem transformar automaticamente o resultado original em aprovado.
- IF a versão do contrato da IA não for suportada THEN the backend SHALL rejeitar o documento e registrar `FALHA_IA`.

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Enums fechados, referências de imagem existentes, até 100 achados, textos limitados, justificativa de 1–1000 caracteres e contrato versionado. |
| Failure / partial-failure states | Indisponibilidade, timeout, loading e JSON inválido terminam em `FALHA_IA`; evidências e manifestações anteriores permanecem. |
| Idempotency / retry / duplicate handling | Identificador de análise impede duplicação; nova tentativa parte de `FALHA_IA`; nenhum fallback automático muda de provedor. |
| Auth boundaries & rate limits | Ownership permanece obrigatório. Rate limiting é N/A no runtime local; a borda de deploy deverá tratá-lo em feature própria. |
| Concurrency / ordering | `@Version` continua protegendo a vistoria; uma análise só persiste se o estado e a execução esperada ainda coincidirem. |
| Data lifecycle / expiry | N/A porque a feature não exclui nem expira evidências ou análises. |
| Observability | Logs estruturados e metadados mínimos, sem imagens, prompts integrais, segredos ou payload completo. |
| External-dependency failure | Erros OCI 401/403/404/429/5xx têm categoria explícita e recuperável quando aplicável, sem mock, relatório parcial ou status de sucesso. |
| State-transition integrity | `AGUARDANDO_IA → RELATORIO_DISPONIVEL` somente após contrato válido; falhas vão para `FALHA_IA`; manifestações não mudam o resultado. |

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| IAR-01 | Provedor real sem sucesso simulado | Validate | Verified locally |
| IAR-02 | Contrato por ambiente | Validate | Verified locally |
| IAR-03 | Resultado automatizado | Validate | Verified locally |
| IAR-04 | Manifestação separada | Validate | Verified locally |
| IAR-05 | Relatório completo | Validate | Verified locally |
| IAR-06 | Validação e observabilidade | Validate | Verified locally; OCI smoke blocked externally |

**Coverage:** 6 total, 6 mapped to tasks, 6 verified locally.

## Success Criteria

- [ ] Uma imagem de mofo não recebe o mesmo resultado de uma imagem sem indício no smoke test controlado do OCI. **Bloqueado externamente até o smoke autorizado.**
- [x] Nenhuma indisponibilidade do OCI produz achado `mock`, relatório ou aprovação.
- [x] Todo achado exibido identifica ambiente e evidência de origem.
- [x] O relatório fica disponível pela conclusão da IA, independentemente de manifestação do cliente.
- [x] Uma contestação aparece no relatório sem modificar a conclusão original.
- [x] Suítes backend e frontend, build, lint e migration PostgreSQL têm evidência atual; a validação OCI real registra PASS ou bloqueio externo comprovado, nunca sucesso presumido.
