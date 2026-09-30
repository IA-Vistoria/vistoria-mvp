# Interface e roteiro adaptativo Specification

## Problem Statement

O MVP atual combina uma interface genérica com um protocolo fixo de 12 itens técnicos. Esse protocolo não representa imóveis reais: ambientes variam entre casas, apartamentos e imóveis comerciais. O cliente precisa saber o que registrar, mas também precisa adaptar o roteiro sem perder estrutura, rastreabilidade ou qualidade mínima para a análise da IA.

## Goals

- [ ] Criar e persistir um roteiro próprio para cada vistoria, com 1 a 30 ambientes ordenados.
- [ ] Exigir uma visão geral por ambiente e permitir detalhes adicionais associados ao mesmo contexto.
- [ ] Redesenhar toda a jornada IA-first com a identidade Lente Operacional revisada.
- [ ] Eliminar protocolo fixo, sobreposição, corte e rolagem horizontal em 390, 768 e 1440 px.
- [ ] Preservar autenticação, ownership, estados da análise, revisão e relatório existentes.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Remover rotas legadas de engenharia | Compatibilidade histórica não faz parte da jornada principal. |
| Laudo técnico, assinatura ou conformidade normativa | O produto entrega relatório fotográfico por IA, não laudo profissional. |
| Alterar o contrato do provedor VLM externo | Não existe documentação confirmada para novos campos nesse serviço. |
| Excluir evidências ou ambientes que já possuam fotos | Preservação de dados exige um fluxo próprio de exclusão. |
| Deploy em produção | A entrega autorizada é local, em branch de trabalho. |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Quantidade de ambientes | Entre 1 e 30 por vistoria | Mantém flexibilidade com payload e UI limitados. | y |
| Estrutura mínima | Uma foto `VISAO_GERAL` por ambiente | Dá contexto mínimo verificável para a IA. | y |
| Fotos adicionais | Categoria `DETALHE`, sem limite funcional novo | Mantém a coleta flexível e reutiliza limites de upload existentes. | y |
| Tipo do imóvel | `CASA`, `APARTAMENTO`, `COMERCIAL` ou `OUTRO`; usado somente para sugestão | A sugestão acelera o fluxo sem impor ambientes. | y |
| Edição do roteiro | Permitida somente em rascunho; ambiente com evidência não pode ser removido | Evita perda silenciosa de dados. | y |
| Identidade | Grafite-petróleo, mineral, verdigris e âmbar funcional; logo sem moldura externa | Reflete o refinamento visual mais recente. | y |
| Legado | Evidências antigas continuam legíveis por `protocoloItem` | Evita apagar ou invalidar dados existentes. | y |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Configurar um roteiro compatível com o imóvel ⭐ MVP

**User Story**: As a responsável pelo imóvel, I want escolher os ambientes existentes so that eu receba um roteiro claro sem registrar cômodos inexistentes.

**Why P1**: O roteiro define a coleta e corrige o principal engessamento do produto.

**Acceptance Criteria**:

1. WHEN o usuário escolhe um tipo de imóvel THEN the system SHALL sugerir ambientes sem marcar a sugestão como obrigatória.
2. WHEN o usuário cria uma vistoria THEN the system SHALL persistir endereço, tipo do imóvel e de 1 a 30 ambientes na ordem enviada.
3. WHEN o usuário adiciona um ambiente personalizado THEN the system SHALL aceitar nome de 2 a 60 caracteres e um tipo conhecido ou `OUTRO`.
4. IF o roteiro contém nomes duplicados sem diferenciar maiúsculas de minúsculas THEN the system SHALL responder `422 Unprocessable Content` com erro associado a `ambientes`.
5. IF a criação contém zero ou mais de 30 ambientes THEN the system SHALL responder `422 Unprocessable Content` sem criar a vistoria.
6. The system SHALL tratar o roteiro persistido na vistoria como fonte de verdade do progresso.

**Independent Test**: Criar casa, apartamento e imóvel comercial com listas diferentes e consultar cada roteiro persistido.

