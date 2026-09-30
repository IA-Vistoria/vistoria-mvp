# Vistor.IA — especificação da interface IA-first

**Data:** 30 de setembro de 2026
**Status:** aprovado pelo usuário em 30 de setembro de 2026
**Repositório analisado:** `IA-Vistoria/vistoria-mvp` no commit `89e494f`

## 1. Objetivo

Redesenhar a experiência do Vistor.IA como um produto mobile-first no qual a
IA conduz a coleta de evidências, aponta indícios visuais e gera um
**Relatório de vistoria por IA** revisável pelo próprio usuário.

O resultado deve parecer um produto profissional e confiável, não um painel
genérico de IA nem uma digitalização do fluxo de um engenheiro. A interface
precisa funcionar bem durante uma vistoria real, com atenção dividida,
luminosidade variável e uso frequente da câmera.

## 2. Fontes e decisões de produto

Esta especificação considera:

- o protótipo navegável em `prototipo/` e o artefato mais recente do Claude,
  com 28 pranchetas;
- o PDF de 26 páginas usado como referência complementar;
- a implementação atual de `app/frontend`, tratada pelo próprio README como
  PoC técnica, não como identidade ou stack definitiva do produto;
- o código e os contratos atuais de `app/`, que ainda incluem cliente,
  engenheiro e homologação profissional;
- as decisões explícitas do usuário nesta conversa.

Quando essas fontes divergem, vale a seguinte decisão de produto:

> O fluxo principal do MVP termina na revisão dos achados pelo usuário e na
> geração do Relatório de vistoria por IA. Engenheiro, fila técnica, parecer,
> aprovação profissional e devolução ao cliente não fazem parte da interface
> principal deste MVP.

A PoC atual continua sendo evidência técnica, mas seus estados e papéis não
definem o novo produto. A implementação futura deverá reconciliar essa
diferença de maneira explícita; o design não esconderá contratos legados.

## 3. Público, contexto e resultado esperado

### Público principal

Pessoa que está recebendo, conferindo ou documentando um imóvel e não possui
conhecimento técnico para organizar uma vistoria sozinha.

### Contexto de uso

- celular na mão e deslocamento entre ambientes;
- conexão possivelmente instável;
- iluminação e ruído variáveis;
- pouco tempo para decidir o que fotografar;
- necessidade de retomar o trabalho sem perder evidências.

### Resultado esperado

Ao concluir o fluxo, o usuário deve ter:

1. evidências organizadas por ambiente e item;
2. achados da IA vinculados às fotos de origem;
3. contexto confirmado, corrigido ou rejeitado pelo usuário;
4. limitações da análise claramente visíveis;
5. um Relatório de vistoria por IA que possa baixar ou compartilhar.

## 4. Princípios da experiência

### 4.1 Evidência antes da conclusão

Nenhum achado aparece como verdade isolada. Toda conclusão visual aponta para
a foto que a originou, o ambiente, a data e a resposta fornecida pelo usuário.

### 4.2 IA explicável e corrigível

A IA sugere um indício, explica o que observou e oferece ações concretas:
confirmar o contexto, corrigir a classificação, indicar que não se aplica ou
refazer a foto.

### 4.3 Uma decisão importante por tela

Durante a captura e a revisão, a interface prioriza uma ação por vez. Resumos
e métricas não competem com a próxima tarefa.

### 4.4 Progresso real, sem sucesso fabricado

Upload, processamento, análise parcial e geração do relatório têm estados
independentes. Falhas preservam as fotos já enviadas e oferecem recuperação.

### 4.5 Limites visíveis

O produto descreve **indícios visuais**. Não promete diagnóstico, conformidade
normativa, validade jurídica, cobertura de garantia ou certificação técnica.

## 5. Direção autoral

### Conceito: Evidência Viva

O traço reconhecível do Vistor.IA será um rastro contínuo entre quatro objetos:

```text
foto → achado da IA → contexto confirmado → trecho do relatório
```

Esse rastro aparece na captura, na revisão e na prévia do relatório. Ele é o
elemento de identidade do produto e evita que a marca dependa apenas de cor,
gradiente ou decoração.

### Direção visual: Clareza Técnica

