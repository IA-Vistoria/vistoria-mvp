# Spec: proteção de custo no ambiente OCI do Vistoria

## Objetivo

Garantir que o ambiente compartilhado de dev/teste do **Vistoria** na OCI
nunca gere cobrança real no cartão cadastrado — só deve consumir o crédito
promocional da conta (Pay As You Go, ~R$ 1.571 / 365 dias, 0% usado na
última checagem). Isso cobre: tags de custo, orçamento com alerta, e uma
trava automática que bloqueia o serviço que realmente cobra (Generative AI)
se o gasto passar do esperado.

## Contexto que precisa ser revalidado ao retomar este trabalho

- **A Oracle reduziu o Always Free do shape Ampere A1** de 4 OCPU/24GB para
  **2 OCPU/12GB**, em 15/jun/2026, sem aviso público (fonte: InfoQ, jul/2026).
  A VM do projeto (`vistoria-mvp-vm`, 2 OCPU/12GB) já usa **100% do novo
  limite** — não há mais margem Always Free pra outra instância Ampere no
  mesmo tenant.
- Foi encontrado um **Budget criado manualmente no console**, chamado
  `Aviso-de-gasto`, valor **R$ 1,00**, mirando o **tenancy inteiro** (não o
  compartment do projeto) — provavelmente um teste. Este spec propõe
  substituí-lo pelo budget correto via Terraform (ver abaixo). Conferir se
  ainda existe antes de criar o novo, pra não duplicar.
- Nenhuma Function, Application de Functions ou tópico de notificação
  existia na checagem mais recente (compartment `vistoria-mvp-dev`).

## O que já está provisionado (não mexer, só adicionar tags/observabilidade)

| Recurso | Identificador |
| --- | --- |
| Tenancy | `ocid1.tenancy.oc1..aaaaaaaaitguzxycgpkwkjp5a7q224hyrb723nsrqpbfz2vf55waivoxoxta` |
| Compartment do projeto | `vistoria-mvp-dev` — `ocid1.compartment.oc1..aaaaaaaaogefvhjugrkowz3p5bbtqjvypatdq4c7qtqebn2ef4bciyi44xuq` |
| Região | `sa-saopaulo-1` |
| VM | `vistoria-mvp-vm`, IP público `144.22.164.25`, acessível via `ssh -i ~/.ssh/id_ed25519 ubuntu@144.22.164.25` |
| Bucket | `vistoria-fotos` |
| Autonomous Database | Always Free, 26ai, wallet em `~/.oci/wallets/vistoria-mvp-db-wallet.zip` (local, nunca commitado) |
| Modelo Generative AI validado | `meta.llama-4-scout-17b-16e-instruct` — `ocid1.generativeaimodel.oc1.sa-saopaulo-1.amaaaaaask7dceya55tdu24pvp53kg6gecei4byyjyjfjas2xfek7toptemq` (confirmar se ainda é o vigente antes de reusar — a OCI aposenta modelos com frequência, ver `infra/docs/onboarding-backend-dev.md`) |
| E-mail para alertas de orçamento | `soujoaomoraes@gmail.com` |
| Valor do orçamento aprovado | **R$ 50** (não dólar, não R$ 100 — confirmado com o usuário) |

