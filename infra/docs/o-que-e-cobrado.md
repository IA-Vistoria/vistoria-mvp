# O que é cobrado e o que não é — ambiente OCI do Vistoria

Guia de referência rápida: por recurso, se é grátis (Always Free), se tem
limite, e quanto custaria se passasse do limite. Objetivo: o crédito de
R$ 1.571 (~US$ 300, válido 365 dias) durar o máximo possível.

**Resposta direta à pergunta "o painel mostra custo por recurso?"**: sim.
**Faturamento → Cost Management → Cost Analysis** já mostra o gasto
agrupado por serviço, compartment ou tag, sem precisar configurar nada
além do que já existe. Com a tag `project=vistoria-mvp` (ver
`spec-protecao-custo.md`), dá pra filtrar só o que é deste projeto.

## Resumo por recurso

| Recurso | Grátis? | Limite Always Free | O que acontece se passar |
| --- | --- | --- | --- |
| Compartment, grupos, policies, dynamic group (IAM) | ✅ Sempre grátis | Sem limite prático | — |
| VCN, subnet, Internet Gateway, route table, NSG | ✅ Sempre grátis | Sem limite prático | — |
| IP público da VM | ✅ Grátis (efêmero) | 1 por VM | — |
| Saída de dados (egress) | ✅ Grátis até 10 TB/mês | 10 TB/mês no tenancy | Cobrança por GB excedente (raro de bater nisso num projeto pequeno) |
| VM Compute (Ampere A1) | ✅ Grátis **só até 2 OCPU / 12 GB combinados** no tenancy (reduzido pela Oracle em jun/2026 — era 4/24) | 2 OCPU / 12 GB — **já estamos usando 100% desse limite** | Se alguém criar outra instância Ampere além disso, essa nova passa a ser cobrada |
| Boot volume da VM | ✅ Grátis até 200 GB combinados (Always Free) | 200 GB no tenancy | ~R$ 0,13/GB/mês acima disso (Block Volume) |
| Object Storage (bucket `vistoria-fotos`) | ✅ Grátis até 20 GB armazenados + 50.000 requisições/mês | 20 GB | ~R$ 0,13/GB/mês de armazenamento + custo por requisição acima disso |
| Autonomous Database | ✅ Sempre grátis, **travado em 20 GB / 1 ECPU** | Confirmado na própria API: mesmo o Terraform pedindo mais, a OCI ignora e trava em 20 GB — não tem como isso custar, mesmo por engano | — |
| **OCI Generative AI (on-demand)** | ❌ **Único serviço realmente pago aqui** | Sem camada grátis — cobra por token desde a primeira chamada | Ver abaixo |
| Budgets, Notifications, Events, Functions | ✅ Grátis pra esse volume de uso | Notifications: 1 milhão de mensagens/mês. Functions: 2 milhões de invocações + 400.000 GB-segundos/mês. Nosso uso real é de dezenas de chamadas, não milhões | — |

## Generative AI — o único custo real, em números

Modelo usado: `meta.llama-4-scout-17b-16e-instruct`.

- **Preço**: US$ 0,72 por **milhão** de tokens (entrada e saída).
- Uma chamada típica de pré-análise de uma foto de vistoria (imagem +
  texto de instrução + resposta) consome, de forma generosa, algo entre
  1.000 e 5.000 tokens.
- Isso dá **menos de R$ 0,02 por foto analisada**, no pior caso.
- Pra gastar R$ 50 só em Generative AI, seriam necessárias **mais de
  2.500 análises de foto** — bem acima do que um time pequeno testando
  localmente geraria em meses.

Ou seja: o risco real de custo neste projeto é baixíssimo. O ponto de
atenção não é "vamos gastar sem querer", é mais "alguém cria sem querer um
segundo recurso fora do Always Free" (ex: outra VM, um volume de bloco
grande) — é isso que a tag + o Cost Analysis + o budget de R$ 50 existem
pra pegar cedo.

## Onde conferir uso em tempo real

- **Faturamento → Cost Management → Cost Analysis**: gasto acumulado, por
  serviço ou por tag.
- **Governança → Limits, Quotas and Usage**: quanto de cada limite Always
  Free já foi usado (ex: quantos OCPU de Ampere A1 já estão alocados).
- **Faturamento → Assinaturas → Universal Credits**: saldo restante do
  crédito de R$ 1.571.

Nenhuma dessas telas precisa de configuração — já vêm prontas na conta.
