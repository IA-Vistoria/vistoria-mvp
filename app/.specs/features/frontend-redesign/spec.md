# Experiência Vistor.IA Specification

## Problem Statement

O frontend atual compila, mas não oferece um fluxo completo e confiável para cliente e engenheiro: o perfil é inferido pelo e-mail, os contratos de vistoria divergem do backend, o cliente não consegue executar o protocolo de evidências e a revisão técnica contém ações sem efeito. A experiência deve ser redesenhada como um produto único, chamado **Vistor.IA**, preservando a separação Human-in-the-Loop entre coleta do cliente, pré-laudo da IA e decisão do engenheiro.

## Goals

- [ ] Permitir que cliente e engenheiro se cadastrem, autentiquem e sejam direcionados pelo perfil retornado pela API, sem heurística baseada no e-mail.
- [ ] Permitir que o cliente crie uma vistoria, registre evidências por item de protocolo, revise e submeta sem perder o estado já salvo.
- [ ] Permitir que o engenheiro encontre vistorias pendentes, examine evidências e pré-laudo e aprove ou devolva a vistoria com parecer obrigatório.
- [ ] Aplicar um sistema visual responsivo e acessível compartilhado pelas duas áreas, com marca Vistor.IA e linguagem ligada à construção civil e ao setor imobiliário.
- [ ] Manter frontend e backend alinhados por contratos tipados, erros visíveis e testes automatizados derivados desta especificação.

## Approved Visual References

- Cliente: [`assets/cliente-vistoria-guiada.png`](assets/cliente-vistoria-guiada.png)
- Engenheiro: [`assets/engenheiro-revisao-tecnica.png`](assets/engenheiro-revisao-tecnica.png)

As referências definem identidade, hierarquia e linguagem visual. Os dados exibidos na implementação permanecem limitados ao contrato real da API e não reproduzem conteúdo técnico fictício do mockup.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
| --- | --- |
| Integração real com OCI/Gemini | O backend atual usa um provedor mock; esta entrega apresenta corretamente o resultado disponível sem ativar chamadas externas com custo ou credenciais. |
| Assinatura digital qualificada e geração de PDF | A aprovação registra o engenheiro e seu parecer, mas certificado ICP-Brasil e documento final são features próprias. |
| Pagamentos, contratação e escolha de engenheiro | O MVP usa a fila existente de vistorias pendentes. |
| Chat em tempo real | O retorno ao cliente permanece assíncrono por status e parecer. |
| Recuperação de senha, refresh token e login social | Não fazem parte do contrato de autenticação atual. |
| Editor administrativo de protocolo | O protocolo desta entrega é fixo e versionado no código. |
| Substituição do JWT em `localStorage` por cookie HttpOnly | Exige mudança de estratégia de autenticação no backend; será tratada como evolução de segurança separada. |
| Severidade ou confiança inventadas pelo frontend | Esses valores só serão exibidos se vierem do contrato da API; o frontend não deduz nem fabrica informação técnica. |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here - nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Marca do produto | Usar `Vistor.IA` em metadata, autenticação e áreas logadas; remover `MedFlow` da interface e das chaves novas de armazenamento. | Nome aprovado visualmente e diretamente associado a vistoria e IA. | y |
| Sistema visual | Concreto quente, calcário, grafite, azul de projeto, terracota, verde estrutural e âmbar de segurança; sem gradientes ou roxo tecnológico. | Direção visual aprovada pelo usuário e coerente com construção civil. | y |
| Relação entre as áreas | Compartilhar tokens, tipografia, componentes básicos, estados e shell; cliente recebe fluxo guiado e engenheiro recebe workspace mais denso. | Mantém uma identidade única sem sacrificar a tarefa principal de cada papel. | y |
| Protocolo inicial | Usar cinco grupos fixos: Sala, Cozinha, Banheiro, Quarto e Instalações, com doze itens versionados no frontend e validados no backend. | Corresponde ao fluxo aprovado e evita um editor administrativo prematuro. | y |
| Pré-laudo estruturado | Transformar o texto do pré-laudo em resumo e achados quando houver itens reconhecíveis; gravidade e confiança ausentes serão omitidas ou marcadas como não informadas. | Preserva o bloco visual aprovado sem apresentar inferências como dados técnicos. | n |
| Contrato de evidências | Acrescentar metadados das imagens e uma rota autenticada de leitura, sem expor o caminho físico do servidor. | O engenheiro não consegue revisar fotos usando o contrato atual. | n |
| Persistência da sessão | Manter JWT no navegador para esta entrega, com chave `vistoria.session`, limpeza em logout/401 e sem registrar o token em logs. | É compatível com o backend stateless atual e limita a mudança ao escopo aprovado. | n |
| Estratégia de testes do frontend | Adicionar Vitest, React Testing Library e `user-event` como dependências de desenvolvimento, após a confirmação operacional exigida pelo projeto. | O projeto não possui runner capaz de provar componentes e interações do React. | n |
| Compatibilidade de rotas | Preservar `/login`, `/client` e `/engineer`; adicionar `/register` e rotas filhas de vistoria. | Evita quebra gratuita das URLs já existentes. | n |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Identidade, cadastro e acesso por perfil ⭐ MVP

