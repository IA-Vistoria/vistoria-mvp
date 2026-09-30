# MVP Relatório de vistoria por IA Context

**Gathered:** 30 de setembro de 2026
**Spec:** `.specs/features/mvp-relatorio-ia/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Entregar a jornada principal IA-first do Vistor.IA, do cadastro do imóvel ao
Relatório de vistoria por IA, com captura guiada, análise assíncrona, revisão
do contexto pelo proprietário e documento rastreável. A entrega reorganiza os
contratos necessários, mas não remove destrutivamente o legado de engenharia e
não cria laudo técnico, assinatura ou diagnóstico profissional.

---

## Implementation Decisions

### Papel do usuário e linguagem

- O usuário do MVP é o responsável pelo imóvel; a interface pública não pede
  escolha entre cliente e engenheiro.
- A revisão humana confirma contexto, corrige ou rejeita observações da IA; não
  é homologação técnica.
- O documento se chama sempre `Relatório de vistoria por IA`.
- Palavras que impliquem laudo, parecer, aprovação profissional, NBR, garantia
  ou diagnóstico são excluídas da experiência principal.

### Fluxo e hierarquia

- Início destaca uma única próxima ação; relatórios recentes vêm depois.
- Captura prioriza câmera, mas a galeria é alternativa equivalente.
- Análise é assíncrona e não exige permanência na tela.
- Revisão mostra um achado por vez com a foto dominante.
- O rastro `foto → achado → contexto → relatório` aparece na revisão e no
  documento.

### Persistência e compatibilidade

- A análise original permanece imutável em `preLaudoIa`.
- Revisões são persistidas como JSON tipado em coluna nova, com upsert por
  `imagemId + indiceAchado` e validação contra a análise original.
- Novos estados são `REVISAO_PENDENTE` e `RELATORIO_DISPONIVEL`.
- Estados, endpoints e colunas de engenharia permanecem no backend como legado
  compatível, mas não determinam a nova jornada.
- Vistorias legadas `CONCLUIDA` continuam disponíveis para leitura.

### Operação assíncrona

- Submissão persiste `AGUARDANDO_IA`, responde 202 e publica um evento apenas
  depois do commit.
- Um listener assíncrono chama a porta de IA fora da transação de banco.
- Retry em `AGUARDANDO_IA` não agenda trabalho duplicado; `FALHA_IA` permite
  nova submissão explícita.

### Documento e compartilhamento

- O relatório é renderizado no frontend a partir de análise e revisões já
  persistidas.
- `window.print()` fornece impressão/salvamento em PDF sem dependência nova.
- Web Share é usado quando disponível; a área de transferência é fallback.
- Achados rejeitados não entram nas conclusões, mas as fotos originais
  permanecem no conjunto de evidências.

### Agent's Discretion

- Composição interna dos componentes React e nomes de helpers.
- Microcopy secundária, desde que preserve limites e vocabulário aprovado.
- Transições de 160–240 ms e detalhes de responsividade que não alterem o fluxo.
- Organização do JSON persistido, desde que o contrato HTTP permaneça tipado e
  não exponha detalhes de armazenamento.

### Declined / Undiscussed Gray Areas → Assumptions

- Não houve nova rodada de perguntas porque o usuário aprovou a direção visual,
  corrigiu explicitamente o papel do engenheiro e pediu execução automática.
- Defaults técnicos não decididos pelo usuário estão registrados na tabela de
  suposições da especificação, com racional e marcação `n`.

---

## Specific References

- `docs/superpowers/specs/2026-09-30-interface-ia-first-design.md`
- `docs/design/prototype/index.html`
- Arquivo Figma parcial: `https://www.figma.com/design/QG8jmgybfdkdQJakAfigSX`
- Referência enviada pelo usuário: artefato Claude mais recente e PDF de fluxo,
  usados como insumo, não como fonte técnica de verdade.

---

## Deferred Ideas

- remoção física do legado de engenharia;
- PDF gerado e assinado no servidor;
- fila offline durável;
- roteiro configurável;
- integração real com câmera nativa;
- provedores pagos de IA e storage.
