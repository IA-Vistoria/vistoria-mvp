# Vistor.IA — handoff do design IA-first

**Status:** direção visual e protótipo local concluídos; Figma parcialmente bloqueado por cota
**Arquivo Figma:** [Vistor.IA · MVP IA-first](https://www.figma.com/design/QG8jmgybfdkdQJakAfigSX)
**File key:** `QG8jmgybfdkdQJakAfigSX`
**Protótipo local editável:** [`docs/design/prototype/index.html`](prototype/index.html)
**Especificação aprovada:**
[`docs/superpowers/specs/2026-09-30-interface-ia-first-design.md`](../superpowers/specs/2026-09-30-interface-ia-first-design.md)

## Objetivo do arquivo

Materializar um sistema visual profissional, moderno e agradável para a jornada
IA-first do Vistor.IA. A experiência termina no **Relatório de vistoria por
IA**, revisado pelo próprio usuário, e não inclui engenheiro, parecer técnico ou
homologação profissional no fluxo principal.

## Limitação operacional do Figma

O arquivo foi criado e recebeu cinco coleções com 78 variáveis vinculáveis:
`Primitives` (17), `Color` (19), `Spacing` (13), `Radius` (7) e
`Typography` (22). Depois disso, o plugin atingiu a cota de chamadas do plano
Starter e bloqueou novas operações.

O design não foi declarado concluído no Figma. Para preservar o trabalho e não
paralisar a entrega, as telas foram materializadas em um protótipo HTML/CSS/JS
local, sem dependências de runtime, usando exatamente a mesma direção visual.
Ele é a fonte executável temporária para as especificações TLC e poderá ser
transferido para os tokens já criados quando a cota estiver disponível.

O plano Starter também restringiu o arquivo a três páginas. Na retomada, os
oito módulos originais serão seções dentro destas páginas:

1. `00 · Sistema visual`
2. `01 · Produto e componentes`
3. `02 · Estados, protótipo e handoff`

## Estrutura lógica planejada

1. `00 · Capa e princípios`
2. `01 · Fundações`
3. `02 · Componentes`
4. `03 · Fluxo mobile`
5. `04 · Tablet e desktop`
6. `05 · Estados e acessibilidade`
7. `06 · Protótipo navegável`
8. `07 · Handoff`

## Inventário v1 bloqueado

### Fundações

- primitivas e cores semânticas da direção **Clareza Técnica**;
- escala espacial em múltiplos de 4, com ritmo principal de 8 px;
- raios de 6, 10 e 16 px, além do raio completo para estados compactos;
- Instrument Sans para interface e relatório;
- IBM Plex Mono para IDs, data, hora e metadados;
- sombras discretas e movimento entre 160 e 240 ms.

### Componentes

- ações: Button, Icon button e Link;
- entrada: Field, Select, Quantity stepper, Checkbox, Radio e Segmented control;
- navegação: App header, Bottom navigation, Side navigation e Sticky action bar;
- progresso: Step progress e Room progress;
- conteúdo: Property card, Inspection row e Report row;
- evidência: Camera viewport, Evidence thumbnail e Upload item;
- IA: AI status, Finding marker, Finding review e Limitation block;
- relatório: cabeçalho, resumo, seção de ambiente, achado e bloco de limitações;
- feedback: status, empty state, skeleton e erro contextual.

### Telas

- acesso e cadastro;
- início e retomada;
- cadastro do imóvel e definição de ambientes;
- roteiro;
- captura e upload;
- análise;
- revisão, correção e rejeição de achado;
- prévia, geração e visualização do relatório;
- histórico de relatórios;
- perfil essencial;
- estados operacionais e adaptações para 390, 768 e 1440 px.

## Descoberta

### Arquivo novo

O arquivo foi confirmado como Figma Design. A leitura inicial encontrou apenas
`Page 1`, vazia, sem coleções, estilos ou componentes locais. Instrument Sans e
IBM Plex Mono estão disponíveis nos pesos necessários.

### Código e ativos

- nenhum arquivo Code Connect foi encontrado;
- o frontend usa Lucide, que será a fonte dos SVGs editáveis;
- `prototipo/screenshots/onboarding-captura-checklist.png` é referência do fluxo,
  não será incorporada como interface achatada;
- o CSS atual e sua identidade mineral/terrosa não são fontes do novo sistema.

### Bibliotecas acessíveis

O arquivo possui Material 3 Design Kit e Simple Design System, além das
bibliotecas Apple. A pesquisa encontrou botões, campos, indicadores, cartões,
variáveis e um estilo de heading, mas os modelos de tokens, variantes e
identidade não correspondem ao produto definido.

| Decisão | Ativos | Justificativa |
| --- | --- | --- |
| Reutilizar | Instrument Sans, IBM Plex Mono e SVGs Lucide do código | correspondem diretamente à identidade e à stack |
| Envolver | nenhum componente externo no v1 | wrappers manteriam dependência visual e API incompatíveis sem benefício suficiente |
| Criar | tokens, estilos, componentes e telas Vistor.IA | garante autoria, bindings coerentes e cobertura dos estados IA-first |

## Critérios de aceite

- jornada principal completa sem contato com engenheiro;
- toda conclusão rastreável a uma evidência;
- achados confirmáveis, corrigíveis, rejeitáveis e refazíveis;
- limitações da IA presentes nos pontos de decisão;
- uso exclusivo do nome Relatório de vistoria por IA;
- nenhuma alegação técnica ou jurídica não comprovada;
- componentes e tokens locais, vinculados e reutilizáveis;
- telas críticas resolvidas em 390, 768 e 1440 px;
- estados vazios, carregamentos, falhas e retomadas desenhados;
- contraste, foco, toque e redução de movimento verificáveis.

## Registro de execução

| Etapa | Estado | Evidência |
| --- | --- | --- |
| Arquivo e inventário | Concluído | Design `QG8jmgybfdkdQJakAfigSX`; sem páginas ou frames duplicados |
| Fundações | Parcial no Figma | 5 coleções e 78 variáveis; estilos e documentação aguardam cota |
| Componentes | Concluído no protótipo local | primitivas, navegação, câmera, achado, progresso e relatório em HTML/CSS |
| Fluxo mobile | Concluído no protótipo local | cadastro, captura e análise em 390 px |
| Revisão e relatório | Concluído no protótipo local | vínculo `IMG-014 → achado 02 → contexto → relatório` |
| Responsividade e estados | Parcial | composições em 390, 768 e desktop; matriz completa segue para a implementação |
| Protótipo e handoff | Concluído localmente | seis telas navegáveis, sem etapa de engenheiro |

## Validação local

- revisão visual em navegador nas composições desktop e mobile;
- ausência de overflow horizontal nas telas críticas verificadas;
- imagens carregadas e alvos de toque da revisão mobile com pelo menos 44 px;
- captura com obturador de 70 px e alternativa equivalente pela galeria;
- busca textual do relatório sem engenheiro, homologação ou parecer;
- bloco de limitações presente e nome oficial preservado;
- correção aplicada ao contraste do botão de retorno no modo câmera.

O relatório da revisão está em
[`docs/design/prototype/audit.md`](prototype/audit.md).
