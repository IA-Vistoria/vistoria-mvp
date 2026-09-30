# IA autoral e relatório verificável — Contexto

**Gathered:** 30 de setembro de 2026
**Spec:** `.specs/features/ia-laudo-autoral/spec.md`
**Status:** Approved for task planning

---

## Feature Boundary

Continuar o caminho de infraestrutura definido pelo grupo, conectando a
aplicação ao OCI Generative AI por uma porta própria, tornando o resultado
automatizado a autoridade do relatório e separando a manifestação do
responsável. A entrega não cria validade jurídica ou responsabilidade técnica
profissional.

## Approaches Considered

1. **Reescrever toda a IA** — rejeitada porque descartaria serviço, adaptador,
   Docker, contrato e testes já aproveitáveis sem evidência de que estejam
   estruturalmente errados.
2. **Concluir a integração OCI definida pelo grupo** — escolhida. Preserva a
   porta `IaIntegrationService`, reutiliza região, compartment, policies e
   autenticação da infraestrutura e valida a resposta no backend.
3. **Usar apenas o Qwen local** — mantida como referência técnica, mas não atende
   à decisão de validar o caminho Oracle do grupo.

## Implementation Decisions

### Autoridade e manifestação

- A IA produz uma análise visual automatizada com resultado por ambiente e geral.
- O cliente pode concordar, contestar ou acrescentar contexto.
- A manifestação nunca apaga, renomeia ou altera gravidade, confiança e resultado da IA.
- O relatório pode ser emitido sem manifestação humana.

### Resultado e linguagem

- Resultados: `APROVADO`, `APROVADO_COM_RESSALVAS`, `NAO_APROVADO` e `INCONCLUSIVO`.
- A interface sempre qualifica o resultado como análise visual por IA.
- Limitações e confiança aparecem próximas da conclusão, não escondidas em rodapé.
- O produto não usa `laudo técnico`, `diagnóstico estrutural`, `conformidade NBR` ou equivalentes como promessa.

### Execução real e infraestrutura

- OCI Generative AI é o provedor real da entrega.
- Desenvolvimento usa `~/.oci/config`; VM usa instance principal e dynamic group.
- Região e compartment vêm da infraestrutura existente, sempre por configuração.
- Mock/fake existe somente em teste ou demonstração explicitamente marcada.
- Falha do provedor mantém evidências e registra `FALHA_IA`; não existe fallback silencioso.
- O modelo inicial é `google.gemini-2.5-flash` sob demanda via OCI, mas o
  identificador permanece configurável porque disponibilidade e aposentadoria
  mudam.

### Relatório

- Resumo executivo e resultado geral aparecem primeiro.
- Cada ambiente possui resultado, evidências e achados próprios.
- Aprovação, reprovação e inconclusão explicam critério, motivo, impacto e próxima ação.
- Contestação aparece junto ao achado, em bloco visual separado.
- Metadados de provedor, modelo, prompt e execução tornam o documento auditável.

### Agent's Discretion

- Estrutura interna dos novos DTOs e componentes, desde que preserve o contrato.
- Forma exata de persistir metadados dentro do JSON versionado da análise.
- Microcopy secundária e composição responsiva, respeitando o design system existente.
- Fixtures locais usadas na validação, desde que não exponham dados pessoais.

### Decisões confirmadas

- O usuário confirmou que o Gemini pode ser adotado desde que a integração
  continue alinhada à infraestrutura Oracle já criada pelo grupo.
- A Oracle permanece como borda de autenticação, autorização, região, cota e
  consumo; o Gemini é o modelo multimodal selecionado dentro do OCI Generative
  AI.

## Specific References

- Implementação introduzida no commit `ab63038 feat(mvp): integra VLM, inference e Docker do fluxo de IA`.
- Serviço Qwen em `inference/app/model_service.py`.
- Adaptador Spring em `integration/vlm/VlmIntegrationService.java`.
- Infraestrutura OCI em `infra/terraform/modules/iam/` e onboarding em `infra/docs/onboarding-backend-dev.md`.
- Jornada existente em `.specs/features/mvp-relatorio-ia/`.

## Deferred Ideas

- troca para modelo dedicado quando custo, cota e necessidade justificarem;
- execução local do Qwen como modo alternativo suportado;
- migração do storage e banco para serviços OCI;
- assinatura profissional e fluxo jurídico;
- normalização relacional de análises e achados;
- treinamento ou fine-tuning especializado;
- fila durável para processamento distribuído.
