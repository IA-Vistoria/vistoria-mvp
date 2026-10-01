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
| `inference` | 8001 | FastAPI + VLM legado, ativado apenas pelo profile Docker `vlm` |

```mermaid
flowchart LR
    Client["Cliente"] --> Web["Next.js 16"]
    Web -->|"REST + JWT"| API["Spring Boot 3.2.3"]
    API --> DB[("PostgreSQL ou H2")]
    API --> Storage["StorageService"]
    Storage --> Files[("Arquivos locais")]
    API --> IaPort["IaIntegrationService"]
    IaPort -->|oci| Oci["OciGenAiIntegrationService"]
    Oci -->|"SDK oficial + Gemini configurável"| GenAi["OCI Generative AI"]
    IaPort -->|mock test/demo| Mock["MockIaIntegrationService"]
    IaPort -->|vlm legado opt-in| Vlm["VlmIntegrationService"]
    Vlm -->|"VLM_URL rede Docker"| Inference["inference VLM :8001"]
```

`IaIntegrationService` é a porta de análise visual. `oci` é o provider padrão e
usa o SDK oficial da OCI com autenticação `config_file` ou
`instance_principal`. `mock` só existe nos profiles `test` e `demo` e emite o
mesmo documento v2 com cenários explicitamente simulados. `vlm` permanece como
adaptação legada opt-in contra o container `inference`.

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
2. O cliente cria uma vistoria em `EM_RASCUNHO`, define um roteiro flexível e envia uma visão geral por ambiente, com detalhes opcionais.
3. Ao submeter, o serviço exige ao menos uma evidência, responde `202 Accepted` e publica o processamento assíncrono.
4. A porta de IA analisa as imagens. Um documento válido conduz a `RELATORIO_DISPONIVEL`; falha explícita conduz a `FALHA_IA`.
5. O sistema deriva uma conclusão automatizada por ambiente e para o imóvel, preservando motivo, severidade, confiança, limitações e rastreabilidade.
6. O usuário pode concordar, contestar ou acrescentar contexto; a manifestação é persistida separadamente e não altera o achado nem a conclusão da IA.
7. O relatório fica disponível assim que a análise válida é persistida; a manifestação é opcional.

O Human-in-the-Loop do MVP é uma manifestação sobre a evidência, não uma
aprovação necessária nem uma forma de editar a conclusão. O relatório não
promete homologação de engenheiro, laudo técnico, diagnóstico estrutural ou
validade jurídica automática. Falhas de análise permanecem explícitas; o
sistema não fabrica sucesso, documento parcial ou fallback após uma exceção.

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
- `GET /api/health/ia` exige autenticação e expõe somente o estado seguro da configuração OCI; não realiza chamada ao modelo nem informa compartment, caminho de chave ou segredo.

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