Terraform já aplicado via backend remoto (bucket `vistoria-mvp-terraform-state`,
S3-compatível) — ver `infra/README.md`, seção "State do Terraform", para as
variáveis de ambiente necessárias (`AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `AWS_REQUEST_CHECKSUM_CALCULATION=when_required`,
`AWS_RESPONSE_CHECKSUM_VALIDATION=when_required`).

## Passo 1 — Tags de rastreamento de custo (Terraform)

Adicionar `freeform_tags = { project = "vistoria-mvp" }` (via uma variável
`tags` propagada por todos os módulos) aos recursos que suportam tag:
compartment, VCN, subnet, NSG, route table, internet gateway, bucket,
Autonomous Database, instância Compute. Permite filtrar o relatório nativo
**Cost Analysis** (Faturamento → Cost Management → Cost Analysis) por essa
tag, pra ver quanto cada serviço está custando dentro do projeto.

Não precisa de Defined Tag Namespace — freeform tag é suficiente e mais
simples pro tamanho deste projeto.

## Passo 2 — Budget com alerta (Terraform)

Novo módulo `infra/terraform/modules/budget/`:

- `oci_budget_budget`: criado no **tenancy** (budgets só podem ser criados
  no compartment raiz), `target_type = "COMPARTMENT"`,
  `targets = [compartment_id do vistoria-mvp-dev]`, `amount = 50`,
  `reset_period = "MONTHLY"`.
- Três `oci_budget_alert_rule`: `threshold_type = "PERCENTAGE"`,
  `type = "ACTUAL"`, thresholds em `50`, `80`, `100`, `recipients =
  "soujoaomoraes@gmail.com"`.
- Substituir (ou instruir a apagar manualmente) o budget antigo
  `Aviso-de-gasto` (R$ 1, escopo tenancy) pra não conflitar/confundir.

Isso sozinho já cobre "me avisar antes de gastar" — é o que a OCI oferece
nativamente, sem automação extra.

## Passo 3 — Bloqueio automático (zero-quota via prebuilt Function)

Baseado no padrão oficial da Oracle ("Enforced budgets on OCI using
functions and quotas" + "prebuilt functions" — blogs.oracle.com, acesso
direto bloqueado pra fetch automatizado, pesquisar via Google se precisar
reconfirmar detalhes).

Arquitetura: **Budget alert (100%) → Events Rule (evento `TriggeredAlert`
do serviço `budget`, filtrado pelo `budgetId`) → OCI Function (prebuilt,
não precisa escrever código nem Docker) → aplica uma quota policy do tipo
"zero" no compartment**, ex:

```
Zero generative-ai-family quotas in compartment vistoria-mvp-dev
```

Passos (via console, feitos pelo usuário — exigem Cloud Shell ou catálogo
de prebuilt functions, não automatizável de forma confiável por aqui):

1. Confirmar que o Budget do Passo 2 está criado e ACTIVE.
2. No console: **Developer Services → Functions → Applications** → criar
   uma Application no compartment `vistoria-mvp-dev`, associada à VCN/subnet
   já existentes.
3. Procurar a prebuilt function de "zero quota" / enforcement de budget no
   catálogo de Functions da Oracle (nome exato a confirmar no console —
   pesquisar "budget" ou "quota" no catálogo) e implantar na Application
   acima.
4. Criar um **Dynamic Group** (ex: `vistoria-mvp-budget-fn-dg`) com matching
   rule pelo OCID da function, e uma **Policy** permitindo esse dynamic
   group gerenciar quotas no tenancy (`Allow dynamic-group
   vistoria-mvp-budget-fn-dg to manage quota in tenancy`).
5. **Governança → Cost Management → Budgets → (o budget criado) → Events**:
   criar uma Events Rule com condição "Triggered Alert" do serviço Budget,
   filtrando pelo OCID deste budget, ação = invocar a function do passo 3.
6. **Testar de verdade antes de confiar**: forçar o threshold (ou simular)
   e confirmar que, depois da quota aplicada, uma chamada de teste ao
   Generative AI (`infra/scripts/smoke-tests/test_genai_chat.py`) passa a
   falhar. **Não assumir que funciona sem esse teste** — não há confirmação
   de que uma zero-quota em `generative-ai-family` bloqueia chamadas
   pay-per-uso já em andamento (a documentação de Quotas fala
   principalmente de contagem de recursos provisionados, não de chamadas de
   API). Se não bloquear, documentar a limitação e manter só o alerta por
   e-mail como proteção real.
7. Depois de confirmado, remover a quota manualmente pra destravar o uso
   de novo (a zero-quota não se desfaz sozinha).

## Passo 4 — Observabilidade (sem construir nada novo)

Não construir Grafana nem painel customizado — desproporcional pro tamanho
do projeto e a própria OCI já oferece:

- **Faturamento → Cost Management → Cost Analysis**: gasto por tag/serviço.
- **Governança → Limits, Quotas and Usage**: uso atual vs. limite de cada
  serviço (inclusive Always Free), já usado nesta conversa pra descobrir o
  `max-on-demand-chat-request-per-minute-count`.

## Pendências não relacionadas, encontradas durante este trabalho

- Branch `codex/especifica-interface-ia-first` apareceu no repositório
  `IA-Vistoria/vistoria-mvp` (possivelmente o time de app já começou a
  trabalhar) — ainda não revisada.
- Repositório antigo `IA-Vistoria/infra`: usuário pediu pra arquivar
  (Settings → Danger Zone → Archive), ação pendente de confirmação se foi
  feita.
- Ansible ainda não foi rodado contra a VM (`vistoria-mvp-vm`,
  `144.22.164.25`) — Docker/nginx/app ainda não instalados nela. A role
  `app` precisa ser ajustada: o repositório da aplicação agora é o
  monorepo `IA-Vistoria/vistoria-mvp`, usando só a pasta `app/` dele — a
  variável `app_git_repo_url` e a lógica de clone em
  `infra/ansible/roles/app/tasks/main.yml` ainda apontam pro modelo antigo
  (repo dedicado), precisam ser adaptadas pra monorepo (clonar o repo
  inteiro e usar `app/` como contexto, ou usar git sparse-checkout).

## Critério de pronto

- `terraform apply` cria as tags, o budget de R$ 50 e os 3 alert rules sem
  erro, substituindo o budget manual de R$ 1.
- Um e-mail de teste chega em `soujoaomoraes@gmail.com` quando um
  threshold é cruzado (ou ao menos a alert rule aparece corretamente
  configurada no console).
- A automação de zero-quota foi testada de verdade contra uma chamada real
  ao Generative AI, com o resultado (bloqueou ou não) documentado aqui.
- Cost Analysis consegue filtrar pela tag `project=vistoria-mvp`.