---

### P1: Editar o roteiro sem perder evidências ⭐ MVP

**User Story**: As a responsável em campo, I want ajustar os ambientes durante a vistoria so that o roteiro continue representando o imóvel real.

**Why P1**: Ambientes podem ser descobertos ou nomeados de outra forma durante a coleta.

**Acceptance Criteria**:

1. WHILE a vistoria está `EM_RASCUNHO`, the system SHALL permitir adicionar, renomear e reordenar ambientes.
2. WHEN um ambiente sem evidências é omitido da atualização THEN the system SHALL removê-lo do roteiro.
3. IF um ambiente com evidências é omitido da atualização THEN the system SHALL responder `409 Conflict` e preservar o roteiro anterior.
4. IF duas atualizações concorrentes alteram a mesma vistoria THEN the system SHALL rejeitar a atualização obsoleta com `409 Conflict`.
5. WHILE a vistoria não está `EM_RASCUNHO`, the system SHALL rejeitar alterações do roteiro com `409 Conflict`.

**Independent Test**: Adicionar e remover ambiente vazio, tentar remover ambiente com foto e tentar alterar vistoria fora de rascunho.

---

### P1: Capturar evidências estruturadas por ambiente ⭐ MVP

**User Story**: As a responsável pelo imóvel, I want receber instruções objetivas para cada ambiente so that eu envie fotos úteis sem seguir uma lista técnica rígida.

**Why P1**: A análise da IA depende de contexto mínimo e associação correta.

**Acceptance Criteria**:

1. WHEN o usuário abre uma vistoria em rascunho THEN the system SHALL exibir somente os ambientes persistidos e o progresso real desse roteiro.
2. WHEN uma foto é enviada THEN the system SHALL associá-la a um `ambienteId` pertencente à vistoria e à categoria `VISAO_GERAL` ou `DETALHE`.
3. IF o `ambienteId` não pertence à vistoria THEN the system SHALL responder `422 Unprocessable Content` e não armazenar o arquivo.
4. IF a categoria não é suportada THEN the system SHALL responder `422 Unprocessable Content` e não armazenar o arquivo.
5. WHILE o upload está em andamento, the system SHALL impedir submissão duplicada no mesmo controle e comunicar o estado ocupado.
6. WHEN o usuário escolhe “Pular por agora” THEN the system SHALL navegar sem marcar o ambiente como concluído.
7. WHEN a tela não possui foto real THEN the system SHALL mostrar um estado explícito sem simular evidência do usuário.

**Independent Test**: Percorrer roteiros de tamanhos diferentes, enviar visão geral e detalhe e validar os erros de associação.

---

### P1: Enviar somente uma coleta minimamente completa ⭐ MVP

**User Story**: As a cliente, I want saber exatamente o que falta so that eu envie uma vistoria compreensível para a IA.

**Why P1**: Flexibilidade sem regra produziria relatórios inconsistentes.

**Acceptance Criteria**:

1. WHEN todos os ambientes possuem ao menos uma `VISAO_GERAL` THEN the system SHALL permitir o envio para análise.
2. IF um ou mais ambientes não possuem `VISAO_GERAL` THEN the system SHALL responder `422 Unprocessable Content` e informar os nomes ausentes.
3. WHEN a vistoria é enviada THEN the system SHALL preservar ambiente, categoria e ordem junto de cada evidência para revisão e relatório.
4. WHILE a IA processa as fotos, the system SHALL informar que as evidências estão salvas sem fabricar porcentagem de progresso.
5. IF a análise falha THEN the system SHALL preservar roteiro e evidências e oferecer nova tentativa.

**Independent Test**: Tentar enviar roteiro incompleto, completar as visões gerais e percorrer processamento, falha e retry.

---

### P1: Revisar e gerar relatório com contexto ⭐ MVP

**User Story**: As a responsável pelo imóvel, I want revisar observações da IA no contexto do ambiente so that o relatório preserve a origem de cada conclusão.

