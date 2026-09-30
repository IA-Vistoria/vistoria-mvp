# Vistor.IA Figma Design Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `figma:figma-use`,
> `figma:figma-create-new-file`, `figma:figma-generate-library` and
> `figma:figma-generate-design` to execute this plan task-by-task. Do not begin
> frontend or backend implementation from this plan.

**Goal:** Criar no Figma o sistema visual e o protótipo navegável IA-first do
Vistor.IA, prontos para validação e para orientar as especificações TLC de
frontend e backend.

**Architecture:** O arquivo será construído em camadas: fundações vinculadas a
variáveis, componentes reutilizáveis, telas compostas por instâncias e um
protótipo validado nos três breakpoints. A jornada terá como eixo o rastro
`foto → achado da IA → contexto confirmado → trecho do relatório` e terminará
no Relatório de vistoria por IA, sem fila ou homologação de engenheiro.

**Tech Stack:** Figma Design, Figma Variables, Auto Layout, componentes e
variantes, prototipação interativa, Lucide, Instrument Sans e IBM Plex Mono.

**Spec:**
`docs/superpowers/specs/2026-09-30-interface-ia-first-design.md`

## Global Constraints

- Nome do produto final: **Relatório de vistoria por IA**.
- A jornada principal não contém engenheiro, parecer profissional, homologação
  ou assinatura técnica.
- Paleta-base: fundo `#F6F8FC`, superfície `#FFFFFF`, texto `#172033`, texto
  secundário `#596579`, primária `#2F5BEA`, primária forte `#1D3FBB`, evidência
  confirmada `#08705F`, atenção `#B86700`, erro `#B83B4B` e borda `#D8DFEA`.
- Instrument Sans é a tipografia principal; IBM Plex Mono fica restrita a IDs,
  datas, horários e metadados técnicos.
- Cor não é o único indicador de estado; texto normal precisa alcançar WCAG AA
  de pelo menos 4,5:1.
- O layout deve usar Auto Layout, variáveis com escopos explícitos, aliases
  semânticos e componentes reutilizáveis, nunca uma interface achatada em uma
  imagem.
- Frames críticos devem ser validados em 390, 768 e 1440 px.
- Cada achado deve apontar para sua evidência e permitir confirmar, corrigir,
  rejeitar ou refazer antes da geração do relatório.
- Falhas assíncronas preservam trabalho válido e apresentam recuperação no
  mesmo contexto da ação.
- Nenhum texto pode prometer diagnóstico, precisão não documentada, validade
  jurídica, conformidade normativa ou aprovação profissional.
- A execução mantém o ledger em
  `/tmp/design-system-state-vistoria-ia-first.json` e registra o resultado
  durável em `docs/design/figma-handoff.md`.

## Review Focus

- **Câmera negada ou indisponível:** a captura oferece galeria e orientação para
  liberar permissão sem bloquear a vistoria inteira; coberto na Tarefa 6.
- **Conexão perdida durante upload:** fotos locais e progresso permanecem
  visíveis, com retomada explícita; coberto na Tarefa 6.
- **IA não consegue analisar uma foto:** a interface identifica a evidência,
  explica o limite e permite refazer ou excluir; coberto nas Tarefas 5 e 6.
- **Usuário discorda do achado:** correção ou rejeição altera a prévia do
  relatório sem apagar a foto original; coberto na Tarefa 5.
- **Conteúdo longo e zoom:** textos, endereços, rótulos e relatório não podem
  cortar ou sobrepor conteúdo nos três breakpoints; coberto nas Tarefas 6 e 7.

---

### Task 1: Criar o arquivo e fixar o inventário

**Files:**

- Create: arquivo Figma Design `Vistor.IA · MVP IA-first`
- Create: `/tmp/design-system-state-vistoria-ia-first.json`
- Create: `docs/design/figma-handoff.md`
- Modify: `docs/superpowers/specs/2026-09-30-interface-ia-first-design.md`

**Interfaces:**

- Consumes: especificação aprovada e inventário técnico do repositório.
- Produces: `fileKey`, URL, IDs iniciais, inventário bloqueado de páginas,
  tokens, componentes e telas.

- [ ] **Step 1: Registrar a aprovação da especificação**

Confirmar que o campo `Status` da especificação contém a data da aprovação e
que o repositório continua na branch de trabalho, sem mudanças não relacionadas.

- [ ] **Step 2: Verificar a conta e criar o arquivo**

