# Vistoria Predial com IA Multimodal Specification

## Problem Statement

A vistoria de entrega de imóveis é um processo crítico, manual e suscetível a erros ou omissões técnicas por parte do comprador leigo, demandando tempo escasso de engenheiros civis para triagem presencial. A ausência de um mecanismo de pré-avaliação ágil gera atrasos na contestação de vícios construtivos aparentes (como trincas, desníveis e infiltrações). Este projeto resolve o problema ao fornecer uma plataforma web onde o comprador submete fotos dos cômodos para análise preliminar imediata por IA multimodal (Gemini via Oracle OCI), seguida de homologação técnica estruturada por um engenheiro civil.

## Goals

- [ ] Disponibilizar interface web simples (HTML/CSS/JS) para envio de fotos e acompanhamento de vistorias.
- [ ] Implementar backend Spring Boot 4.1.1 para orquestrar uploads, persistência relacional e integração com a IA da Oracle.
- [ ] Gerar pré-laudos automatizados via IA multimodal com identificação e classificação de anomalias construtivas.
- [ ] Oferecer painel exclusivo para o engenheiro civil auditar, editar e homologar o laudo final (Human-in-the-Loop).

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
| ------- | ------ |
| Treinamento ou fine-tuning de modelo de visão computacional | O modelo Gemini 2.5 Pro no OCI já é nativamente multimodal e supre a análise via engenharia de prompt. |
| Processamento e pagamento de taxas em cartão/PIX | O escopo é estritamente técnico e acadêmico de vistoria, sem transações financeiras. |
| Assinatura digital com certificado ICP-Brasil | Homologação técnica será registrada via login autenticado e registro de CREA no banco de dados. |
| Aplicativo mobile nativo para iOS e Android | A aplicação web será responsiva e acessível diretamente pelo navegador do smartphone. |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here - nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Banco de dados relacional | PostgreSQL em container Docker | Excelente suporte no Spring Data JPA e ideal para deployment na VM gratuita da Oracle. | y |
| Formato de envio de fotos | Multipart/form-data com upload de arquivos JPEG/PNG até 10MB | Padrão robusto para captura direta de câmeras de smartphones. | y |
| Protocolo de comunicação com a IA | Chamada REST síncrona via Spring RestClient | Simplicidade de implementação sem dependências do Spring Cloud. | y |
| Armazenamento em ambiente de desenvolvimento | Armazenamento em diretório de arquivos local com abstração para OCI Object Storage | Facilita desenvolvimento e testes offline antes do deploy na nuvem. | y |

**Open questions:** none - all resolved or logged above (required before the spec is confirmed).

---

## User Stories

### P1: Autenticação e Gestão de Acesso ⭐ MVP

**User Story**: Como usuário (cliente ou engenheiro civil), quero realizar login na plataforma para acessar minhas funcionalidades específicas conforme meu perfil de acesso.

**Why P1**: Requisito fundamental para separar os fluxos do cliente (solicitante) e do engenheiro (avaliador).

**Acceptance Criteria**:

1. WHEN o usuário submeter credenciais válidas na tela de login THEN the system SHALL emitir token de autenticação e redirecionar para o dashboard correspondente ao papel (ROLE_CLIENTE ou ROLE_ENGENHEIRO).
2. IF as credenciais forem inválidas THEN the system SHALL retornar código HTTP 401 Unauthorized com mensagem de erro clara.
3. The system SHALL armazenar as senhas criptografadas usando algoritmo BCrypt.
4. WHILE a sessão estiver autenticada the system SHALL proteger os endpoints administrativos permitindo acesso apenas a usuários com ROLE_ENGENHEIRO.

**Independent Test**: Pode ser testado enviando requisições POST para `/api/auth/login` com credenciais corretas e incorretas e verificando os tokens e códigos HTTP retornados.

---

### P2: Submissão de Vistoria e Geração de Pré-Laudo com IA ⭐ MVP

**User Story**: Como cliente, quero enviar fotos de um cômodo do meu imóvel informando detalhes básicos para que a IA gere um pré-laudo técnico identificando possíveis anomalias.

