# Integração OCI: armazenamento e IA reais

## Feature Overview

Substituir os adaptadores mockados de armazenamento e geração de pré-laudo por implementações reais contra a OCI (Object Storage e Generative AI), preservando as portas já existentes (`StorageService`, `IaIntegrationService`) sem alterar controllers, `VistoriaService` ou contratos HTTP.

## Problem Statement

A aplicação hoje funciona de ponta a ponta com armazenamento local (`LocalStorageService`) e pré-laudo mockado (`MockIaIntegrationService`). O ambiente de nuvem compartilhado do time (repositório `infra`, organização `IA-Vistoria-Nome-em-construcao`) já provisionou os recursos reais — bucket de Object Storage e Autonomous Database criados e testados; Generative AI e a VM de produção momentaneamente bloqueados por limites da conta trial. Falta o código do lado da aplicação que troca os mocks pelos adaptadores reais.

## Out of Scope

| Item | Motivo |
| --- | --- |
| Provisionamento de infraestrutura (Terraform/Ansible) | Já existe no repositório `infra`; este documento cobre só o código Java. |
| Fila assíncrona / processamento em background da IA | MVP mantém chamada síncrona; risco já registrado no design de `gestao-vistorias`. |
| Treinamento ou fine-tuning de modelo | Consome modelo multimodal já hospedado pela OCI Generative AI. |
| Autenticação/gestão de usuários OCI | Cada dev usa sua própria API key ou instance principal; fora do escopo do backend da aplicação. |

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Modelo multimodal vigente | `meta.llama-4-scout-17b-16e-instruct` | O modelo citado numa spec anterior do time (`meta.llama-3.2-90b-vision-instruct`) foi descontinuado — chamadas retornam 404. | n — confirmar de novo antes de codificar; a OCI aposenta modelos com frequência. |
| Uma chamada de IA por imagem, texto final montado em código | `OciGenAiIntegrationService` formata o JSON estruturado de cada foto em texto, sem uma segunda chamada de IA para redigir texto corrido | Evita custo e latência de uma chamada extra; só revisitar se o texto formatado ficar didaticamente pior que um texto corrido escrito por modelo | n |
| Alternância mock/real | Propriedades de configuração (`app.storage.provider`, `app.ia.provider`) resolvendo o bean ativo | Consistente com o resto da configuração do projeto; evita profile do Spring dedicado só para isso | n |
| Banco de dados | Trocar PostgreSQL por Oracle Autonomous DB nesta mesma feature | O ambiente compartilhado real provisionou Autonomous DB, não Postgres gerenciado | n |

**Open questions:** nenhuma bloqueante — os itens acima têm default e ficam confirmados durante a implementação (ver "Pendência a validar" em `design.md`).

## User Stories

### P1: Armazenamento real de evidências ⭐

**User Story**: Como sistema, quero persistir as evidências no Object Storage da OCI em vez do disco local, para que as fotos sobrevivam ao ciclo de vida da VM e sejam acessíveis de forma consistente entre implantações.

**Why P1**: Sem isso a VM de produção perde todas as fotos a cada recriação.

**Acceptance Criteria**:

1. WHEN o adaptador OCI está ativo THEN o upload de uma evidência SHALL gravar o arquivo no bucket configurado, sem expor a URL bruta do Object Storage no contrato HTTP.
2. WHEN uma evidência é solicitada por um usuário autorizado THEN o conteúdo SHALL ser lido do bucket e devolvido pela mesma rota já existente (`/vistorias/{id}/imagens/{imagemId}/conteudo`).
3. IF a chamada ao Object Storage falhar THEN o sistema SHALL traduzir a falha para `StorageException`, preservando a compensação já existente em `VistoriaService.uploadImagem`.

**Independent Test**: Com `app.storage.provider=oci` e credenciais válidas, fazer upload de uma evidência via API e confirmar que ela aparece no bucket `vistoria-fotos` e é recuperável pela rota de conteúdo.

---

### P1: Pré-laudo real via OCI Generative AI ⭐

**User Story**: Como sistema, quero enviar as evidências de uma vistoria para o modelo multimodal da OCI e gerar um pré-laudo real, para que a análise deixe de ser um texto fixo.

**Why P1**: É a proposta de valor central do produto.

**Acceptance Criteria**:

1. WHEN `submeterVistoria` aciona a IA com o adaptador OCI ativo THEN o sistema SHALL chamar o modelo multimodal vigente para cada evidência e compor um único texto de pré-laudo a partir dos achados estruturados.
2. IF a resposta da OCI não tiver o formato esperado THEN o sistema SHALL tratar como falha de IA (mesmo caminho de `FALHA_IA` já existente), nunca fabricando um pré-laudo.
3. WHEN o adaptador mock está ativo (`app.ia.provider=mock`) THEN nenhuma chamada de rede SHALL ocorrer — necessário para desenvolvimento de tela e para os testes automatizados.