**Why P1**: Revisão e relatório são a entrega central do MVP.

**Acceptance Criteria**:

1. WHEN a revisão está pendente THEN the system SHALL mostrar foto, ambiente, observação da IA e decisão humana no mesmo contexto.
2. WHEN todas as observações são revisadas THEN the system SHALL permitir gerar o Relatório de vistoria por IA.
3. WHEN o relatório está disponível THEN the system SHALL agrupar evidências por ambiente e distinguir observação da IA de decisão do responsável.
4. The system SHALL declarar que o relatório fotográfico por IA não substitui laudo técnico profissional.
5. WHEN o relatório é impresso THEN the system SHALL ocultar controles e manter uma superfície clara legível.

**Independent Test**: Revisar achados de dois ambientes, concluir o relatório e imprimir a visualização.

---

### P2: Operar uma interface clara, responsiva e acessível

**User Story**: As a pessoa usando celular ou teclado, I want entender intenção, estado e próxima ação so that eu conclua a vistoria sem barreiras evitáveis.

**Why P2**: A coleta ocorre em campo e em dispositivos variados.

**Acceptance Criteria**:

1. WHEN uma tela autenticada é exibida THEN the system SHALL mostrar a marca sem moldura externa e navegação para Início, Nova vistoria e Relatórios.
2. WHEN a largura é 390, 768 ou 1440 px THEN the system SHALL reorganizar conteúdo sem rolagem horizontal, sobreposição ou texto cortado.
3. WHERE a navegação móvel é exibida, the system SHALL usar alvos de toque de pelo menos 44 px e respeitar safe area.
4. WHEN um erro de formulário ocorre THEN the system SHALL anunciá-lo por `role="alert"` e associá-lo ao campo relevante.
5. WHEN `prefers-reduced-motion` está ativo THEN the system SHALL remover movimento não essencial sem ocultar conteúdo.
6. The system SHALL manter contraste WCAG 2.2 AA nos pares de texto, ação e estado utilizados.

**Independent Test**: Navegar com teclado e verificar as telas nos três breakpoints.

---

## Edge Cases

- IF o endereço ou nome do ambiente é longo THEN the system SHALL quebrar o texto dentro do contêiner sem sobrepor controles.
- IF o upload falha após armazenamento e antes da persistência THEN the system SHALL remover o arquivo órfão e preservar o estado anterior.
- IF uma atualização de roteiro contém um identificador de outro cliente THEN the system SHALL responder `404 Not Found` ou `403 Forbidden` conforme o contrato existente, sem revelar dados.
- WHEN o zoom alcança 200% THEN the system SHALL preservar leitura e acesso às ações principais.
- IF uma evidência legada não possui ambiente estruturado THEN the system SHALL continuar exibindo seu `protocoloItem` sem bloquear relatórios existentes.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| ROTEIRO-01 | P1: Configurar roteiro | Implementation | In Progress |
| ROTEIRO-02 | P1: Editar roteiro | Implementation | Planned |
| CAPTURA-01 | P1: Captura estruturada | Implementation | In Progress |
| ENVIO-01 | P1: Coleta completa | Implementation | Planned |
| RELATORIO-01 | P1: Revisão e relatório | Implementation | Planned |
| UI-01 | P2: Interface acessível | Implementation | Planned |

**Coverage:** 6 total, 6 mapped to tasks, 0 unmapped.

---

## Success Criteria

- [ ] Casa, apartamento e imóvel comercial podem usar roteiros diferentes sem mudança de código.
- [ ] Nenhuma vistoria nova depende do total fixo de 12 itens.
- [ ] Backend e frontend rejeitam envio enquanto algum ambiente não possui visão geral.
- [ ] Testes de backend e frontend, lint e builds passam sem remoção ou skip.
- [ ] Capturas em 390, 768 e 1440 px não apresentam colisão, corte ou overflow horizontal.
- [ ] `app/design-qa.md` termina com `final result: passed`.