**Why P2**: Funcionalidade central do produto e entrega principal de valor do sistema.

**Acceptance Criteria**:

1. WHEN o cliente enviar uma imagem válida com identificação do cômodo THEN the system SHALL persistir o registro da vistoria com status EM_PROCESSAMENTO e despachar a imagem para a API do Gemini no OCI.
2. WHEN a IA do OCI responder com a análise técnica THEN the system SHALL salvar o pré-laudo estruturado no banco de dados e atualizar o status da vistoria para PRE_LAUDO_GERADO.
3. IF o arquivo enviado não for uma imagem suportada ou exceder 10MB THEN the system SHALL rejeitar a requisição com código HTTP 400 Bad Request.
4. IF a chamada ao serviço de IA do OCI falhar ou expirar por timeout THEN the system SHALL registrar a falha, manter a vistoria com status ERRO_PROCESSAMENTO e notificar o usuário para reprocessar.

**Independent Test**: Pode ser testado submetendo uma foto via formulário/endpoint `/api/vistorias` e verificando a geração e persistência do registro com parecer da IA no banco.

---

### P3: Homologação Técnica pelo Engenheiro Civil

**User Story**: Como engenheiro civil, quero visualizar as vistorias pendentes, analisar a foto junto com a sugestão da IA e emitir o laudo técnico definitivo para o cliente.

**Why P3**: Garante responsabilidade técnica (Human-in-the-Loop) e conformidade do laudo perante normas de engenharia.

**Acceptance Criteria**:

1. WHEN o engenheiro civil acessar o painel de auditoria THEN the system SHALL listar todas as vistorias com status PRE_LAUDO_GERADO.
2. WHEN o engenheiro homologar o laudo com eventuais correções de texto e seu número de CREA THEN the system SHALL alterar o status da vistoria para CONCLUIDA e marcar o laudo como HOMOLOGADO.
3. IF um usuário sem perfil ROLE_ENGENHEIRO tentar acessar o endpoint de homologação THEN the system SHALL bloquear o acesso com código HTTP 403 Forbidden.

**Independent Test**: Pode ser testado autenticando como engenheiro, requisitando vistorias pendentes e acionando o endpoint `/api/engenheiro/vistorias/{id}/homologar`.

---

## Edge Cases

- IF a imagem enviada estiver escura ou ilegível THEN the system SHALL incluir no parecer técnico a recomendação de reenvio de nova foto com iluminação adequada.
- IF ocorrer perda de conexão com o banco de dados durante a transação de vistoria THEN the system SHALL reverter as operações parciais garantindo consistência relacional.
- WHEN múltiplos clientes enviarem fotos simultaneamente THEN the system SHALL processar cada vistoria de forma isolada e thread-safe.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AUTH-01 | P1: Autenticação e Gestão de Acesso | Design | pending |
| AUTH-02 | P1: Autenticação e Gestão de Acesso | Design | pending |
| VIST-01 | P2: Submissão de Vistoria e Geração de Pré-Laudo com IA | Design | pending |
| VIST-02 | P2: Submissão de Vistoria e Geração de Pré-Laudo com IA | Design | pending |
| IA-01 | P2: Submissão de Vistoria e Geração de Pré-Laudo com IA | Design | pending |
| ENG-01 | P3: Homologação Técnica pelo Engenheiro Civil | Design | pending |
| ENG-02 | P3: Homologação Técnica pelo Engenheiro Civil | Design | pending |

**ID format:** `[CATEGORY]-[NUMBER]`
**Coverage:** 7 total, 7 mapped to user stories, 0 unmapped.

---

## Success Criteria

- [ ] O cliente consegue realizar login, submeter uma foto e visualizar o pré-laudo gerado pela IA em menos de 15 segundos.
- [ ] O engenheiro civil consegue revisar a foto e o parecer da IA, ajustar o texto se necessário e homologar o laudo com sucesso.
- [ ] 100% das regras de negócio cobertas com testes automatizados no backend Spring Boot (JUnit 5 e Mockito).
