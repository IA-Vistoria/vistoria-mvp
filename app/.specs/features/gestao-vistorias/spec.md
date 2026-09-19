# Gestão de Vistorias e Laudos Specification

## Problem Statement

Quando clientes comuns compram um apartamento, eles precisam realizar uma vistoria de entrega, mas contratar um engenheiro civil para fazer uma perícia técnica presencial e emitir um laudo é excessivamente caro. O sistema visa democratizar e baratear esse processo, permitindo que o próprio morador faça uma auto-vistoria guiada, auxiliada por Inteligência Artificial (IA) para gerar um pré-laudo, e posteriormente um engenheiro na plataforma apenas valide e assine o laudo final remotamente.

## Goals

- [x] O cliente deve ser capaz de criar uma vistoria e seguir um protocolo padronizado (checklist) para enviar fotos e dados do apartamento.
- [x] O sistema deve processar os dados da vistoria através de uma IA para gerar um "Pré-laudo" automático (adaptador mockado nesta versão; ver seção de decisões no README).
- [x] O cliente deve conseguir solicitar a assinatura de um engenheiro cadastrado na plataforma (fila técnica em `/api/vistorias/pendentes`, sem escolha manual de profissional — ver nota sobre "Alocação de Engenheiro" abaixo).
- [x] O engenheiro deve ser capaz de revisar as fotos, dados e o pré-laudo gerado pela IA para emitir e assinar o Laudo Definitivo.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
| --- | --- |
| Processamento de Pagamentos | O MVP focará no fluxo de vistoria e laudo. O pagamento pelo serviço do engenheiro será tratado fora da plataforma ou em uma feature futura. |
| Treinamento do Modelo de IA | O sistema consumirá uma API externa de IA (ex: OpenAI Vision ou Gemini) via prompt estruturado; não haverá treinamento de modelo próprio no escopo da aplicação backend. |
| Chat em Tempo Real | A comunicação cliente-engenheiro será assíncrona (solicitação -> análise -> laudo), sem chat live. |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here - nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| **Integração com IA assíncrona** | A geração do pré-laudo ocorrerá em background após o envio das fotos. | O processamento de múltiplas imagens pela IA pode ser demorado, causando timeout se for síncrono. | n |
| **Checklist (Protocolo) Fixo** | O MVP usará um protocolo padrão estático no banco (ex: "Quarto 1", "Banheiro", "Encanamento"). | Dinamizar checklists aumenta muito a complexidade inicial; um padrão fixo resolve 80% dos casos residenciais. | n |
| **Alocação de Engenheiro** | O cliente enviará a vistoria para um "pool" ou escolherá de uma lista de profissionais. Assumimos no MVP uma lista pública. | Necessário definir a mecânica exata de contratação no app. | n |
| **Armazenamento de Mídia** | Imagens e vídeos serão salvos no storage configurado na infraestrutura. | Reaproveita a infraestrutura já definida. | n |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Auto-vistoria e Protocolo ⭐ MVP

**User Story**: As a Cliente, I want criar uma nova vistoria e enviar fotos baseadas em um protocolo padronizado so that eu possa registrar o estado do meu apartamento.

**Why P1**: É a entrada de dados principal do sistema.

**Acceptance Criteria** (each line is one EARS pattern):

1. WHEN o cliente cria uma vistoria THEN o sistema SHALL gerar uma vistoria com status `EM_RASCUNHO` associada a um protocolo padrão.
2. WHILE a vistoria estiver `EM_RASCUNHO`, o cliente SHALL poder fazer upload de imagens associadas a itens do protocolo.
3. IF o upload for de um formato não suportado THEN o sistema SHALL rejeitar a requisição com erro HTTP 400.
4. WHEN o cliente finaliza o envio THEN o sistema SHALL alterar o status da vistoria para `AGUARDANDO_IA`.

**Independent Test**: Criar uma vistoria via API, fazer upload de uma imagem simulando um item do protocolo, e finalizar a vistoria mudando o status.

---

### P1: Geração de Pré-laudo por IA ⭐ MVP