Uma linguagem contemporânea, precisa e acolhedora. A interface combina
superfícies claras, contraste forte, tipografia sem serifa e detalhes visuais
inspirados em enquadramento fotográfico e documentação de evidências.

O resultado deve transmitir organização e segurança sem parecer hospitalar,
jurídico, luxuoso ou excessivamente industrial.

### O que será substituído

A linguagem atual de `app/frontend/src/app/globals.css` — `concrete`,
`structural`, `clay`, Georgia, cartões editoriais e paleta terrosa — não será
preservada. Também não serão herdados o verde-petróleo dominante, o coral como
assinatura visual ou uma tipografia editorial restrita ao relatório.

## 6. Sistema visual

### 6.1 Cor

A paleta inicial para prototipação será:

| Papel | Token inicial | Uso |
| --- | --- | --- |
| Fundo | `#F6F8FC` | canvas claro e confortável |
| Superfície | `#FFFFFF` | conteúdo, formulários e documentos |
| Texto principal | `#172033` | títulos e leitura principal |
| Texto secundário | `#596579` | metadados e ajuda |
| Primária | `#2F5BEA` | navegação, foco e ação principal |
| Primária forte | `#1D3FBB` | pressed, seleção e contraste |
| Evidência confirmada | `#08705F` | contexto confirmado e conclusão positiva |
| Atenção | `#B86700` | item que exige conferência |
| Erro | `#B83B4B` | falha ou conteúdo inválido |
| Borda | `#D8DFEA` | separação estrutural |

Esses valores são pontos de partida. A biblioteca final só será consolidada
depois da verificação de contraste WCAG AA nos pares realmente utilizados.
Cor nunca será o único indicador de estado.

Os pares-base já verificados — texto principal e secundário sobre o fundo,
texto branco sobre primária, primária forte e evidência confirmada — superam
4,5:1. Variantes futuras ainda deverão passar pelo mesmo teste.

### 6.2 Tipografia

- **Instrument Sans:** interface, títulos, botões e o próprio relatório;
- **IBM Plex Mono:** identificadores de foto, data, hora e metadados técnicos.

O relatório se diferencia pela composição, escala e ritmo, não por uma fonte
serifada separada. Isso mantém a identidade moderna e coerente em todo o fluxo.

### 6.3 Forma e profundidade

- escala espacial baseada em múltiplos de 4 e ritmo principal de 8 px;
- cantos de 6, 10 e 16 px, conforme função e dimensão;
- pílulas reservadas para estados compactos, nunca para todos os controles;
- bordas finas e sombras discretas apenas quando comunicarem elevação;
- nenhuma dependência de glassmorphism, gradientes decorativos ou mosaicos de
  cartões sem hierarquia.

### 6.4 Iconografia e fotografia

- Lucide como base, com traço e tamanho consistentes;
- nunca usar emoji como ícone funcional;
- fotos reais permanecem em proporção original sempre que possível;
- marcações da IA usam pontos numerados e contornos precisos, sem simular
  certeza maior do que o modelo possui;
- imagens inadequadas exibem o motivo e a ação de recuperação.

### 6.5 Movimento

- transições entre 160 e 240 ms para continuidade espacial;
- progresso assíncrono estável, sem spinner piscando para ações instantâneas;
- animação da ligação entre evidência e achado somente quando ajudar a explicar
  a origem da informação;
- suporte obrigatório a `prefers-reduced-motion`.

## 7. Arquitetura da informação

### Navegação principal

No celular, a navegação inferior contém no máximo quatro destinos:

1. **Início**
2. **Vistorias**
3. **Relatórios**
4. **Perfil**

A ação contextual — fotografar, revisar ou gerar relatório — permanece fixa na
área inferior da tela, acima da safe area, sem competir com a navegação.

### Fluxo principal

```text
Entrar
  ↓
Início
  ↓
Cadastrar imóvel
  ↓
Definir ambientes
  ↓
Revisar roteiro
  ↓
Capturar evidências
  ↓
Análise da IA
  ↓
Revisar achados
  ↓
Prévia do relatório
  ↓
Relatório de vistoria por IA
```

## 8. Telas do MVP

### 8.1 Acesso

Login e cadastro simples, sem vender capacidades que ainda não existem. A tela
explica em uma frase que o produto organiza fotos e aponta indícios visuais.