**User Story**: Como cliente ou engenheiro, quero criar minha conta e entrar na área correspondente ao meu perfil para acessar somente as operações permitidas.

**Why P1**: Todos os demais fluxos dependem de uma identidade real e da separação segura de papéis.

**Acceptance Criteria** (each line is one EARS pattern):

1. The frontend SHALL exibir a marca `Vistor.IA` e não SHALL exibir o nome `MedFlow` nas páginas, metadata ou textos de ajuda.
2. WHEN a API autenticar um usuário THEN the frontend SHALL persistir os dados mínimos da sessão e direcionar `ROLE_CLIENTE` para `/client` e `ROLE_ENGENHEIRO` para `/engineer` usando o campo `perfil` retornado.
3. WHEN um visitante enviar cadastro válido THEN the frontend SHALL enviar nome, e-mail, senha, perfil e CREA condicional para `/api/auth/register` e iniciar a sessão com a resposta recebida.
4. WHILE o perfil selecionado for `ROLE_ENGENHEIRO` the frontend SHALL exigir o CREA; WHILE o perfil for `ROLE_CLIENTE` the frontend SHALL ocultar e excluir o CREA do payload.
5. IF login ou cadastro retornar um `ProblemDetail` THEN the frontend SHALL manter os dados não sensíveis preenchidos e exibir `detail` como mensagem de erro associada ao formulário.
6. IF uma rota protegida for aberta sem sessão válida THEN the frontend SHALL redirecionar para `/login` sem renderizar conteúdo do papel protegido.
7. IF uma resposta autenticada retornar HTTP 401 THEN the frontend SHALL limpar a sessão e direcionar para `/login` com uma mensagem de sessão expirada.
8. IF o perfil autenticado tentar abrir a área do outro papel THEN the frontend SHALL direcionar para sua própria área sem enviar a requisição protegida incompatível.

**Independent Test**: Cadastrar e autenticar um cliente e um engenheiro, verificar o redirecionamento por `perfil`, bloquear a área oposta e validar os erros 401/409/422 na interface.

---

### P1: Jornada guiada de auto-vistoria do cliente ⭐ MVP

**User Story**: Como cliente, quero registrar o imóvel e suas evidências passo a passo para enviar uma vistoria completa e acompanhar seu processamento.

**Why P1**: É o fluxo de entrada que sustenta todo o valor do produto.

**Acceptance Criteria**:

1. WHEN o cliente abrir `/client` THEN the frontend SHALL listar as vistorias retornadas por `/api/vistorias/minhas` em ordem decrescente de `dataCriacao`, com endereço, estado e próxima ação compatível.
2. WHEN o cliente iniciar uma nova vistoria THEN the frontend SHALL criar somente o rascunho com endereço válido e SHALL aguardar a inclusão de evidências antes de submetê-lo.
3. WHILE a vistoria estiver `EM_RASCUNHO` ou `DEVOLVIDA_CLIENTE` the frontend SHALL apresentar os doze itens do protocolo agrupados em Sala, Cozinha, Banheiro, Quarto e Instalações, com progresso calculado pelas evidências confirmadas pela API.
4. WHEN o cliente selecionar um item do protocolo THEN the frontend SHALL permitir enviar imagens JPEG, PNG ou WebP de até 10 MB associadas ao identificador exato do item.
5. WHEN um upload for confirmado pela API THEN the frontend SHALL atualizar miniaturas e progresso sem descartar uploads anteriores.
6. IF um arquivo estiver vazio, exceder 10 MB ou tiver tipo não permitido THEN the system SHALL rejeitar o upload, preservar as evidências válidas e exibir a causa ao lado do item.
7. IF o upload falhar por rede ou servidor THEN the frontend SHALL manter o item incompleto, preservar o restante do rascunho e oferecer nova tentativa sem criar outra vistoria.
8. IF o cliente tentar finalizar sem ao menos uma evidência confirmada THEN the system SHALL impedir a transição e informar que uma evidência é obrigatória.
9. WHEN o cliente confirmar a revisão final THEN the frontend SHALL chamar `/api/vistorias/{id}/submeter` uma única vez, desabilitar ações duplicadas e renderizar o status retornado.
10. WHILE a vistoria estiver `AGUARDANDO_IA`, `AGUARDANDO_ENGENHEIRO` ou `CONCLUIDA` the frontend SHALL apresentar o acompanhamento em modo somente leitura.
11. WHILE a vistoria estiver `FALHA_IA` the frontend SHALL apresentar a falha real e uma ação de reenvio compatível com a transição aceita pelo backend.
12. WHILE a vistoria estiver `DEVOLVIDA_CLIENTE` the frontend SHALL exibir o parecer do engenheiro e permitir complementar evidências antes de novo envio.

