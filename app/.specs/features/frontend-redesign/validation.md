# Validação final — Experiência Vistor.IA

**Status**: PASS
**Data**: 2026-09-19
**Escopo**: interfaces cliente e engenheiro, contratos REST, segurança, persistência, concorrência, responsividade e empacotamento.

## Resultado executivo

A experiência Vistor.IA foi validada de ponta a ponta com frontend e backend reais. O cliente cadastra a vistoria, envia evidências e acompanha o fluxo; o engenheiro acessa a fila, revisa o pré-laudo e devolve ou aprova com parecer. O pré-laudo permanece preliminar e nenhuma decisão técnica é atribuída à IA.

O fechamento também comprovou segredo JWT obrigatório, convite administrativo para cadastro de engenheiro, respostas RFC 9457, autorização por recurso, controle otimista das decisões e migration V4 no PostgreSQL 16.

## Evidência por requisito

| Requisito | Evidência de implementação | Evidência automatizada / UAT | Resultado |
| --- | --- | --- | --- |
| FUX-01 — identidade, cadastro e acesso | `frontend/src/features/auth/AuthForm.tsx:23`, `src/main/java/br/com/vistoriapredial/usuario/application/UsuarioService.java:41`, `src/main/java/br/com/vistoriapredial/config/security/SecurityConfig.java:55` | `frontend/src/features/auth/AuthForm.test.tsx:45`, `src/test/java/br/com/vistoriapredial/usuario/web/AuthControllerTest.java:51`; smoke HTTP 201/403/403/201/200/200 | PASS |
| FUX-02 — jornada do cliente | `frontend/src/features/inspections/client/client-dashboard.tsx:13`, `frontend/src/features/inspections/client/inspection-workflow.tsx:56`, `src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java:65` | `frontend/src/features/inspections/client/inspection-workflow.test.tsx:43`, `src/test/java/br/com/vistoriapredial/vistoria/application/VistoriaServiceTest.java:84`; UAT de rascunho, upload, submissão, devolução, complemento e reenvio | PASS |
| FUX-03 — revisão de engenharia | `frontend/src/features/inspections/engineer/engineer-review.tsx:31`, `frontend/src/features/inspections/engineer/pre-report.tsx:1`, `src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java:178` | `frontend/src/features/inspections/engineer/engineer-workspace.test.tsx:38`, `src/test/java/br/com/vistoriapredial/vistoria/web/VistoriaControllerTest.java:168`; UAT de devolução e aprovação final | PASS |
| FUX-04 — evidências autenticadas | `src/main/java/br/com/vistoriapredial/vistoria/web/VistoriaController.java:60`, `src/main/java/br/com/vistoriapredial/vistoria/application/VistoriaService.java:133`, `src/main/java/br/com/vistoriapredial/vistoria/application/EvidenceFileValidator.java:13` | `src/test/java/br/com/vistoriapredial/vistoria/web/VistoriaControllerTest.java:123`, `src/test/java/br/com/vistoriapredial/storage/LocalStorageServiceTest.java:115`; ownership 403, ausência 404 e compensação | PASS |
| FUX-05 — sistema visual | `frontend/src/app/globals.css:3`, `frontend/src/components/DashboardShell.tsx:29`, `frontend/src/app/globals.css:886` | inspeção em 375, 768 e 1440 px, teclado, foco visível, movimento reduzido e ausência de overflow | PASS |
| FUX-06 — HTTP e falhas observáveis | `frontend/src/lib/api.ts:7`, `frontend/src/features/auth/ProtectedArea.tsx:21`, `src/main/java/br/com/vistoriapredial/shared/web/error/GlobalExceptionHandler.java:49` | `frontend/src/lib/api.test.ts:24`, `src/test/java/br/com/vistoriapredial/config/security/SecurityCorsTest.java:15`; CORS real e smoke protegido | PASS |

## Gates executados