**Independent Test**: Com `app.ia.provider=oci`, submeter uma vistoria com evidências reais e confirmar que `Vistoria.preLaudoIa` reflete achados específicos da imagem enviada, não um texto genérico.

---

### P2: Autenticação única com a OCI

**User Story**: Como desenvolvedor, quero que o mesmo código de aplicação funcione no meu laptop e na VM de produção sem alterar uma linha, para que eu não precise manter dois caminhos de autenticação.

**Why P2**: Consequência direta de rodar em dois ambientes (dev compartilhado e VM), não é a entrega de valor principal.

**Acceptance Criteria**:

1. WHERE a variável `OCI_AUTH_MODE=instance_principal` está definida (setada pelo Ansible só na VM) THEN o provider de autenticação SHALL ser `InstancePrincipalsAuthenticationDetailsProvider`.
2. WHERE essa variável não está definida THEN o provider SHALL ser `ConfigFileAuthenticationDetailsProvider`, lendo `~/.oci/config`.
3. WHEN qualquer um dos dois adaptadores reais (Object Storage, Generative AI) precisa se autenticar THEN ambos SHALL usar o mesmo bean de provider, sem decidir individualmente qual autenticação usar.

**Independent Test**: Subir o backend localmente com `~/.oci/config` válido e confirmar que os dois adaptadores reais autenticam sem configuração adicional; revisar o código para confirmar que nenhuma outra classe decide o modo de autenticação.

---

### P1: Persistência em Oracle Autonomous Database ⭐

**User Story**: Como sistema, quero persistir os dados em Autonomous Database em vez de PostgreSQL, para usar o banco já provisionado no ambiente compartilhado do time.

**Why P1**: Sem isso a aplicação não roda contra o ambiente real, só localmente com H2/Postgres.

**Acceptance Criteria**:

1. WHEN a aplicação sobe apontando para o Autonomous DB THEN todas as migrations Flyway SHALL aplicar sem erro de sintaxe específico de dialeto.
2. IF uma migration usa um tipo ou sintaxe incompatível com Oracle THEN ela SHALL ser corrigida ou substituída por uma nova migration compatível — nunca editando uma migration já aplicada em algum ambiente.
3. WHEN os testes de integração rodam contra o dialeto Oracle THEN o schema SHALL validar com `ddl-auto=validate`, sem depender de geração automática do Hibernate.

**Independent Test**: Rodar `flyway migrate` contra uma instância Oracle (Testcontainers ou Autonomous DB real) e confirmar sucesso; rodar a suíte de testes com o driver Oracle configurado.

## Edge Cases

- IF a conta OCI trial atingir o limite de requisições da Generative AI (`429`) THEN o sistema SHALL tratar como falha de IA recuperável (`FALHA_IA`, reenvio permitido), não como erro fatal.
- IF o modelo configurado (`GENAI_MODEL_ID`) for descontinuado pela OCI (`404`) THEN o sistema SHALL falhar de forma explícita e logada, nunca silenciosa.
- IF `OCI_AUTH_MODE` estiver ausente e não houver `~/.oci/config` acessível THEN a aplicação SHALL falhar ao subir com uma mensagem clara, no mesmo espírito da validação atual de `JWT_SECRET`.

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| OCI-01 | Armazenamento real de evidências | Specify | Pending |
| OCI-02 | Pré-laudo real via OCI Generative AI | Specify | Pending |
| OCI-03 | Autenticação única com a OCI | Specify | Pending |
| OCI-04 | Persistência em Oracle Autonomous Database | Specify | Pending |

**ID format:** `[CATEGORY]-[NUMBER]`
**Status values:** Pending → In Design → In Tasks → Implementing → Verified
**Coverage:** 4 total, 0 mapped a tasks implementadas — ver `tasks.md` desta feature para o plano.

## Success Criteria

- [ ] Upload e leitura de evidência funcionam contra o bucket real, sem alterar o contrato HTTP.
- [ ] Pré-laudo gerado reflete uma chamada real ao modelo multimodal vigente da OCI.
- [ ] O mesmo código de autenticação funciona sem alteração no laptop do dev e na VM.
- [ ] Migrations aplicam sem erro em Autonomous Database real.
- [ ] `app.ia.provider=mock` e `app.storage.provider=local` continuam funcionando sem nenhuma chamada de rede, para desenvolvimento e testes.