Consultar os planos disponíveis do Figma. Se houver um único plano, criar o
arquivo Design nele; se houver mais de um, interromper somente para o usuário
selecionar o destino. Salvar `fileKey` e URL no ledger.

- [ ] **Step 3: Executar a descoberta obrigatória**

Inspecionar o arquivo vazio, consultar bibliotecas disponíveis antes de buscar
ativos e pesquisar separadamente ícones, componentes, variáveis e estilos. No
repositório, procurar somente Code Connect e ativos associados ao inventário.

Expected: relatório de lacunas distingue `reutilizar`, `envolver` e `criar`, sem
assumir que biblioteca vazia significa ausência de ativos pesquisáveis.

- [ ] **Step 4: Fixar o escopo no handoff**

Criar `docs/design/figma-handoff.md` com URL, `fileKey`, páginas planejadas,
inventário v1, decisões de reutilização e critérios de aceite.

- [ ] **Step 5: Validar a inicialização**

Read-back obrigatório: o arquivo é do tipo Design, o ledger contém o `fileKey`,
o handoff aponta para a especificação aprovada e não existem páginas ou frames
duplicados.

- [ ] **Step 6: Commit**

```bash
git add docs/design/figma-handoff.md docs/superpowers/specs/2026-09-30-interface-ia-first-design.md
git commit -m "docs(figma): registra arquivo e inventário do design"
```

### Task 2: Construir fundações e estrutura do arquivo

**Files:**

- Create: páginas Figma `00 · Capa e princípios`, `01 · Fundações`,
  `02 · Componentes`, `03 · Fluxo mobile`, `04 · Tablet e desktop`,
  `05 · Estados e acessibilidade`, `06 · Protótipo navegável` e `07 · Handoff`
- Modify: `/tmp/design-system-state-vistoria-ia-first.json`
- Modify: `docs/design/figma-handoff.md`

**Interfaces:**

- Consumes: `fileKey`, inventário e gap analysis da Tarefa 1.
- Produces: variáveis primitivas e semânticas, estilos de texto e efeito,
  escalas de espaço/raio e oito páginas determinísticas.

- [ ] **Step 1: Demonstrar a ausência das fundações**

Executar uma leitura estrutural e confirmar que as coleções e páginas esperadas
ainda não existem. A checagem deve falhar para pelo menos uma fundação antes da
construção.

- [ ] **Step 2: Criar as coleções de variáveis**

Criar `Primitives`, `Color`, `Spacing`, `Radius` e `Typography`. Valores
semânticos de cor devem ser aliases; todas as variáveis terão escopo explícito e
sintaxe web no formato `var(--token-name)`.

- [ ] **Step 3: Criar estilos e páginas**

Verificar os nomes reais das fontes disponíveis antes de carregá-las. Criar os
estilos tipográficos e de efeito e, depois, as oito páginas na ordem definida.

- [ ] **Step 4: Montar a documentação de fundações**

Exibir paleta, tipografia, espaçamento, raios, elevação, iconografia, movimento
e exemplos corretos/incorretos. Remover placeholders ao concluir.

- [ ] **Step 5: Validar fundações**

Read-back obrigatório: nomes, contagens, aliases, escopos, sintaxe web, fontes
e ordem das páginas. Conferir os pares de contraste da especificação e fazer uma
captura visual única da página `01 · Fundações`, corrigindo apenas defeitos
observados.

- [ ] **Step 6: Atualizar handoff e commit**

```bash
git add docs/design/figma-handoff.md
git commit -m "docs(figma): registra fundações do sistema visual"
```

### Task 3: Construir a biblioteca de componentes

**Files:**

- Modify: página Figma `02 · Componentes`
- Modify: `/tmp/design-system-state-vistoria-ia-first.json`
- Modify: `docs/design/figma-handoff.md`

**Interfaces:**

- Consumes: tokens e estilos validados da Tarefa 2.
- Produces: componentes e variantes consumidos pelas telas das Tarefas 4–6.

- [ ] **Step 1: Definir o gate estrutural antes da criação**

Listar as famílias necessárias e confirmar que pelo menos `Button`, `Field`,
`Evidence thumbnail` e `Finding review` ainda não têm componentes locais
compatíveis.

- [ ] **Step 2: Criar primitivas interativas**

Construir `Button`, `Icon button`, `Link`, `Field`, `Select`, `Quantity stepper`,
`Checkbox`, `Radio`, `Segmented control` e `Status badge`, com estados default,
hover, focus, pressed, disabled, loading e error quando aplicáveis.

- [ ] **Step 3: Criar navegação e progresso**

