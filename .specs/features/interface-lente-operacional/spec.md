# Interface Lente Operacional Specification

## Problem Statement

O frontend funcional atual apresenta uma aparência genérica e um defeito de composição no cadastro da vistoria: o título invade a área do formulário em larguras de desktop. A interface também não traduz com força a proposta do MVP — capturar evidências, receber uma análise de IA revisável e gerar um relatório — nem mantém uma identidade coerente entre acesso, navegação, coleta, revisão e documento final.

## Goals

- [ ] Recriar a jornada do cliente com a direção visual Lente Operacional aprovada, sem alterar os contratos do backend.
- [ ] Eliminar sobreposição, corte e rolagem horizontal em 390, 768 e 1440 px.
- [ ] Tornar IA, evidência e relatório reconhecíveis na marca, na navegação e nos estados do fluxo.
- [ ] Preservar comportamento, recuperação de erro, acessibilidade e testes existentes.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Alterar API, banco ou estados de domínio | O redesign consome contratos já implementados. |
| Remover as rotas legadas de engenharia | Compatibilidade técnica não é o objetivo desta entrega. |
| Criar laudo técnico, assinatura ou conformidade normativa | Contraria os limites confirmados do MVP. |
| Adicionar biblioteca visual ou de ícones | A stack existente já contém os recursos necessários e dependências exigem decisão separada. |
| Deploy em produção | A entrega autorizada é local, em branch de trabalho. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Autoridade visual | Usar o conceito aprovado `exec-1be9f796-2054-4264-8c51-a7bfd4861639.png` | O usuário aprovou essa versão após refinar símbolo e cores. | y |
| Jornada principal | Priorizar cliente e relatório por IA; manter engenharia fora da navegação principal | O usuário corrigiu explicitamente o foco do MVP. | y |
| Fotografias antes da primeira captura | Usar imagens autorais como demonstração identificada, nunca como evidência do usuário | Mantém fidelidade visual sem fabricar dados. | y |
| Ícones | Reutilizar Lucide | Já está instalado, mantém consistência e evita nova dependência. | y |
| Tema | Dark-first na aplicação; relatório mantém superfície clara de documento | Preserva a direção escolhida e a legibilidade de impressão. | y |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Iniciar uma vistoria sem colisão visual ⭐ MVP

**User Story**: As a responsável pelo imóvel, I want identificar o imóvel em uma interface clara so that eu inicie a captura com confiança.

**Why P1**: É a entrada do fluxo principal e contém o defeito visual relatado pelo usuário.

**Acceptance Criteria**:

1. WHEN o usuário abre `/client/vistorias/nova` em 1440 px THEN system SHALL exibir fotografia contextual, progresso, título e formulário sem sobreposição ou texto cortado.
2. WHEN a largura varia entre 390, 768 e 1440 px THEN system SHALL reorganizar conteúdo sem rolagem horizontal e sem quebra artificial por `<br>`.
3. WHEN o endereço está vazio e o usuário envia o formulário THEN system SHALL anunciar o erro junto ao campo e preservar foco recuperável.
4. WHILE a criação do rascunho está em andamento system SHALL impedir submissões duplicadas e comunicar o estado ocupado.
5. The system SHALL indicar que a imagem contextual é ilustrativa e que nenhuma foto foi enviada à IA nessa etapa.

**Independent Test**: Abrir a rota nos três breakpoints, submeter vazio e criar um rascunho válido.

---

### P1: Reconhecer e navegar pelo produto IA-first ⭐ MVP

**User Story**: As a usuário do MVP, I want reconhecer rapidamente marca, próxima ação e estágio da vistoria so that eu não confunda o produto com um painel genérico ou uma fila profissional.

**Why P1**: A identidade e a hierarquia orientam toda a jornada.

**Acceptance Criteria**:

1. WHEN uma tela autenticada do cliente é exibida THEN system SHALL mostrar a marca Vistor.IA aprovada e navegação para Início, Nova vistoria e Relatórios.
2. WHEN a rota ativa muda THEN system SHALL identificar o destino atual por texto, ícone e estado visual, sem depender apenas de cor.
3. WHEN a tela é usada por teclado THEN system SHALL manter foco visível e não oculto por navegação fixa.
4. WHERE o usuário utiliza celular, the system SHALL oferecer navegação inferior com alvos de toque mínimos de 44 px e safe area.
5. The system SHALL manter engenharia, homologação e parecer profissional fora da jornada principal do cliente.

