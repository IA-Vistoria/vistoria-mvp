# QA de design — Vistor.IA

**Status:** PASS
**Data:** 2026-09-19
**Referências:**

- `.specs/features/frontend-redesign/assets/cliente-vistoria-guiada.png`
- `.specs/features/frontend-redesign/assets/engenheiro-revisao-tecnica.png`

## Escopo e estados avaliados

- Cliente em rascunho, com o item `Sala — Paredes e revestimentos` selecionado e duas evidências persistidas pela API real.
- Engenheiro com três casos na fila, duas evidências autenticadas, pré-laudo preliminar e decisão ainda bloqueada por parecer vazio.
- Viewports efetivas de 1488 × 1056, 768 × 1024 e 375 × 812, sem overflow horizontal da página.

## Coerência do sistema

- As duas áreas compartilham marca, shell, tipografia, tokens de concreto, azul de projeto, terracota, verde estrutural e âmbar, além dos mesmos estados de foco, borda e ação.
- A área do cliente preserva hierarquia guiada: etapa, imóvel, progresso, item em foco, evidências e orientação de envio.
- A área da engenharia preserva hierarquia operacional: fila, caso, galeria, pré-laudo e decisão profissional no mesmo workspace.
- A arquitetura do frontend permanece organizada por feature (`auth` e `inspections`), com contratos HTTP em `lib`, componentes compartilhados mínimos e rotas finas no App Router.
- Não há `MedFlow`, gradientes decorativos, severidade, confiança ou CREA inventados pelo frontend.

## Achados e disposições

| Severidade | Área | Evidência | Correção / disposição | Status |
| --- | --- | --- | --- | --- |
| Média | Responsividade | Em 375 px, o stepper exigia rolagem horizontal e o item em foco aparecia depois de toda a navegação do protocolo. | Stepper reorganizado em 2 × 2 e conteúdo em foco antecipado no fluxo móvel por ordem de grid. | Corrigido |
| Média | Acessibilidade | Duas fotos do mesmo item recebiam o mesmo nome acessível no seletor do engenheiro. | Nome do botão passou a incluir o índice da evidência e ganhou teste de componente. | Corrigido |
| Média | Estado do fluxo | O stepper mantinha “Evidências” ativa mesmo depois de a vistoria ser enviada. | As etapas agora derivam do status real, com estado ativo e concluído também expostos semanticamente. | Corrigido |
| Baixa | Fidelidade de conteúdo | A referência do engenheiro exibe gravidade, confiança e identificação profissional não fornecidas pelo contrato atual. | Elementos omitidos para não fabricar dados técnicos; a responsabilidade profissional continua explícita. | Aceito por contrato |
| Baixa | Densidade | A referência contém mais itens de fila e mais dados do edifício do que a massa local. | O layout adapta a densidade aos dados reais da API e mantém alinhamento, truncamento e hierarquia. | Aceito por dados |

## Comparação visual

- Cliente: `.specs/features/frontend-redesign/evidence/qa-cliente-lado-a-lado.png`
- Engenharia: `.specs/features/frontend-redesign/evidence/qa-engenheiro-lado-a-lado.png`
- Responsivo: `.specs/features/frontend-redesign/evidence/03-cliente-protocolo-375.png` e `.specs/features/frontend-redesign/evidence/05-engenheiro-revisao-768.png`

## Resultado

Nenhum bloqueador visual, estrutural, responsivo ou de acessibilidade foi encontrado após as correções. As diferenças remanescentes são deliberadas e preservam o contrato real, a segurança do domínio e a responsabilidade do engenheiro.