### 8.2 Início

Destaca a próxima ação: iniciar uma vistoria ou continuar a vistoria ativa.
Relatórios recentes aparecem depois da tarefa principal, sem métricas de vaidade.

### 8.3 Cadastro do imóvel

Fluxo curto, dividido em identificação, endereço e ambientes. Planta ou
memorial descritivo pode ser anexado como recurso opcional, mas o MVP não
depende disso para continuar.

### 8.4 Roteiro da vistoria

Lista ordenada de ambientes, estimativa apresentada como referência e itens a
levar. Não usa planta baixa como navegação principal, pois o produto não pode
pressupor que ela exista ou esteja estruturada.

### 8.5 Captura guiada

Modo de campo com câmera dominante, instrução curta e progresso do ambiente.
Inclui:

- visão geral, detalhes e pontos críticos;
- retorno sobre enquadramento, iluminação e nitidez;
- opção equivalente de selecionar imagem da galeria;
- fila de upload e indicação de conexão;
- possibilidade de refazer sem perder o restante.

### 8.6 Análise

Mostra progresso por foto e ambiente. O usuário pode sair da tela e retornar.
Falhas identificam quais imagens precisam ser reenviadas; análise parcial não é
apresentada como conclusão.

### 8.7 Revisão dos achados

Apresenta um achado por vez, com a foto como elemento dominante:

- marca visual numerada;
- descrição observacional da IA;
- pergunta de contexto;
- respostas grandes e inequívocas;
- ações para corrigir, rejeitar ou refazer a evidência;
- acesso ao conjunto de fotos relacionadas.

### 8.8 Prévia do relatório

O documento é montado diante do usuário. Cada achado mostra a foto de origem,
o contexto confirmado e as limitações. A edição retorna diretamente ao achado
correspondente.

### 8.9 Relatório gerado

Exibe resumo, achados, evidências e limitações. Permite baixar e compartilhar,
mas não usa selos de aprovação, assinatura profissional ou alegações de prova
formal.

### 8.10 Relatórios

Histórico pesquisável por imóvel e data. Estados possíveis na interface:
`Em elaboração`, `Processando`, `Revisão pendente`, `Disponível` e
`Falha na geração`.

### 8.11 Perfil

Conta, privacidade, notificações e ajuda. Não apresenta garantia ou prazo legal
como se fossem calculados ou validados pelo produto.

## 9. Linguagem e contrato de confiança

### Vocabulário preferido

- Relatório de vistoria por IA
- achado
- indício visual
- contexto confirmado por você
- análise em andamento
- não foi possível avaliar esta imagem
- refazer foto

### Vocabulário proibido sem capacidade comprovada

- laudo técnico
- parecer profissional
- aprovado ou homologado
- assinado digitalmente
- prova formal
- em conformidade com NBR
- garantia assegurada
- diagnóstico confirmado
- porcentagem de precisão não documentada

## 10. Estados e recuperação

O sistema de componentes precisa cobrir:

- carregamento inicial;
- lista vazia e primeira vistoria;
- permissão de câmera negada;
- arquivo inválido ou grande demais;
- foto escura, tremida ou incompleta;
- upload em fila, pausado, concluído ou com falha;
- modo offline e retomada;
- análise parcial ou interrompida;
- achado sem foto vinculada;
- relatório em geração, pronto ou com falha;
- download e compartilhamento indisponíveis.

Falhas aparecem junto à ação que as resolve. Toast não substitui erro de campo,
resumo acessível ou estado persistente.

## 11. Responsividade

### Celular: 360–430 px

Fluxo linear, câmera e evidência em primeiro plano, controles fixos respeitando
safe area e alvos de toque confortáveis.

### Tablet: 768–1024 px

Evidência e contexto podem dividir a tela. A lista de ambientes vira uma coluna
de apoio sem reduzir a fotografia a uma miniatura.

### Desktop: 1280 px ou mais

Três regiões possíveis: roteiro, evidência e contexto/relatório. É uma expansão
do mesmo fluxo do usuário, não um painel de engenharia.

## 12. Acessibilidade