Construir `App header`, `Bottom navigation`, `Side navigation`, `Step progress`,
`Room progress` e `Sticky action bar` usando instâncias de primitivas.

- [ ] **Step 4: Criar componentes do domínio**

Construir `Property card`, `Inspection row`, `Report row`, `Camera viewport`,
`Evidence thumbnail`, `Upload item`, `AI status`, `Finding marker`,
`Finding review`, `Limitation block` e blocos do relatório.

- [ ] **Step 5: Validar a biblioteca**

Read-back obrigatório: conjuntos, propriedades, variantes, bindings, estilos e
descrições. Nenhuma matriz pode exceder 30 combinações; ícones usam
`INSTANCE_SWAP` ou SVG editável. Revisar uma única vitrine de instâncias em
escala normal, sem cortes, baixa legibilidade ou componentes destacados.

- [ ] **Step 6: Atualizar handoff e commit**

```bash
git add docs/design/figma-handoff.md
git commit -m "docs(figma): registra biblioteca de componentes"
```

### Task 4: Desenhar a coleta mobile-first

**Files:**

- Modify: página Figma `03 · Fluxo mobile`
- Modify: `/tmp/design-system-state-vistoria-ia-first.json`
- Modify: `docs/design/figma-handoff.md`

**Interfaces:**

- Consumes: componentes da Tarefa 3.
- Produces: frames mobile de acesso até captura, todos com largura de 390 px.

- [ ] **Step 1: Criar wrappers vazios e verificar o fluxo**

Criar wrappers auto-layout determinísticos para `Acesso`, `Início`, `Imóvel`,
`Ambientes`, `Roteiro` e `Captura`. Antes de preencher, confirmar que a ordem
representa uma jornada única e não contém papel de engenheiro.

- [ ] **Step 2: Compor acesso, início e cadastro**

Usar instâncias e conteúdo realista em português. A ação principal deve ser
inequívoca e labels devem permanecer visíveis após o preenchimento.

- [ ] **Step 3: Compor roteiro e captura guiada**

A câmera domina a captura; instrução, ambiente, progresso, feedback de qualidade
e fila de upload permanecem legíveis com uma mão. A galeria é uma alternativa
equivalente, não uma saída escondida.

- [ ] **Step 4: Validar composição mobile**

Read-back obrigatório: frames em 390 px, instâncias ligadas, texto editável,
alvos de toque mínimos de 44 px e sticky actions fora da safe area. Fazer uma
captura visual do fluxo completo e corrigir apenas problemas observados.

- [ ] **Step 5: Atualizar handoff e commit**

```bash
git add docs/design/figma-handoff.md
git commit -m "docs(figma): registra fluxo mobile de coleta"
```

### Task 5: Desenhar análise, revisão e relatório

**Files:**

- Modify: página Figma `03 · Fluxo mobile`
- Modify: página Figma `06 · Protótipo navegável`
- Modify: `/tmp/design-system-state-vistoria-ia-first.json`
- Modify: `docs/design/figma-handoff.md`

**Interfaces:**

- Consumes: evidências capturadas na Tarefa 4.
- Produces: frames de análise, revisão, correção, prévia, geração, histórico e
  visualização do relatório.

- [ ] **Step 1: Fixar o cenário de teste**

Usar uma vistoria fictícia coerente com pelo menos três fotos, dois achados e um
caso inconclusivo. Cada achado precisa compartilhar o mesmo ID de evidência em
todas as telas e no relatório.

- [ ] **Step 2: Compor análise assíncrona**

Mostrar progresso por foto e ambiente, permissão para sair e retornar, análise
parcial explícita e recuperação localizada de falha.

- [ ] **Step 3: Compor revisão corrigível**

Exibir foto dominante, marcador numerado, observação da IA, pergunta de contexto
e ações para confirmar, corrigir, rejeitar ou refazer. Desenhar os resultados
distintos dessas quatro ações.

- [ ] **Step 4: Compor prévia, relatório e histórico**

O documento liga evidência, contexto confirmado e trecho resultante. Incluir
limitações, download e compartilhamento sem selo, assinatura, NBR ou linguagem
de laudo técnico.

- [ ] **Step 5: Validar rastreabilidade e linguagem**

Read-back obrigatório: IDs de evidência iguais entre captura, revisão e
relatório; busca textual sem vocabulário proibido; fontes corretas; conteúdo
editável. Fazer uma captura visual do conjunto e uma pós-correção apenas se
necessária.

- [ ] **Step 6: Atualizar handoff e commit**