**Independent Test**: Navegar pelo shell do cliente em desktop e celular e verificar foco, estado ativo e rótulos.

---

### P1: Concluir coleta, revisão e relatório no mesmo sistema visual ⭐ MVP

**User Story**: As a usuário em vistoria, I want percorrer captura, análise, revisão e relatório com continuidade so that eu compreenda a origem e o estado de cada informação.

**Why P1**: É o valor central do produto.

**Acceptance Criteria**:

1. WHEN o usuário abre uma vistoria em rascunho THEN system SHALL apresentar roteiro, evidência ativa, progresso real e ações de câmera/galeria com hierarquia operacional.
2. WHILE a IA processa as fotos system SHALL comunicar que as evidências estão salvas, evitar porcentagem fabricada e permitir retorno ao início.
3. WHEN a revisão está pendente THEN system SHALL manter a foto dominante, a observação da IA e as decisões humanas relacionadas no mesmo contexto visual.
4. WHEN todas as revisões são persistidas THEN system SHALL oferecer a geração do Relatório de vistoria por IA.
5. WHEN o relatório está disponível THEN system SHALL apresentar documento legível, rastreável e imprimível sem perder a identidade do produto.
6. IF upload, análise, carregamento ou compartilhamento falha THEN system SHALL preservar o último estado confirmado e mostrar recuperação no contexto da ação.

**Independent Test**: Executar o happy path coberto pelos mocks existentes do rascunho até o relatório e inspecionar os estados de falha.

---

### P2: Usar uma interface acessível e resiliente

**User Story**: As a pessoa com diferentes capacidades e dispositivos, I want operar o fluxo com teclado, leitor de tela ou toque so that eu consiga concluir a vistoria sem barreiras evitáveis.

**Why P2**: A vistoria pode ocorrer em condições físicas e ambientais variadas.

**Acceptance Criteria**:

1. The system SHALL manter contraste WCAG 2.2 AA nos pares de texto, ação e estado utilizados.
2. WHEN `prefers-reduced-motion` está ativo THEN system SHALL remover movimento não essencial sem ocultar conteúdo.
3. WHEN um controle iconográfico não possui texto visível THEN system SHALL fornecer nome acessível.
4. WHEN um erro de formulário ocorre THEN system SHALL anunciá-lo por `role="alert"` e associá-lo ao campo relevante.

**Independent Test**: Verificar a semântica com Testing Library e os pares/estados durante a inspeção visual.

---

## Edge Cases

- IF um endereço ou rótulo é longo THEN system SHALL quebrá-lo dentro do próprio contêiner sem sobrepor controles.
- IF uma foto real não está disponível THEN system SHALL mostrar um estado explícito, não um placeholder que simule evidência.
- WHEN o zoom do navegador alcança 200% THEN system SHALL preservar leitura e acesso às ações principais.
- IF JavaScript demora a hidratar THEN system SHALL evitar deslocamento estrutural causado por fontes ou imagens sem dimensão reservada.
- WHEN o relatório é impresso THEN system SHALL ocultar controles e manter o documento em superfície clara.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| UI-01 | P1: Iniciar vistoria | Design | In Design |
| UI-02 | P1: Navegar no produto | Design | In Design |
| UI-03 | P1: Coleta e relatório | Design | In Design |
| UI-04 | P2: Acessibilidade | Design | In Design |

**Coverage:** 4 total, 0 mapped to tasks, 4 unmapped.

---

## Success Criteria

- [ ] Os testes de frontend passam sem remoção ou skip.
- [ ] Lint e build de produção passam.
- [ ] As capturas em 390, 768 e 1440 px não apresentam colisão, corte ou rolagem horizontal.
- [ ] A comparação visual da nova vistoria atende à composição aprovada e o `design-qa.md` termina com `final result: passed`.
- [ ] Detector visual, revisão independente e validação TLC não deixam achado material aberto.
