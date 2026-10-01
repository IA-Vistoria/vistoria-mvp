# IA autoral e relatório verificável — Validação final

**Data:** 2026-09-30
**Spec:** `.specs/features/ia-laudo-autoral/spec.md`
**Diff range:** `84bbffa..899ae25`
**Verifier:** subagente independente (autor ≠ verificador)

## Validation: PASS local ✅

**Veredito:** a implementação cumpre IAR-01 a IAR-06 nos testes e na UAT
local. O smoke real da OCI permanece **BLOQUEADO EXTERNAMENTE** porque exige
credenciais, cota e autorização para uma chamada com possível custo; ele não é
contabilizado como PASS real.

## Resultado da revisão independente

- P0: nenhum.
- P1: nenhum.
- P2: nenhum aberto.
- O primeiro ciclo identificou observabilidade incompleta em IAR-06.
- O commit `899ae25` adicionou health check seguro e logs de início, conclusão,
  falha, evento ignorado e resultado descartado.
- A revalidação independente confirmou o fechamento do P2.

## Rastreabilidade

| Requisito | Evidência verificada | Resultado |
| --- | --- | --- |
| IAR-01 | OCI é o provider principal; mock só existe em `test`/`demo`; falhas reais não acionam fallback nem criam relatório parcial. Evidência: `app/src/test/java/br/com/vistoriapredial/config/IaProviderSelectionTest.java:27`. | ✅ PASS local |
| IAR-02 | Solicitação, schema e documento v2 preservam imagem, ambiente, categoria, achados e metadados de execução. Evidência: `app/src/test/java/br/com/vistoriapredial/integration/oci/genai/OciGenAiIntegrationServiceTest.java:85`. | ✅ PASS local |
| IAR-03 | Resultado por ambiente e geral é calculado pelo backend; somente documento integralmente válido conduz a `RELATORIO_DISPONIVEL`. Evidências: `app/src/test/java/br/com/vistoriapredial/vistoria/application/analysis/ResultadoAnaliseCalculatorTest.java:94` e `app/src/main/java/br/com/vistoriapredial/vistoria/domain/Vistoria.java:365`. | ✅ PASS local |
| IAR-04 | Manifestação é opcional, separada e não altera o achado nem a conclusão automatizada. Evidência: `app/src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java:230`. | ✅ PASS local |
| IAR-05 | Relatório apresenta conclusão, motivo, ambientes, evidências, achados, limitações, recaptura, rastreabilidade e contestação separada. Evidência: `app/frontend/src/features/inspections/client/inspection-report.tsx:127`. | ✅ PASS local |
| IAR-06 | Falhas OCI têm categoria; logs não incluem imagem/payload/segredo; `GET /api/health/ia` verifica configuração e autenticação sem chamada externa. Evidências: `app/src/main/java/br/com/vistoriapredial/integration/oci/genai/OciGenAiIntegrationService.java:118`, `app/src/main/java/br/com/vistoriapredial/vistoria/application/analysis/VistoriaAnalysisProcessor.java:105` e `app/src/test/java/br/com/vistoriapredial/integration/oci/genai/config/OciGenAiHealthControllerTest.java:34`. | ✅ PASS local |

## Gates frescos

| Gate | Resultado |
| --- | --- |
| Java 21 `app\\.\\mvnw.cmd test` | ✅ 283 testes, 0 falhas, 0 erros; PostgreSQL 16 via Testcontainers e 10 migrations aplicadas. |
| Testes focados de observabilidade | ✅ 52 testes, 0 falhas e 0 erros. |
| `frontend\\npm test` | ✅ 15 arquivos e 130 testes. |
| `frontend\\npm run lint` | ✅ Sem erros. |
| `frontend\\npm run build` | ✅ Build Next.js concluído e rotas geradas. |
| Docker Compose OCI | ✅ Configuração combinada validada com valores sintéticos, sem segredo real. |
| `git diff --check` | ✅ Sem erro de whitespace nos arquivos da feature. |

## UAT local considerada

- fluxo completo em `demo/mock` com quatro ambientes e quatro resultados
  distintos: `NAO_APROVADO`, `APROVADO_COM_RESSALVAS`, `APROVADO` e
  `INCONCLUSIVO`;
- associação ambiente → evidência → resultado preservada;
- contestação gravada sem alterar “Não aprovado na análise visual”;
- relatório preserva a contestação em bloco separado;
- desktop e mobile inspecionados, sem overflow em 375, 768 e 1440 px;
- ações principais têm foco visível, contraste adequado e alvos de toque
  compatíveis com uso móvel;
- o modo demo identifica explicitamente que usa cenários simulados e não
  interpreta a imagem real.

## Observabilidade e privacidade

- início, conclusão, falha, evento ignorado e resultado descartado registram
  vistoria, provider, modelo, duração, categoria e analysisId quando disponível;
- mensagens não registram bytes/base64, conteúdo integral da imagem, prompt,
  payload do modelo, contexto integral, chave, token ou mensagem bruta da
  exceção;
- o health check autenticado informa somente provider, região, modelo, modo de
  autenticação e disponibilidade local, sempre com
  `externalCallPerformed=false`.

## Bloqueio externo

Nenhuma chamada real foi feita à Oracle nesta validação. Permanecem não
comprovadas no ambiente da conta do time: disponibilidade regional do modelo,
permissões IAM, validade das credenciais, cota/créditos e resposta runtime do
Gemini via OCI. O procedimento opt-in está em `app/docs/oci-genai-smoke.md` e
deve registrar `PASS real` ou `BLOQUEADO EXTERNAMENTE`, nunca sucesso presumido.