```bash
git add docs/design/figma-handoff.md
git commit -m "docs(figma): registra revisão e relatório IA-first"
```

### Task 6: Resolver responsividade, estados e acessibilidade

**Files:**

- Modify: página Figma `04 · Tablet e desktop`
- Modify: página Figma `05 · Estados e acessibilidade`
- Modify: `/tmp/design-system-state-vistoria-ia-first.json`
- Modify: `docs/design/figma-handoff.md`

**Interfaces:**

- Consumes: fluxo mobile completo das Tarefas 4 e 5.
- Produces: layouts de 768 e 1440 px, matriz de estados e anotações de
  acessibilidade.

- [ ] **Step 1: Criar cenários responsivos antes da composição**

Duplicar por instância ou recompor os três momentos críticos: captura, revisão
e relatório. Definir comportamento esperado de cada região antes de redimensionar.

- [ ] **Step 2: Compor tablet e desktop**

Em 768 px, evidência e contexto dividem o espaço. Em 1440 px, roteiro,
evidência e contexto/relatório podem coexistir sem virar painel de engenharia.

- [ ] **Step 3: Construir a matriz operacional**

Cobrir lista vazia, câmera negada, galeria, arquivo inválido, foto ruim, fila de
upload, offline, retomada, análise parcial, falha de IA, achado sem foto,
geração e compartilhamento indisponíveis.

- [ ] **Step 4: Documentar acessibilidade**

Anotar ordem de foco, nomes acessíveis, foco visível, anúncios assíncronos,
alternativas a gestos, redução de movimento e uso combinado de cor, texto e
ícone. Testar explicitamente labels e endereços longos e zoom a 200%.

- [ ] **Step 5: Validar os três breakpoints**

Read-back obrigatório: larguras 390, 768 e 1440; ausência de texto cortado e
sobreposição; pares de contraste válidos; família tipográfica correta; ações
principais alcançáveis. Fazer uma captura por composição coerente e somente uma
pós-correção quando houver defeito visual.

- [ ] **Step 6: Atualizar handoff e commit**

```bash
git add docs/design/figma-handoff.md
git commit -m "docs(figma): registra responsividade e estados"
```

### Task 7: Prototipar, validar e entregar o design

**Files:**

- Modify: página Figma `06 · Protótipo navegável`
- Modify: página Figma `07 · Handoff`
- Modify: `/tmp/design-system-state-vistoria-ia-first.json`
- Modify: `docs/design/figma-handoff.md`

**Interfaces:**

- Consumes: sistema, telas e estados validados nas Tarefas 2–6.
- Produces: protótipo navegável aprovado e contrato visual para as especificações
  TLC de implementação.

- [ ] **Step 1: Ligar o happy path**

Prototipar entrada, cadastro, roteiro, captura, análise, revisão, correção,
prévia e relatório. As transições permanecem entre 160 e 240 ms e têm alternativa
sem movimento essencial.

- [ ] **Step 2: Ligar os caminhos de recuperação**

Prototipar câmera negada, upload interrompido, foto inconclusiva, rejeição do
achado e falha na geração sem apagar evidências válidas.

- [ ] **Step 3: Executar as quatro tarefas de aceitação**

1. cadastrar um imóvel e definir ambientes;
2. concluir a captura de um ambiente;
3. revisar e corrigir um achado da IA;
4. gerar e localizar o relatório.

Expected: todas terminam sem contato com engenheiro, sem nó morto e sem depender
de conhecimento técnico do usuário.

- [ ] **Step 4: Executar a auditoria final**

Verificar inventário, aliases, bindings, componentes, fontes, contraste,
breakpoints, conteúdo editável, vocabulário e ausência de placeholders. Remover
somente temporários identificados por IDs exatos do ledger.

- [ ] **Step 5: Consolidar o handoff**

Registrar URL, versão, páginas, componentes, tokens, decisões, limitações,
matriz tela → contrato necessário e os pontos que exigirão mudança no backend.

- [ ] **Step 6: Commit**

```bash
git add docs/design/figma-handoff.md
git commit -m "docs(figma): conclui protótipo e handoff do MVP"
```

## Gate de conclusão

O plano termina somente quando os dez critérios de aceitação da especificação
estiverem evidenciados no Figma e no handoff. Depois da aprovação visual do
protótipo, o próximo trabalho deverá criar artefatos TLC separados — requisitos
EARS, design técnico e tarefas — para frontend e backend. Nenhuma alteração de
código de produção pertence a este plano.
