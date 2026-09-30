# Estado das especificações

## Decisions

### AD-001 — Jornada principal IA-first

- **status**: active
- **decision**: A experiência principal termina na revisão humana e no Relatório de vistoria por IA; engenharia permanece apenas como compatibilidade legada fora da navegação do cliente.
- **rationale**: Esta é a função central do MVP confirmada pelo usuário e já sustentada pelos contratos atuais de análise, revisão e relatório.

### AD-002 — Identidade Lente Operacional

- **status**: superseded by AD-003
- **decision**: O frontend do cliente usa superfícies azul-marinho profundas, fotografia dominante, branco mineral, turquesa na marca e amarelo-lima apenas como sinal funcional de progresso.
- **rationale**: O usuário selecionou e refinou visualmente essa direção antes da implementação.

### AD-003 — Paleta Lente Operacional revisada

- **status**: active
- **decision**: O frontend usa grafite-petróleo, branco mineral, verdigris para marca e ação e âmbar apenas para progresso ou atenção. Azul elétrico, amarelo fluorescente e moldura externa do logo não são usados.
- **rationale**: O usuário pediu uma identidade mais coesa e aprovou visualmente a revisão da paleta.

### AD-004 — Roteiro adaptativo por ambiente

- **status**: active
- **decision**: Cada vistoria persiste seu próprio roteiro de ambientes. Cada ambiente exige uma visão geral e aceita detalhes adicionais. O tipo do imóvel apenas sugere ambientes e nunca impõe uma lista fixa.
- **rationale**: Imóveis reais possuem composições diferentes; flexibilidade sem estrutura prejudica a IA, enquanto um protocolo fixo exclui cenários válidos.

## Handoff

- **feature**: `interface-lente-operacional`
- **phase**: Implementação da rota estruturada
- **completed**: T1 — domínio, persistência e migration V8 do roteiro adaptativo
- **in_progress**: T2 — contratos de criação e edição do roteiro
- **next_step**: Implementar `POST /api/vistorias` atômico e `PUT /api/vistorias/{id}/roteiro`
- **blockers**: Nenhum
- **uncommitted_files**: `.impeccable/build/`, `.impeccable/review/`, `assets/`, `app/frontend/src/components/brand/`
- **branch**: `codex/especifica-interface-ia-first`
