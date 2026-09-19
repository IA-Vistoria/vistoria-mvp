# Vistoria Predial com IA Multimodal Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/vistoria-predial/design.md`
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `GEMINI.md` (Java 21, Spring Boot 4.1.1, JUnit 5, Mockito).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Service | unit | All business branches; 1:1 to spec ACs; edge cases covered | `src/test/java/**/*ServiceTest.java` | `./mvnw test -Dtest=*ServiceTest` |
| Controller / REST | integration | All routes: happy path + auth + validation + error paths | `src/test/java/**/*ControllerTest.java` | `./mvnw test -Dtest=*ControllerTest` |
| Repository / JPA | integration | Query derivation and constraints validation | `src/test/java/**/*RepositoryTest.java` | `./mvnw test -Dtest=*RepositoryTest` |
| Client OCI / Integration | unit | Mocked RestClient payloads and timeout scenarios | `src/test/java/**/Oci*Test.java` | `./mvnw test -Dtest=*Oci*Test` |
| Entity / DTO / Config | none | Build gate only (compilation and bean initialization) | - | `./mvnw test-compile` |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After tasks with unit tests only | `mvn test -Dtest=*ServiceTest` |
| Full | After tasks with integration tests | `mvn test` |
| Build | After phase completion or config/entity-only tasks | `mvn clean test-compile` |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Fundacao e Configuracao
Tarefas iniciais de setup do projeto, profiles e serviço de arquivos.

```
T1 -> T2 -> T3
```

### Phase 2: Modulo Usuario e Autenticacao
Entidades, regras de negócio de usuários, JWT e endpoints de autenticação.

```
T4 -> T5 -> T6 -> T7
```

### Phase 3: Modulo Vistoria e Integracao IA
Entidades de vistoria e laudo, cliente OCI Gemini, serviços de negócio e endpoints.

```
T8 -> T9 -> T10 -> T11 -> T12
```

### Phase 4: Frontend Vanilla Integrado
Páginas HTML5 semânticas, scripts JavaScript nativos de consumo da API e estilos.

```
T13 -> T14 -> T15 -> T16
```

---

## Task Breakdown

### Phase 1: Fundacao e Configuracao

#### T1: Criar configuracao inicial do Maven pom.xml com Java 21 e Spring Boot 4.1.1
**What**: Criar arquivo `pom.xml` com dependências `web`, `data-jpa`, `validation`, `security`, `h2`, `postgresql` e plugins de build Java 21.
**Where**: `pom.xml`
**Depends on**: None
**Tests**: Build gate only
**Gate**: Build
**Requirement**: AUTH-01

**Done when**:
- [x] `pom.xml` compila sem erros de sintaxe
- [x] Versões alinhadas com Java 21 e Spring Boot 4.1.1

#### T2: Configurar application.properties para ambientes local e producao
**What**: Configurar datasource H2 em memória para testes/dev e suporte a PostgreSQL via variáveis de ambiente.
**Where**: `src/main/resources/application.properties`
**Depends on**: T1
**Tests**: Build gate only
**Gate**: Build
**Requirement**: AUTH-01

**Done when**:
- [x] Configuração do H2 e JPA `ddl-auto` definidas
- [x] Limite de upload configurado para 10MB

#### T3: Implementar StorageService para persistencia de imagens de vistoria
**What**: Criar interface `StorageService` e implementação `LocalStorageService` que grava imagens em diretório local.
**Where**: `src/main/java/br/com/vistoriapredial/storage/LocalStorageService.java`
**Depends on**: T2
**Tests**: `src/test/java/br/com/vistoriapredial/storage/LocalStorageServiceTest.java`
**Gate**: Quick
**Requirement**: VIST-01

**Done when**:
- [x] Salva arquivo binário e retorna caminho relativo acessível
- [x] Teste unitário verifica gravação correta de bytes

### Phase 2: Modulo Usuario e Autenticacao

#### T4: Implementar entidade Usuario e UsuarioRepository
**What**: Criar classe JPA `Usuario` com campos `id`, `nome`, `email`, `senha`, `perfil`, `crea` e seu repository.
**Where**: `src/main/java/com/vistoriapredial/domain/Usuario.java`
**Depends on**: T3
**Tests**: Build gate only
**Gate**: Build
**Requirement**: AUTH-01

**Done when**:
- [ ] Entidade mapeada com anotações JPA corretas
- [ ] Repository estende `JpaRepository<Usuario, Long>` com busca por email

#### T5: Implementar UsuarioService com codificacao de senha BCrypt
**What**: Implementar regras de cadastro e consulta de usuário utilizando `PasswordEncoder`.
**Where**: `src/main/java/com/vistoriapredial/service/UsuarioService.java`
**Depends on**: T4
**Tests**: `src/test/java/com/vistoriapredial/service/UsuarioServiceTest.java`
**Gate**: Quick
**Requirement**: AUTH-01

**Done when**:
- [ ] Senhas salvas com hash seguro
- [ ] Teste unitário com Mockito cobrindo sucesso e email duplicado

#### T6: Configurar SecurityConfig e geracao de tokens JWT
**What**: Configurar filtro de segurança stateless no Spring Security protegendo rotas por perfil.
**Where**: `src/main/java/com/vistoriapredial/config/SecurityConfig.java`
**Depends on**: T5
**Tests**: `src/test/java/com/vistoriapredial/config/SecurityConfigTest.java`
**Gate**: Full
**Requirement**: AUTH-01

**Done when**:
- [ ] Rotas públicas liberadas (`/api/auth/**`, frontend)
- [ ] Rotas `/api/engenheiro/**` restritas para `ROLE_ENGENHEIRO`

