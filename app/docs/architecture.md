# Arquitetura do Vistor.IA

## 1. Visão geral

O Vistor.IA combina um frontend Next.js com uma API Java/Spring Boot. O backend segue um monólito modular por feature: autenticação e vistoria compartilham o mesmo processo, mas mantêm domínio, aplicação, persistência e borda HTTP separados.

O banco-alvo é PostgreSQL e o desenvolvimento local pode usar H2. Flyway é a fonte do schema. O armazenamento de imagens é local nesta versão, atrás da porta `StorageService`.

## 2. Contêineres e dependências

No protótipo, o repositório sobe o programa e a IA juntos no mesmo `docker-compose.yml`, em **containers separados**:

| Serviço | Porta | Papel |
| --- | --- | --- |
| `frontend` | 3000 | Next.js |
| `backend` | 8080 | Spring Boot |
| `inference` | 8001 | FastAPI + VLM (análise visual) |

```mermaid
flowchart LR
    Client["Cliente"] --> Web["Next.js 16"]
    Web -->|"REST + JWT"| API["Spring Boot 3.2.3"]
    API --> DB[("PostgreSQL ou H2")]
    API --> Storage["StorageService"]
    Storage --> Files[("Arquivos locais")]
    API --> IaPort["IaIntegrationService"]
    IaPort -->|mock| Mock["MockIaIntegrationService"]
    IaPort -->|vlm| Vlm["VlmIntegrationService"]
    Vlm -->|"VLM_URL rede Docker"| Inference["inference VLM :8001"]
```

`IaIntegrationService` é a porta de análise visual. Com `app.ia.provider=mock` usa `MockIaIntegrationService`; com `vlm` (default no compose) usa `VlmIntegrationService` contra o container `inference` (`VLM_URL=http://inference:8001` + `VLM_API_KEY`). O código da VLM fica em `inference/` neste repositório.

## 3. Fronteiras do backend

```text
br.com.vistoriapredial/
├── usuario/
│   ├── domain/       # Usuário e perfis
│   ├── persistence/  # UsuarioRepository
│   ├── application/  # Cadastro, autenticação e contratos
│   └── web/          # AuthController
├── vistoria/
│   ├── domain/       # Agregado, evidências e estados
│   ├── persistence/  # Repositories JPA
│   ├── application/  # Casos de uso, protocolo e porta de IA
│   └── web/          # VistoriaController e DTOs HTTP
├── storage/          # Porta e adaptador local de arquivos
├── config/security/  # JWT, CORS e autorização por rota
└── shared/web/error/ # Contrato RFC 9457
```

- `web` adapta HTTP e delega para `application`.
- `application` coordena domínio, persistência, armazenamento e pré-análise.
- `domain` concentra estado e transições da vistoria.
- O acesso às evidências combina autorização de rota e autorização por recurso.

## 4. Frontend

O App Router separa autenticação e rotas protegidas. A camada `features/inspections` organiza a jornada pública do MVP e preserva a área antiga de forma isolada:

- cliente: painel, imóvel, captura guiada, análise assíncrona, revisão dos achados e relatório;
- engenharia: fluxo legado mantido para compatibilidade, fora da navegação principal do MVP;
- compartilhado: tipos, status, protocolo e carregamento autenticado de imagens.

O cliente HTTP aceita JSON, `FormData` e blobs, traduz erros RFC 9457 e expira a sessão em respostas `401` autenticadas.

## 5. Fluxo principal do MVP

1. O usuário cria uma conta de cliente e recebe um JWT.
2. O cliente cria uma vistoria em `EM_RASCUNHO` e envia evidências associadas aos 12 itens do protocolo.
3. Ao submeter, o serviço exige ao menos uma evidência, responde `202 Accepted` e publica o processamento assíncrono.
4. A porta de IA analisa as imagens. Sucesso conduz a `REVISAO_PENDENTE`; falha explícita conduz a `FALHA_IA`.
5. O usuário confirma, corrige ou rejeita cada achado e registra o contexto observado. Cada decisão é persistida antes do avanço.
6. Depois de todas as decisões, a conclusão conduz a `RELATORIO_DISPONIVEL` e apresenta o documento rastreável.

O Human-in-the-Loop do MVP é o contexto do próprio usuário sobre a evidência. O relatório não promete homologação de engenheiro, laudo técnico ou diagnóstico estrutural. Falhas de análise permanecem explícitas; o sistema não fabrica sucesso após uma exceção.

## 6. Consistência e segurança

- JWT stateless e perfis negados por padrão na cadeia de segurança.
- Segredo JWT obrigatório com no mínimo 32 bytes e sem fallback versionado.
- O cadastro da interface pública envia apenas o perfil de cliente; recursos legados de engenharia continuam protegidos por papel e convite no backend.
- `ProblemDetail` para validação, autenticação, autorização, ausência e conflito.
- Validação de tamanho, MIME e assinatura antes de persistir imagens.
- Nome físico gerado pelo servidor e caminho mantido fora do contrato HTTP.
- `@Version` na vistoria para detectar decisões concorrentes.
- `Cache-Control: private, no-store` no conteúdo autenticado das evidências.
- Falha ao persistir a evidência aciona a remoção compensatória do arquivo já armazenado.
- `/vistorias/minhas` e `/vistorias/pendentes` são paginadas (`PaginaResponseDto`); a página é buscada sem `@EntityGraph` e as imagens da página são recarregadas em uma segunda consulta por lote de ids, evitando tanto paginação em memória (fetch join de coleção + `Pageable`) quanto N+1.
- `GET /vistorias/{id}` permite buscar uma vistoria específica sem depender da lista paginada, com a mesma autorização por recurso das demais rotas de leitura.

## 7. Persistência

As migrations atuais são:

- `V1__create_initial_schema.sql`: usuários;
- `V2__create_vistoria_schema.sql`: vistorias e imagens;
- `V3__add_endereco_to_vistoria.sql`: endereço do imóvel;
- `V4__add_version_to_vistoria.sql`: versão otimista para decisões concorrentes;
- `V5__add_vistoria_indexes.sql`: índices de acesso a vistorias;
- `V6__add_vistoria_pagination_indexes.sql`: índices das consultas paginadas;
- `V7__add_user_review_to_vistoria.sql`: revisão rastreável dos achados.

Os testes de integração executam as migrations em PostgreSQL real com Testcontainers. O schema não depende de geração automática do Hibernate (`ddl-auto=validate`).
