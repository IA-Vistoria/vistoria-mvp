# QA de design e integração — roteiro adaptativo

**Status:** aprovado
**Data:** 2026-09-30
**Escopo:** acesso, início, configuração do roteiro, captura, análise por IA, revisão humana e relatório.

## Gates executados

| Gate | Evidência atual | Resultado |
| --- | --- | --- |
| Backend | `mvnw.cmd test` com Java 21: 168 testes, 0 falhas e 10 migrations validadas também no PostgreSQL 16 via Testcontainers | Passou |
| Frontend | `npm test`: 14 arquivos e 106 testes, 0 falhas | Passou |
| Lint | `npm run lint`: ESLint sem erros ou avisos | Passou |
| Build | `npm run build`: Next.js compilado, TypeScript validado e 8 páginas geradas | Passou |

## Fluxos funcionais validados

| Cenário | Roteiro final | Evidência da flexibilidade | Resultado |
| --- | --- | --- | --- |
| Casa | Entrada e fachada, Sala, Cozinha, Banheiro, Suíte do casal e Área de serviço | “Quarto” foi renomeado e “Área externa” foi removida antes da criação | Passou |
| Apartamento | Entrada, Sala, Cozinha, Banheiro, Quarto, Área de serviço e Varanda gourmet | A sugestão não trouxe garagem e aceitou uma varanda personalizada | Passou |
| Comercial | Entrada, Loja térrea e Banheiro | “Área principal” foi renomeada e “Área de apoio” foi removida | Passou |

O cenário comercial também percorreu a jornada completa usando o provedor local `mock`: três visões gerais foram associadas aos ambientes, a análise gerou três sugestões, o responsável confirmou uma, corrigiu uma e rejeitou uma, e o relatório final preservou foto, ambiente, observação da IA e decisão humana.

## Responsividade e legibilidade

As medições foram feitas no navegador com dados reais da API e sem overflow horizontal:

| Alvo | Medição observada | Estado visual |
| --- | --- | --- |
| 390 × 844 px | `innerWidth=390`, `scrollWidth=390` | Cabeçalho compacto, stepper 2 × 2, painel de ambientes em coluna e navegação inferior sem colisão |
| 768 × 1024 px | `innerWidth=767` por arredondamento do zoom do navegador, `scrollWidth=751`, `clientWidth=751` | Ambientes em trilho horizontal, captura em uma coluna e navegação inferior preservada |
| 1440 × 1000 px | `innerWidth=1440`, `scrollWidth=1423`, `clientWidth=1423` | Três áreas de trabalho visíveis, títulos e ações sem sobreposição |

Também foram conferidos nomes longos, progresso real, ações desabilitadas, foco por ambiente e o relatório em 1440 px. O relatório não cria rolagem horizontal e mantém o conteúdo imprimível agrupado pelo roteiro persistido.

## Achados corrigidos durante o QA

| Severidade | Achado | Correção | Verificação |
| --- | --- | --- | --- |
| P1 | O progresso da coluna esquerda e os textos de próximos passos herdavam cores escuras sobre fundo petróleo | Seletores específicos passaram a usar branco mineral e cinza-esverdeado de alto contraste | Estilos computados: títulos `rgb(255, 254, 250)` e apoio `rgb(185, 201, 199)` |
| P1 | O logo do relatório ainda recebia uma cápsula retangular herdada do layout antigo | A marca passou à variante inversa, sem borda, fundo, raio ou preenchimento externo | Estilo computado: `border: none` e fundo transparente |
| P2 | Capa e cartões fotográficos do relatório ainda usavam azul-marinho e efeito de vidro | As superfícies passaram para grafite-petróleo e o `backdrop-filter` foi removido | Capa computada em `rgb(8, 39, 43)` e anel em verdigris discreto |
| P2 | O item “Relatórios” apontava para um filtro sem comportamento próprio e podia omitir o documento mais recente | A rota passou a renderizar uma visão exclusiva, listar também o relatório primário e marcar o menu correto como ativo | Testes de componente e navegação; fluxo real em `/client?filtro=relatorios` com o relatório 0003 visível |
| P2 | Avisos de endereço e roteiro não estavam associados aos controles nem levavam o foco ao ponto de correção | Os erros agora usam `aria-invalid`, `aria-describedby` e foco determinístico; uploads e cartões de tipo ganharam `focus-within` visível | Testes de interação e inspeção no navegador: foco em `inspection-address`, descrito por `new-inspection-error` |
| P3 | A navegação móvel ainda mantinha desfoque translúcido, incompatível com a identidade sem vidro | Cabeçalho e navegação passaram a superfícies opacas e o CSS deixou de conter `backdrop-filter: blur` | Teste de fundações visuais e build de produção |
| P2 | A visão de relatórios filtrava somente o conteúdo da página corrente | `GET /api/vistorias/minhas` recebeu filtro opcional de status com ownership, paginação real e índice composto em V10 | Testes web, serviço, persistência e PostgreSQL; frontend solicita `RELATORIO_DISPONIVEL` ao servidor |
| P2 | A revisão ainda não associava validações à decisão, ao contexto ou ao tipo corrigido | Cada erro agora identifica o controle, anuncia o aviso e move o foco; os cartões de decisão têm foco visível | Testes de interação para os três campos e teste visual de `focus-within` |
| P2 | Relatório e resultado criavam um segundo landmark `main` dentro do shell | As duas superfícies passaram a seções nomeadas pelo título | Teste de componentes garante ausência de `main` adicional |

## Clareza do produto

- O tipo do imóvel é apresentado como sugestão inicial, não como regra.
- O cliente pode adicionar, renomear, reordenar e remover ambientes sem evidências.
- A única exigência de completude é uma visão geral por ambiente; detalhes são opcionais.
- “Pular por agora” apenas muda o foco e não altera o progresso.
- A IA só recebe as fotos depois da estrutura mínima e suas sugestões permanecem separadas da decisão do responsável.
- O relatório deixa explícito que não é laudo técnico nem substitui avaliação presencial.

## Verificação independente final

A terceira revisão independente do diff confirmou o fechamento dos achados anteriores: o filtro de relatórios ocorre no backend antes da paginação, os campos da revisão possuem associação e foco acessíveis, e relatório e resultado não criam landmarks `main` aninhados. O veredito foi **PASS**, com **0 P0, 0 P1 e 0 P2 abertos**.

final result: passed
