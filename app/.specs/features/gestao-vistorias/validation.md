# Gestão de Vistorias — Validação T8/T9

**Data**: 2026-09-19
**Spec**: `.specs/features/gestao-vistorias/spec.md`
**Diff**: `76ae657..HEAD` (`7da93c6`, `e9aa928`)
**Verificador**: subagente independente (autor != verificador)
**Escopo**: exclusivamente T8, T9 e o edge case novo de ordenação determinística; T1–T7 não foram reabertos.

---

## Validation: PASS

## Tarefas no escopo

| Tarefa | Resultado |
| --- | --- |
| T8 — ordenação determinística e segura | ✅ `VistoriaController.java:27-29` define a ordem fixa; `VistoriaController.java:62-63` e `VistoriaController.java:122-123` a aplicam às duas rotas. |
| T9 — índices compostos de paginação | ✅ `V6__add_vistoria_pagination_indexes.sql:2-6` declara os índices; `PostgreSqlMigrationIntegrationTest.java:27` e `PostgreSqlMigrationIntegrationTest.java:56-64` os validam em PostgreSQL vazio. |

## Critérios ancorados na spec e tasks

| Critério | Resultado definido | Evidência `file:line` + asserção | Resultado |
| --- | --- | --- | --- |
| T8: `/minhas` preserva `page` e `size`, fixa `dataCriacao DESC, id DESC` e ignora `sort` | Página 3, tamanho 4, ordem exata e HTTP 200 com `sort=campoInexistente,asc`. | `VistoriaControllerTest.java:238-248` — `status().isOk()`, `isEqualTo(3)`, `isEqualTo(4)` e `containsExactly(desc(dataCriacao), desc(id))`. | ✅ PASS |
| T8: `/pendentes` preserva `page` e `size`, fixa `dataCriacao DESC, id DESC` e ignora `sort` | Página 2, tamanho 7, ordem exata e HTTP 200 com `sort=campoInexistente,asc`. | `VistoriaControllerTest.java:199-209` — `status().isOk()`, `isEqualTo(2)`, `isEqualTo(7)` e `containsExactly(desc(dataCriacao), desc(id))`. | ✅ PASS |
| Edge case novo: lista paginada ignora ordenação externa e mantém desempate determinístico | As duas rotas usam `dataCriacao DESC, id DESC` mesmo diante de campo externo inválido. | `VistoriaController.java:145-146` recria o `PageRequest` com a constante; `VistoriaControllerTest.java:199-209` e `VistoriaControllerTest.java:238-248` verificam as duas rotas. | ✅ PASS |
| T9: Flyway aplica V6 em PostgreSQL vazio | Migração bem-sucedida e schema alvo `6`. | `PostgreSqlMigrationIntegrationTest.java:26-27` — `isTrue()` e `isEqualTo("6")`. | ✅ PASS |
| T9: índice por cliente e ordenação | Índice `idx_vistoria_cliente_criacao_id` com `(cliente_id, data_criacao DESC, id DESC)`. | `PostgreSqlMigrationIntegrationTest.java:56-62` — `containsKeys(...)` e `contains("(cliente_id, data_criacao DESC, id DESC)")`. | ✅ PASS |
| T9: índice por status e ordenação | Índice `idx_vistoria_status_criacao_id` com `(status, data_criacao DESC, id DESC)`. | `PostgreSqlMigrationIntegrationTest.java:56-64` — `containsKeys(...)` e `contains("(status, data_criacao DESC, id DESC)")`. | ✅ PASS |

## Discrimination Sensor

O sensor usou um worktree descartável em `HEAD`; o clone real nunca recebeu mutações. Ambas as mutações foram comportamentais e os testes focais foram executados fora do sandbox.

| Mutação | Alvo | Teste focal | Evidência de morte | Resultado |
| --- | --- | --- | --- | --- |
| Trocar o desempate `id DESC` por `id ASC` | `VistoriaController.java:29` | `VistoriaControllerTest` | 20 executados; 2 falhas nas asserções de ordem em `VistoriaControllerTest.java:207` e `VistoriaControllerTest.java:246`. | ✅ Morta |
| Remover `id DESC` do índice por status | `V6__add_vistoria_pagination_indexes.sql:6` | `PostgreSqlMigrationIntegrationTest` | 1 executado; 1 falha em `PostgreSqlMigrationIntegrationTest.java:64`, pois a definição retornada não continha `id DESC`. | ✅ Morta |

**Profundidade**: leve (2 mutações, proporcional ao escopo T8/T9).
**Resultado**: 2/2 mutações mortas — PASS.

## Gate obrigatório

- **Comando**: `.\\mvnw.cmd clean test` (executado fora do sandbox, devido à limitação conhecida da identidade restrita).
- **Resultado**: 107 executados, 0 falhas, 0 erros, 0 ignorados — `BUILD SUCCESS`.
- **Integridade de testes**: 99 métodos `@Test` em `76ae657`; 101 em `HEAD`; delta estático `+2`, correspondente aos dois testes adicionados por T8. A contagem do Surefire é 107 por incluir outros testes parametrizados/dinâmicos.
- **Testes focais**: `VistoriaControllerTest` e `PostgreSqlMigrationIntegrationTest` exercitados pelos sensores e ambos mataram seus respectivos mutantes.

## Qualidade do diff

| Checagem | Resultado |
| --- | --- |
| Mudança mínima e sem escopo extra | ✅ Constante de `Sort`, helper privado, dois testes de contrato e migration aditiva. |
| Apenas arquivos requeridos | ✅ Diff T8/T9 limitado a controller, testes, migration e documentação correlata. |
| Formatação | ✅ `git diff --check 76ae657..HEAD` sem saída. |
| Cobertura por camada e critérios | ✅ Controller cobre as duas rotas e o edge case; integração PostgreSQL valida versionamento e definições reais de índice. |
| Diretrizes documentadas | ✅ Conforme `AGENTS.md`: Maven Wrapper, testes de controller e Testcontainers PostgreSQL. |

## Isolamento

O worktree de sensor foi removido. O `git status --porcelain` do clone real é igual ao baseline de código: nenhuma alteração de código, teste, migration, spec, design ou tasks; somente este `validation.md` não rastreado, escrita autorizada para a validação.

## Conclusão

**Overall**: ✅ Ready

T8/T9 cumprem os resultados definidos em `tasks.md`; o novo edge case da spec é coberto de forma específica nas duas rotas; o gate completo e os dois sensores independentes passaram.