- contraste WCAG AA de pelo menos 4,5:1 para texto normal;
- alvos de toque de 44 px no mínimo no web mobile;
- foco visível e ordem de teclado coerente;
- labels persistentes, ajuda e erros ligados semanticamente ao campo;
- estado assíncrono anunciado com mensagens contextuais, não apenas números;
- status representado por ícone, texto e cor;
- zoom do navegador permitido;
- alternativas para câmera e gestos;
- imagens com descrição funcional e marcações explicadas em texto.

## 13. Sistema de componentes

A primeira biblioteca no Figma deverá conter:

- botões, links, ações iconográficas e barras fixas;
- campos, selects, stepper de quantidade e upload;
- cabeçalho, navegação inferior e navegação lateral responsiva;
- indicador de progresso por etapa e por ambiente;
- cartão de imóvel e linha de relatório;
- miniatura de evidência e visor de câmera;
- marcador de achado e painel de confirmação;
- estados de IA, upload e geração;
- bloco de limitações;
- estrutura do relatório;
- empty states, erros e skeletons.

Componentes repetidos serão variantes e instâncias, não desenhos duplicados.

## 14. Estrutura do arquivo Figma

O plano de execução do design detalhará a criação do arquivo. Depois que esse
plano também for revisado e aprovado, o Figma será organizado em:

1. `00 · Capa e princípios`
2. `01 · Fundações`
3. `02 · Componentes`
4. `03 · Fluxo mobile`
5. `04 · Tablet e desktop`
6. `05 · Estados e acessibilidade`
7. `06 · Protótipo navegável`
8. `07 · Handoff`

Os frames principais serão validados em 390, 768 e 1440 px.

## 15. Relação com o código atual

O frontend existente já prova autenticação, upload, listagem, estados
assíncronos e exibição do retorno da IA. Entretanto:

- `app/frontend/src/features/inspections/engineer/` está fora da nova jornada;
- `AGUARDANDO_ENGENHEIRO` e `DEVOLVIDA_CLIENTE` não pertencem ao novo modelo
  de navegação;
- `VistoriaResponseDto` e o domínio ainda carregam `engenheiroId` e
  `parecerEngenheiro`;
- o resultado atual é somente leitura e ainda não oferece confirmação ou
  correção dos achados pelo usuário;
- o código atual cobre paredes, enquanto o protótipo de produto cobre ambientes
  e diferentes itens;
- o README ainda descreve homologação profissional obrigatória.

Esses pontos exigem uma decisão de implementação e possíveis mudanças de
contrato. O Figma não deve mascará-los como se já estivessem disponíveis.

## 16. Fora do escopo desta etapa

- implementação do frontend;
- alteração de API, banco ou estados do domínio;
- remoção do código de engenharia;
- escolha definitiva da stack do produto;
- validade jurídica do relatório;
- integração real com câmera, compartilhamento ou geração de PDF;
- diagnóstico estrutural, elétrico ou hidráulico.

## 17. Critérios de aceitação do design

O design será considerado aprovado quando:

1. o fluxo principal puder ser percorrido sem contato com um engenheiro;
2. toda conclusão da IA puder ser rastreada até uma evidência;
3. o usuário puder corrigir ou rejeitar um achado antes do relatório;
4. limites da IA estiverem presentes nos pontos de decisão;
5. o relatório usar o nome acordado e não fizer promessas técnicas ou legais;
6. a identidade não reutilizar a linguagem mineral/terrosa atual;
7. componentes e tokens formarem um sistema consistente e escalável;
8. telas críticas estiverem resolvidas para celular, tablet e desktop;
9. estados vazios, carregamentos e falhas estiverem desenhados;
10. contraste, foco, toque e redução de movimento forem verificáveis.

## 18. Validação planejada

Antes do handoff, o protótipo será testado com quatro tarefas:

1. cadastrar um imóvel e definir ambientes;
2. concluir a captura de um ambiente;
3. revisar e corrigir um achado da IA;
4. gerar e localizar o relatório.

A validação visual terá uma rodada completa nos três breakpoints e uma única
rodada de correções. A criação do Figma só começará depois da aprovação desta
especificação e do plano de execução do design. Qualquer implementação no
frontend terá uma decisão e um plano próprios, posteriores à aprovação do
protótipo.