**User Story**: As a Sistema, I want enviar os dados da vistoria finalizada para uma IA so that um pré-laudo estruturado seja gerado para o cliente.

**Why P1**: É o core value proposition do app: baratear a análise usando IA.

**Acceptance Criteria**:

1. WHEN o status da vistoria muda para `AGUARDANDO_IA` THEN o sistema SHALL enviar os dados e links das imagens para o provedor de IA.
2. IF o provedor de IA falhar ou der timeout THEN o sistema SHALL registrar o erro e manter o status como `FALHA_IA` para retentativa.
3. WHEN o provedor de IA retornar o resultado THEN o sistema SHALL salvar o pré-laudo e alterar o status da vistoria para `AGUARDANDO_ENGENHEIRO`.

**Independent Test**: Acionar o fluxo (mockando a API da IA) e verificar se o pré-laudo é salvo no banco e o status avança corretamente.

---

### P1: Revisão e Assinatura do Engenheiro ⭐ MVP

**User Story**: As an Engenheiro, I want visualizar a vistoria, as fotos e o pré-laudo gerado pela IA, e aprovar/assinar so that o laudo definitivo seja emitido.

**Why P1**: O pré-laudo não tem validade legal sem a validação humana de um engenheiro.

**Acceptance Criteria**:

1. WHERE o usuário logado tem o perfil `ROLE_ENGENHEIRO` the sistema SHALL permitir buscar vistorias no status `AGUARDANDO_ENGENHEIRO`.
2. WHEN o engenheiro aprova o pré-laudo THEN o sistema SHALL alterar o status da vistoria para `CONCLUIDA`.
3. WHEN o engenheiro aprova o pré-laudo THEN o sistema SHALL registrar a assinatura do engenheiro vinculado à vistoria.
4. IF o engenheiro reprova a vistoria THEN o sistema SHALL retornar a vistoria para status `DEVOLVIDA_CLIENTE` com um parecer.

**Independent Test**: Autenticar como engenheiro, aprovar uma vistoria em `AGUARDANDO_ENGENHEIRO` e verificar se a vistoria passa a `CONCLUIDA`.

---

## Edge Cases

- IF o cliente tenta alterar uma vistoria que não está `EM_RASCUNHO` THEN o sistema SHALL bloquear com HTTP 403/409.
- IF o cliente solicita revisão sem ter feito upload de nenhuma imagem THEN o sistema SHALL rejeitar a finalização.
- IF a IA identificar conteúdos impróprios nas imagens THEN o sistema SHALL abortar a geração e notificar a violação.
- WHEN o cliente ou o engenheiro consulta uma lista paginada THEN o sistema SHALL ordenar por `dataCriacao DESC` e `id DESC`, ignorando ordenação externa para preservar o contrato e o desempate determinístico.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| VISTORIA-01 | P1: Auto-vistoria e Protocolo | Implementing | Verified |
| VISTORIA-02 | P1: Geração de Pré-laudo por IA | Implementing | Verified (mock) |
| VISTORIA-03 | P1: Revisão e Assinatura do Engenheiro | Implementing | Verified |

**ID format:** `[CATEGORY]-[NUMBER]`
**Status values:** Pending → In Design → In Tasks → Implementing → Verified
**Coverage:** 3 total, 3 mapped a `.specs/features/gestao-vistorias/tasks.md` (T1–T7, todas concluídas e testadas)

> Revalidado em 2026-09-19: esta tabela estava desatualizada em relação ao código (marcava os três requisitos como `Pending`/`Specify` quando `VistoriaService`, `VistoriaController` e as migrations correspondentes já estavam implementados e testados). "Verified" aqui significa comportamento coberto por teste automatizado executado nesta revisão, não validação de produção.

---

## Success Criteria

- [x] Cliente consegue criar vistoria, fazer upload e submeter.
- [x] Pré-laudo é gerado via integração mock/real de IA (mock nesta versão).
- [x] Engenheiro consegue assumir o laudo e finalizá-lo com sucesso.