| Gate | Evidência atual | Resultado |
| --- | --- | --- |
| Backend completo | `.\mvnw.cmd test` — 87 testes, 0 falhas, 0 erros | PASS |
| PostgreSQL real | Testcontainers PostgreSQL 16.15; 4 migrations até V4; coluna `version BIGINT NOT NULL DEFAULT 0` | PASS |
| Frontend completo | `npm test -- --run` — 7 arquivos, 55 testes | PASS |
| Qualidade estática | `npm run lint` — zero erro | PASS |
| Build Next.js | `npm run build` — 9 rotas geradas | PASS |
| Imagem frontend | `docker build --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080/api ...` | PASS |
| Startup seguro | JAR sem `JWT_SECRET` encerrou com código 1 por placeholder obrigatório | PASS |
| Smoke HTTP real | cliente 201; engenheiro sem convite 403; convite incorreto 403; convite válido 201; login 200; rota protegida 200 | PASS |

## UAT integrado

- Cadastro/login e redirecionamento por perfil foram exercitados com API real.
- Cliente criou um único rascunho, rejeitou arquivo inválido, enviou JPEG válido, recarregou mantendo progresso e submeteu a vistoria.
- Engenheiro abriu evidência autenticada e pré-laudo preliminar, teve parecer vazio bloqueado, devolveu o caso e posteriormente aprovou o reenvio.
- Outro cliente recebeu 403 ao tentar acessar a evidência; CORS aceitou somente a origem configurada.
- As telas foram verificadas em 375, 768 e 1440 px, sem overflow horizontal, com foco visível, labels, regiões `aria-live` e movimento reduzido.
- Capturas estão em `.specs/features/frontend-redesign/evidence/`.

## Sensor de discriminação

Uma cópia temporária isolada recebeu quatro mutações deliberadas. Todas foram mortas pelos gates correspondentes:

1. redirecionamento por perfil incorreto — três testes falharam;
2. `Content-Type` JSON forçado em `FormData` — teste do cliente HTTP falhou;
3. remoção do ownership de evidência — teste do serviço falhou;
4. parecer técnico vazio permitido — teste de revisão falhou.

O repositório real não recebeu essas mutações. A cópia de sensor permaneceu fora do workspace por segurança operacional.

## Revisão independente

A revisão por subagente encontrou e motivou os últimos endurecimentos: JWT sem fallback público, convite profissional, 401/403/409 em `application/problem+json`, versionamento otimista, limite multipart com overhead, sessão expirada persistente e Docker reproduzível. Uma segunda inspeção não encontrou novo bloqueador nesses pontos; o convite foi então mascarado, limpo após erro/troca de perfil e incorporado à especificação.

## Limites conscientes

- O pré-laudo usa adaptador mock e não chama provedor externo pago.
- O armazenamento ativo é local atrás de `StorageService`.
- O convite é controle administrativo do MVP e não substitui verificação formal do CREA.
- O token permanece em `localStorage` conforme escopo aprovado; cookie HttpOnly/refresh token ficam para evolução posterior.

## Validação complementar — T11

O cliente foi consolidado em uma jornada guiada com stepper, protocolo lateral, item em foco e orientação de envio. A engenharia passou a reunir fila, galeria, pré-laudo e decisão em um único workspace coerente com a mesma identidade visual.

- Comparação lado a lado registrada em `evidence/qa-cliente-lado-a-lado.png` e `evidence/qa-engenheiro-lado-a-lado.png`.
- Viewports de 1488 × 1056, 768 × 1024 e 375 × 812 inspecionadas sem overflow horizontal.
- Stepper móvel reorganizado em duas colunas e conteúdo em foco antecipado na ordem de leitura.
- Revisão independente identificou o stepper estático após o envio; a correção passou a derivar etapas concluídas/ativas do status real e recebeu teste de regressão.
- Seletor de evidências repetidas recebeu nomes acessíveis únicos e teste de regressão.
- `design-qa.md` registra os achados corrigidos e as diferenças deliberadas por ausência de dados no contrato.
- `npm test -- --run`: 7 arquivos, 55 testes, 0 falhas.
- `npm run lint`: 0 erros.
- `npm run build`: 9 rotas geradas.
- `.\mvnw.cmd test`: 87 testes, 0 falhas, incluindo PostgreSQL 16 real por Testcontainers.

## Conclusão

Os critérios de aceite, gates técnicos e verificações integradas desta feature receberam PASS. Não há blocker conhecido para commit e push das mudanças pertencentes ao escopo.
