# Auditoria do protótipo IA-first

**Data:** 30 de setembro de 2026

**Artefato:** [`index.html`](index.html)

**Direção:** Clareza Técnica · conceito Evidência Viva

## Escopo materializado

O protótipo contém seis momentos navegáveis e adaptações explícitas para
celular, tablet e desktop:

1. início orientado à próxima ação;
2. cadastro progressivo do imóvel e dos ambientes;
3. captura guiada com câmera e galeria equivalentes;
4. análise assíncrona com progresso por evidência;
5. revisão de um achado por vez;
6. Relatório de vistoria por IA com rastreabilidade e limitações.

As imagens de evidência foram geradas especificamente para o projeto com a
ferramenta nativa de geração de imagens e mantidas em `assets/`. Os prompts
pediram fotografias residenciais documentais, sem pessoas, textos, marcas,
setas ou danos dramatizados: uma visão geral de sala e um detalhe de fissura
fina com umidade inicial próxima ao rodapé.

## Auditoria visual

### Veredito

Atende a barra para orientar a implementação do MVP. A identidade não depende
de um painel genérico: o rastro visual entre foto, marcador, contexto e trecho
do documento é próprio do Vistor.IA. A captura assume linguagem escura de campo;
as demais áreas usam o canvas claro e o cobalto de forma restrita.

### Evidências

- hierarquia forte sem grade de métricas de vaidade;
- Instrument Sans em toda a experiência e IBM Plex Mono apenas em metadados;
- fotografia dominante durante captura e revisão;
- movimentos limitados a 160–300 ms e removidos com
  `prefers-reduced-motion`;
- layout móvel sem overflow horizontal nas telas críticas verificadas;
- alvos móveis da revisão com pelo menos 44 px;
- falha de contraste observada no retorno da câmera foi corrigida com superfície
  escura translúcida e ícone branco.

## Auditoria de fluxo cognitivo

### Decisões removidas ou adiadas

- o usuário não escolhe um “tipo de laudo” antes de entender o produto;
- ambientes comuns já vêm selecionados e podem ser ajustados;
- a análise continua em segundo plano, sem exigir espera na tela;
- um achado é revisado por vez, sem expor uma fila técnica;
- download e compartilhamento só aparecem depois da revisão.

### Informação para decidir

- a captura explica o enquadramento esperado e a qualidade atual;
- o achado separa observação visual do que a imagem não permite concluir;
- a pergunta de contexto fica ao lado da foto de origem;
- a etapa mostra confirmar, corrigir e rejeitar antes de gerar o relatório;
- o documento repete evidência, contexto e momento da confirmação.

### Recuperação e reversibilidade

- galeria é alternativa à câmera;
- o cadastro pode ser salvo como rascunho;
- análise informa que é seguro sair e retornar;
- correção e rejeição não apagam a foto original;
- a implementação deverá ampliar a matriz com câmera negada, offline, upload
  interrompido, imagem inconclusiva e falha na geração.

## Gate para implementação

O protótipo é uma referência visual executável, não código de produção. A
implementação deverá preservar:

- o nome **Relatório de vistoria por IA**;
- a ausência de engenheiro no fluxo principal;
- a ligação estável entre evidência, achado, contexto e relatório;
- estados assíncronos verdadeiros;
- limites explícitos da análise visual;
- acessibilidade por teclado, foco visível, contraste e toque.