#### T7: Implementar AuthController com endpoints de registro e login
**What**: Expor endpoints REST `POST /api/auth/register` e `POST /api/auth/login`.
**Where**: `src/main/java/com/vistoriapredial/controller/AuthController.java`
**Depends on**: T6
**Tests**: `src/test/java/com/vistoriapredial/controller/AuthControllerTest.java`
**Gate**: Full
**Requirement**: AUTH-01

**Done when**:
- [ ] Retorna 200 com token no login válido
- [ ] Retorna 401 com mensagem clara em credenciais incorretas

### Phase 3: Modulo Vistoria e Integracao IA

#### T8: Implementar entidades Vistoria e Laudo com seus repositorios
**What**: Criar entidades JPA `Vistoria` e `Laudo` com seus respectivos relacionamentos e repositories.
**Where**: `src/main/java/com/vistoriapredial/domain/Vistoria.java`
**Depends on**: T7
**Tests**: Build gate only
**Gate**: Build
**Requirement**: VIST-01

**Done when**:
- [ ] Relacionamento `@OneToOne` entre `Vistoria` e `Laudo` mapeado
- [ ] Repositories criados e compilando

#### T9: Implementar OciGenerativeAiClient utilizando Spring RestClient
**What**: Criar cliente HTTP para envio de imagem e prompt multimodal para a API do Gemini.
**Where**: `src/main/java/com/vistoriapredial/integration/oci/OciGenerativeAiClient.java`
**Depends on**: T8
**Tests**: `src/test/java/com/vistoriapredial/integration/oci/OciGenerativeAiClientTest.java`
**Gate**: Quick
**Requirement**: IA-01

**Done when**:
- [ ] Monta payload multimodal com imagem em base64 e prompt de engenharia
- [ ] Teste unitário valida serialização e tratamento de timeout

#### T10: Implementar VistoriaService orquestrando upload e geracao de pre-laudo
**What**: Criar serviço que recebe upload, salva imagem, chama cliente de IA e persiste vistoria e laudo.
**Where**: `src/main/java/com/vistoriapredial/service/VistoriaService.java`
**Depends on**: T9
**Tests**: `src/test/java/com/vistoriapredial/service/VistoriaServiceTest.java`
**Gate**: Quick
**Requirement**: VIST-01

**Done when**:
- [ ] Cria vistoria com status `PRE_LAUDO_GERADO` ao receber resposta da IA
- [ ] Teste unitário cobre fluxo feliz e fallback em erro da IA

#### T11: Implementar VistoriaController para criacao e listagem de vistorias
**What**: Expor endpoints REST `POST /api/vistorias` (multipart) e `GET /api/vistorias`.
**Where**: `src/main/java/com/vistoriapredial/controller/VistoriaController.java`
**Depends on**: T10
**Tests**: `src/test/java/com/vistoriapredial/controller/VistoriaControllerTest.java`
**Gate**: Full
**Requirement**: VIST-01

**Done when**:
- [ ] Aceita upload de fotos até 10MB
- [ ] Retorna status 201 com dados do laudo prévio gerado

#### T12: Implementar LaudoService e EngenheiroController para homologacao
**What**: Expor endpoints de listagem de pendências e homologação do laudo técnico com número de CREA.
**Where**: `src/main/java/com/vistoriapredial/controller/EngenheiroController.java`
**Depends on**: T11
**Tests**: `src/test/java/com/vistoriapredial/controller/EngenheiroControllerTest.java`
**Gate**: Full
**Requirement**: ENG-01

**Done when**:
- [ ] Apenas engenheiros conseguem homologar laudo
- [ ] Status da vistoria atualiza para `CONCLUIDA` e laudo para `HOMOLOGADO`

### Phase 4: Frontend Vanilla Integrado

#### T13: Implementar interface web de Login e Cadastro com JavaScript nativo
**What**: Criar tela de login com formulário limpo e script de autenticação via fetch.
**Where**: `src/main/resources/static/login.html`
**Depends on**: T12
**Tests**: Build gate only
**Gate**: Build
**Requirement**: AUTH-01

**Done when**:
- [ ] Formulário envia credenciais e armazena token no `localStorage`
- [ ] Redireciona para dashboard de cliente ou engenheiro conforme perfil

#### T14: Implementar dashboard do Cliente para envio de foto e acompanhamento
**What**: Criar tela de cliente com input de foto de câmera/arquivo, seleção de cômodo e cards de histórico.
**Where**: `src/main/resources/static/cliente.html`
**Depends on**: T13
**Tests**: Build gate only
**Gate**: Build
**Requirement**: VIST-01

**Done when**:
- [ ] Submete foto via `FormData` e exibe resultado da IA na tela
- [ ] Lista vistorias anteriores com badge de status

#### T15: Implementar painel de auditoria e homologacao do Engenheiro Civil
**What**: Criar painel do engenheiro exibindo fotos, diagnóstico da IA e formulário de homologação.
**Where**: `src/main/resources/static/engenheiro.html`
**Depends on**: T14
**Tests**: Build gate only
**Gate**: Build
**Requirement**: ENG-01

**Done when**:
- [ ] Lista vistorias pendentes de homologação
- [ ] Permite aprovar ou retificar texto com inserção de CREA

#### T16: Implementar folha de estilos CSS responsiva e moderna
**What**: Criar folha de estilo CSS unificada com design profissional, responsivo para desktop e smartphone.
**Where**: `src/main/resources/static/css/style.css`
**Depends on**: T15
**Tests**: Build gate only
**Gate**: Build
**Requirement**: VIST-01

**Done when**:
- [ ] Layout adaptável a celulares e computadores
- [ ] Cores, tipografia e badges de status consistentes