**Independent Test**: Criar um rascunho, anexar uma imagem válida a um item, rejeitar um arquivo inválido, revisar, submeter e conferir a mudança de estado sem criação duplicada.

---

### P1: Fila e revisão técnica do engenheiro ⭐ MVP

**User Story**: Como engenheiro civil, quero revisar evidências e o pré-laudo da IA antes de registrar meu parecer e decidir pela conclusão ou devolução da vistoria.

**Why P1**: Materializa a responsabilidade profissional e impede que a IA seja tratada como decisão final.

**Acceptance Criteria**:

1. WHEN o engenheiro abrir `/engineer` THEN the frontend SHALL listar somente vistorias retornadas por `/api/vistorias/pendentes`, ordenadas de forma determinística por data de criação e identificador.
2. WHEN o engenheiro abrir uma vistoria pendente THEN the frontend SHALL exibir protocolo, imóvel, cliente, status, datas, evidências autenticadas e pré-laudo retornados pela API.
3. WHEN o pré-laudo contiver achados separados THEN the frontend SHALL renderizar cada achado em linha própria dentro de `Pré-laudo da IA` e SHALL identificar o conjunto como `Análise preliminar`.
4. IF gravidade ou confiança não estiverem presentes no contrato THEN the frontend SHALL omitir esses valores ou exibir `Não informado` e SHALL NOT fabricar classificações ou percentuais.
5. The engineer workspace SHALL exibir as mensagens `A IA sugere. O engenheiro decide.` e `Este conteúdo não substitui a avaliação técnica profissional.` próximas ao pré-laudo e à decisão.
6. WHILE o parecer técnico estiver vazio THEN the frontend SHALL manter desabilitadas as ações de aprovação e devolução.
7. WHEN o engenheiro aprovar com parecer válido THEN the frontend SHALL enviar `{ aprovado: true, parecer }`, bloquear repetição durante a requisição e apresentar o status `CONCLUIDA` retornado.
8. WHEN o engenheiro devolver com parecer válido THEN the frontend SHALL enviar `{ aprovado: false, parecer }`, bloquear repetição durante a requisição e remover o caso da fila após a resposta `DEVOLVIDA_CLIENTE`.
9. IF a vistoria deixar de estar pendente antes da decisão THEN the frontend SHALL atualizar a fila, fechar o estado editável e informar que o caso já foi processado.
10. IF uma evidência não puder ser carregada THEN the frontend SHALL identificar a foto indisponível sem bloquear a leitura do restante do caso.

**Independent Test**: Abrir um caso pendente com evidências e pré-laudo, comprovar a ausência de dados técnicos inventados e executar separadamente aprovação e devolução com parecer obrigatório.

---

### P1: Contrato autenticado de evidências ⭐ MVP

**User Story**: Como usuário autorizado, quero acessar somente as evidências das vistorias que posso consultar para revisar o caso sem exposição do armazenamento físico.

**Why P1**: O frontend do engenheiro não funciona com segurança enquanto a API não fornecer metadados e conteúdo autorizado das imagens.

**Acceptance Criteria**:

1. WHEN a API serializar uma vistoria THEN the backend SHALL incluir para cada evidência apenas `id`, `protocoloItem`, `dataUpload` e uma URL autenticada da API, sem caminho físico do servidor.
2. WHEN o cliente proprietário solicitar uma evidência de sua vistoria THEN the backend SHALL retornar o conteúdo com tipo de mídia compatível.
3. WHEN um engenheiro autenticado solicitar uma evidência de uma vistoria `AGUARDANDO_ENGENHEIRO` THEN the backend SHALL retornar o conteúdo com tipo de mídia compatível.
4. IF outro cliente solicitar a evidência THEN the backend SHALL responder 403 em `application/problem+json`.
5. IF a evidência não existir ou o arquivo estiver ausente THEN the backend SHALL responder 404 em `application/problem+json` sem revelar caminhos internos.
6. IF o protocolo recebido não pertencer à lista fixa ou o arquivo não for JPEG, PNG ou WebP de até 10 MB THEN the backend SHALL rejeitar o upload com resposta HTTP 400 ou 422 padronizada.

**Independent Test**: Fazer upload como proprietário, ler a mesma imagem como proprietário e como engenheiro de caso pendente e provar que outro cliente recebe 403 sem exposição de caminho.

---

### P1: Sistema visual responsivo e acessível ⭐ MVP

**User Story**: Como usuário, quero uma interface legível e coerente em diferentes tamanhos de tela para concluir minhas tarefas sem depender apenas de cor, mouse ou conhecimento técnico.

**Why P1**: O redesenho só cumpre seu objetivo se o fluxo permanecer compreensível e operável.

**Acceptance Criteria**:

1. The frontend SHALL aplicar os mesmos tokens de cor, tipografia, espaçamento, borda, foco e estados nas áreas de cliente e engenheiro.
2. The frontend SHALL usar concreto quente, calcário, grafite, azul de projeto, terracota, verde estrutural e âmbar de segurança sem gradientes decorativos.
3. The frontend SHALL oferecer nomes acessíveis para controles com ícone, associação entre labels e campos, foco visível e regiões de erro anunciáveis.
4. The frontend SHALL comunicar carregamento, vazio, sucesso, alerta e erro com texto ou ícone além da cor.
5. WHEN a viewport tiver 375 px, 768 px ou 1440 px de largura THEN the frontend SHALL preservar o fluxo principal sem rolagem horizontal na página.
6. WHEN um diálogo for aberto THEN the frontend SHALL mover o foco para o diálogo, manter navegação por teclado dentro dele e devolver o foco ao acionador ao fechar.
7. IF o usuário preferir movimento reduzido THEN the frontend SHALL remover animações não essenciais.
8. The frontend SHALL exibir na navegação somente destinos implementados e SHALL NOT renderizar links ou botões sem comportamento funcional.

**Independent Test**: Executar os fluxos principais apenas por teclado, validar nomes acessíveis e inspecionar as três larguras sem overflow horizontal.

---

### P1: Cliente HTTP, estados e falhas observáveis ⭐ MVP

**User Story**: Como usuário, quero feedback confiável das operações para saber quando uma ação foi concluída, falhou ou pode ser tentada novamente.

**Why P1**: A implementação atual registra falhas apenas no console e pode enviar headers incompatíveis com upload.

**Acceptance Criteria**:

1. The frontend SHALL usar um cliente HTTP único que envie Bearer quando exigido e SHALL NOT definir `Content-Type: application/json` para corpos `FormData`.
2. WHEN a resposta tiver corpo RFC 9457 THEN the frontend SHALL preservar `status`, `title`, `detail`, `instance` e erros de campo em um erro tipado.
3. IF a resposta não tiver JSON válido THEN the frontend SHALL produzir uma mensagem segura baseada no status HTTP sem expor stack trace.
4. WHILE uma mutação estiver em andamento the frontend SHALL desabilitar somente as ações conflitantes e indicar o estado ocupado no controle acionado.
5. IF uma leitura inicial falhar THEN the frontend SHALL exibir um estado de erro com ação de tentar novamente no conteúdo, sem depender do console.
6. WHEN uma mutação for concluída THEN the frontend SHALL reconciliar a tela com a resposta do servidor ou com uma nova leitura antes de liberar a próxima transição.
7. The frontend SHALL NOT registrar JWT, senha, conteúdo integral de evidência ou parecer técnico em logs do navegador.
8. WHEN frontend e backend forem executados localmente THEN the frontend SHALL consumir a API Spring Boot real configurada por `NEXT_PUBLIC_API_URL`, sem fallback para dados mockados no navegador.
9. IF a política CORS impedir uma chamada válida da origem configurada THEN the integrated system SHALL falhar no gate de integração e SHALL NOT ser declarado concluído.

**Independent Test**: Simular sucesso, ProblemDetail, resposta sem JSON, 401 e falha de rede para JSON e multipart, verificando feedback, retry e headers.

---

## Edge Cases

- IF o usuário clicar repetidamente em criar, submeter, aprovar ou devolver THEN the frontend SHALL enviar no máximo uma requisição concorrente daquela ação.
- IF duas sessões processarem a mesma vistoria THEN the frontend SHALL aceitar o estado mais recente do servidor e bloquear transição baseada em estado obsoleto.
- IF o cliente atualizar a página durante um rascunho THEN the frontend SHALL reconstruir o progresso a partir das evidências persistidas pela API.
- IF o texto de pré-laudo vier vazio THEN the frontend SHALL informar `Pré-laudo ainda não disponível` sem criar achados fictícios.
- IF a sessão expirar durante upload ou decisão THEN the frontend SHALL encerrar a sessão e SHALL NOT repetir automaticamente a mutação após novo login.
- IF a lista estiver vazia THEN the frontend SHALL explicar o estado e apresentar somente uma próxima ação compatível com o papel.

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --- | --- |
| Input validation & bounds | Endereço, CREA, parecer, protocolo, tipo e limite de 10 MB estão cobertos pelos critérios. |
| Failure / partial-failure states | Upload parcial, leitura, mutação, imagem ausente, IA e sessão expirada têm estados explícitos. |
| Idempotency / retry / duplicate handling | A UI bloqueia mutações concorrentes; retries só ocorrem por ação explícita e usam o mesmo recurso quando aplicável. Não será criado mecanismo geral de idempotency-key nesta entrega porque os endpoints atuais protegem transições por estado. |
| Auth boundaries & rate limits | Papéis, ownership, 401 e 403 estão cobertos. Rate limiting é N/A porque pertence à infraestrutura/backend transversal, não ao redesenho. |
| Concurrency / ordering | Estado retornado pelo servidor prevalece e casos processados deixam de ser editáveis. |
| Data lifecycle / expiry | Sessão é removida em logout/401; evidências seguem o ciclo da vistoria. Exclusão e retenção de evidências são N/A porque não há operação de exclusão nesta feature. |
| Observability | Falhas são visíveis na UI e dados sensíveis não são logados. Telemetria externa é N/A porque não há provedor configurado. |
| External-dependency failure | `FALHA_IA`, falha de rede e imagem indisponível têm apresentação e retry delimitados. |
| State-transition integrity | Ações são derivadas de `VistoriaStatus`, e a resposta mais recente do backend prevalece. |

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FUX-01 | P1: Identidade, cadastro e acesso por perfil | Specify | Pending |
| FUX-02 | P1: Jornada guiada de auto-vistoria do cliente | Specify | Pending |
| FUX-03 | P1: Fila e revisão técnica do engenheiro | Specify | Pending |
| FUX-04 | P1: Contrato autenticado de evidências | Specify | Pending |
| FUX-05 | P1: Sistema visual responsivo e acessível | Specify | Pending |
| FUX-06 | P1: Cliente HTTP, estados e falhas observáveis | Specify | Pending |

**ID format:** `[CATEGORY]-[NUMBER]`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 6 total, 0 mapped to tasks, 6 unmapped until the Tasks phase.

## Success Criteria

- [ ] Cliente consegue cadastrar-se, criar rascunho, enviar evidência válida, revisar e submeter usando a interface em uma execução integrada local.
- [ ] Engenheiro consegue autenticar-se, abrir evidências e pré-laudo, registrar parecer e aprovar ou devolver em uma execução integrada local.
- [ ] Nenhuma área protegida é renderizada para sessão ausente ou papel incompatível.
- [ ] Lint, testes automatizados do frontend, build Next.js e suíte Maven passam na execução final.
- [ ] Um teste de contexto Spring real sobe a aplicação com Flyway e segurança configurados; método de teste vazio não conta como evidência.
- [ ] Os contratos usados pelo frontend têm testes backend para autenticação, listagem, criação, upload, leitura de evidência, submissão, aprovação e devolução.
- [ ] Uma execução integrada com frontend e backend ativos comprova requisições HTTP e CORS reais, sem interceptação ou dados mockados no navegador.
- [ ] Login, jornada do cliente e revisão do engenheiro são verificadas visualmente em 375 px, 768 px e 1440 px sem overflow horizontal.
- [ ] A interface final não contém `MedFlow`, ações sem efeito, dados técnicos inventados, logs sensíveis ou caminhos físicos de upload.
