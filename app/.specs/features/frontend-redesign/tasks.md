# Experiência Vistor.IA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar as interfaces de cliente e engenheiro Vistor.IA conectadas à API Spring Boot real, com evidências autenticadas, estados confiáveis e validação integrada.

**Architecture:** O backend Spring Boot permanece como autoridade de autenticação, papéis, estados e arquivos. O frontend Next.js usa organização por feature, um cliente REST tipado e Client Components apenas onde sessão, formulário, upload ou blob autenticado exigirem APIs do navegador.

**Tech Stack:** Java 21, Spring Boot 3.2.3, Spring MVC/Security/Data JPA, Flyway, PostgreSQL/Testcontainers, Next.js 16.3.5, React 19.2.8, TypeScript 5, Tailwind CSS 4, Vitest e React Testing Library.

**Spec:** `.specs/features/frontend-redesign/spec.md` e `.specs/features/frontend-redesign/design.md`

## Global Constraints

- A marca visível e a chave nova de sessão usam exatamente `Vistor.IA` e `vistoria.session`; `MedFlow` não aparece na interface ou metadata.
- A paleta usa concreto quente `#F3EFE7`, calcário `#DDD4C6`, grafite `#252A2E`, azul de projeto `#17324D`, terracota `#A95736`, verde estrutural `#2F6657` e âmbar `#C8912F`, sem gradientes.
- O frontend não inventa severidade, confiança, diagnóstico, cliente, data ou evidência ausente do contrato da API.
- A API Spring Boot real configurada por `NEXT_PUBLIC_API_URL` é a única fonte de dados do navegador; não há fallback mock no frontend.
- JWT, senha, conteúdo integral de evidência e parecer não podem ser registrados em logs.
- O token permanece em `localStorage` nesta entrega; cookies HttpOnly, refresh token, PDF, assinatura digital e provedor real de IA ficam fora do escopo.
- Todo comportamento novo ou alterado recebe teste derivado dos critérios de aceitação, com RED observado antes da produção e GREEN antes do commit.
- Um commit Conventional Commit em português por tarefa; somente arquivos da tarefa entram no stage.
- A migração V2 só pode ser alterada depois da confirmação de que não foi aplicada em banco persistente/compartilhado; o uso em H2 efêmero de teste será registrado.
- A instalação de dependências de teste do frontend exige confirmação operacional imediatamente antes de `npm install`.
- As mudanças locais preexistentes diretamente relacionadas a endereço, listagem do cliente, CORS e scaffold Next.js só entram nos commits após autorização explícita para incorporá-las; mudanças alheias em `AGENTS.md`, `.specs/STATE.md` e outras features permanecem fora.
- O push ocorre somente após backend, frontend, integração, UAT e validação TLC receberem PASS.

## Review Focus

1. Arquivo com MIME permitido e bytes incompatíveis deve ser rejeitado pelo backend; T3 fixa o caso com assinatura PNG/JPEG/WebP inválida.
2. URL de evidência adulterada com `imagemId` de outra vistoria deve responder 404 sem revelar se a imagem existe; T4 cobre o par vistoria/evidência.
3. Sessão JSON corrompida ou com papel desconhecido deve ser limpa e redirecionada sem disparar fetch protegido; T5 e T6 cobrem o caso.
4. Duplo clique em criar, subir, submeter ou analisar deve produzir uma única mutação concorrente; T7, T8 e T9 cobrem botões ocupados.
5. Foto autenticada que falha depois da tela carregar deve liberar o `objectURL` anterior e manter o restante do caso utilizável; T9 cobre erro e cleanup.

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Use `superpowers:test-driven-development` for every behavior and `product-design:image-to-code` for the visual tasks T6-T9. Use `superpowers:systematic-debugging` whenever a gate fails and `superpowers:verification-before-completion` before T10 can receive PASS.

If any required skill cannot be activated, stop before changing production code and report the blocker.

---

**Design**: `.specs/features/frontend-redesign/design.md`
**Status**: In Progress

---

## Test Coverage Matrix

> Generated from `AGENTS.md`, `pom.xml`, `frontend/package.json`, the Next.js 16 local Vitest guide, 13 backend test files and the approved spec. Existing tests define style and location; `AGENTS.md` and the spec define the stronger coverage target.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Build tooling / wrapper | smoke | Wrapper inicia Maven e executa a suíte pelo comando canônico | `mvnw.cmd` | `.\mvnw.cmd test` |
| Flyway / PostgreSQL | integration | Todas as migrations executam em PostgreSQL vazio e Hibernate valida o schema | `src/test/java/**/*IntegrationTest.java` | `.\mvnw.cmd "-Dtest=PostgreSqlMigrationIntegrationTest" test` |
| Domínio e application backend | unit | Todos os ramos, invariantes, papéis, estados e edge cases da spec | `src/test/java/**/application/*Test.java` | `.\mvnw.cmd "-Dtest=VistoriaServiceTest,EvidenceFileValidatorTest" test` |
| Storage backend | unit | Gravação, leitura, tipo, ausência, path traversal e arquivo inválido com `@TempDir` | `src/test/java/**/storage/*Test.java` | `.\mvnw.cmd "-Dtest=LocalStorageServiceTest" test` |
| Web / segurança backend | integração MockMvc | Toda rota alterada: sucesso, validação, 401/403/404/409 e contrato RFC 9457 | `src/test/java/**/web/*Test.java` | `.\mvnw.cmd "-Dtest=VistoriaControllerTest" test` |
| Utilitários TypeScript | unit | JSON, FormData, ProblemDetail, rede, 401, sessão válida/corrompida e ordenação | `frontend/src/**/*.test.ts` | `npm run test -- src/lib src/features/inspections` |
| Componentes React | component | Render, teclado, estados busy/empty/error/success e interações de cada AC | `frontend/src/**/*.test.tsx` | `npm run test -- src/features` |
| Jornadas integradas | UAT em navegador | Cadastro/login dos dois papéis; rascunho/upload/submissão; fila/revisão/decisão; 375/768/1440 px | `.specs/features/frontend-redesign/validation.md` | Backend + frontend locais e navegador controlado |
| CSS, metadata e config | build/lint | Sem erro de TypeScript/ESLint/build, metadata correta e sem overflow visual | `frontend/src/app/`, `frontend/next.config.ts` | `npm run lint` e `npm run build` |

## Gate Check Commands

> Generated from the repository. Backend commands must run outside the restricted sandbox because the sandbox blocks `javac` from resolving directory classpaths. PostgreSQL gates additionally require Docker ativo.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Backend quick | T3 e T4 durante RED/GREEN | `.\mvnw.cmd "-Dtest=VistoriaServiceTest,EvidenceFileValidatorTest,LocalStorageServiceTest,VistoriaControllerTest" test` |
| Backend full | Final de cada tarefa backend | `.\mvnw.cmd test` |
| PostgreSQL | T2 e T10 | `.\mvnw.cmd "-Dtest=PostgreSqlMigrationIntegrationTest" test` |
| Frontend focused | T5-T9 durante RED/GREEN | `npm run test -- src/lib` ou `npm run test -- src/features` conforme a camada da tarefa |
| Frontend full | Final de cada tarefa frontend | `npm run test`, depois `npm run lint`, depois `npm run build` |
| Integrated | T10 | Backend full + PostgreSQL + frontend full + UAT real em navegador |
| TLC completion | Depois do relatório PASS | `python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\validate_state.py frontend-redesign --root .` |

## Requirement Traceability

| Spec area | Tasks |
| --- | --- |
| Identidade, cadastro e acesso por perfil | T5, T6, T10 |
| Jornada guiada do cliente | T3, T7, T8, T10 |
| Fila e revisão do engenheiro | T4, T9, T10 |
| Contrato autenticado de evidências | T3, T4, T9, T10 |
| Sistema visual responsivo e acessível | T6, T7, T8, T9, T10 |
| Cliente HTTP, estados e falhas | T5, T7, T8, T9, T10 |
| Correção e execução verificável do backend | T1, T2, T3, T4, T10 |

---

## Execution Plan

Phases are ordered and run sequentially. Tasks within each phase also run sequentially.

### Phase 1: Backend verificável

```text
T1 -> T2 -> T3 -> T4
```

### Phase 2: Fundação visual e autenticação

```text
T5 -> T6
```

### Phase 3: Jornadas de produto

```text
T7 -> T8 -> T9
```

### Phase 4: Integração e aceite

```text
T10
```

---

## Task Breakdown

### Phase 1: Backend verificável

## Task 1

### T1: Restabelecer o Maven Wrapper canônico

**Status**: Complete

**What**: Corrigir a detecção do diretório `.m2` no script PowerShell embutido para que `mvnw.cmd` inicie o Maven 3.9.16 em diretórios normais e em links simbólicos.
**Where**: `mvnw.cmd`
**Depends on**: None
**Reuses**: `.mvn/wrapper/maven-wrapper.properties`
**Requirement**: Definição de pronto do projeto e gate backend reproduzível.

**Tools**:

- MCP: terminal local
- Skills: `tlc-spec-driven`, `superpowers:systematic-debugging`

- [x] **Step 1: Reproduzir o RED do wrapper**

```powershell
.\mvnw.cmd -version
```

Expected: FAIL atual com `Não é possível indexar em uma matriz nula` na expressão `(Get-Item $MAVEN_M2_PATH).Target[0]`.

- [x] **Step 2: Aplicar a correção mínima**

Substituir a indexação direta por uma decisão nula-segura:

```powershell
$MAVEN_M2_ITEM = Get-Item $MAVEN_M2_PATH
$MAVEN_M2_TARGET = @($MAVEN_M2_ITEM.Target) | Select-Object -First 1
if (-not $MAVEN_M2_TARGET) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_TARGET/wrapper/dists"
}
```

- [x] **Step 3: Provar o GREEN do comando canônico**

```powershell
.\mvnw.cmd -version
.\mvnw.cmd test
```

Expected: Maven 3.9.16, Java 21 e os 39 testes da linha de base passando; o total não pode diminuir.

- [x] **Step 4: Atualizar rastreabilidade e commitar somente o wrapper**

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "fix(build): corrige inicialização do Maven Wrapper"
git add mvnw.cmd .specs/features/frontend-redesign/tasks.md
git commit -m "fix(build): corrige inicialização do Maven Wrapper"
```

**Done when**:

- [x] `mvnw.cmd -version` e `mvnw.cmd test` passam pelo wrapper.
- [x] A suíte mantém no mínimo 39 testes backend.
- [x] T1 está marcado antes do commit e nenhum arquivo alheio está staged.

**Tests**: smoke + suíte backend
**Gate**: Backend full
**Commit**: `fix(build): corrige inicialização do Maven Wrapper`

## Task 2

### T2: Provar migrations no PostgreSQL alvo

**Status**: Complete

**What**: Criar um teste Testcontainers que execute Flyway em PostgreSQL vazio e substituir a sintaxe H2-only da V2 somente após confirmar que ela não foi aplicada em ambiente persistente/compartilhado.
**Where**: `src/main/resources/db/migration/V2__create_vistoria_schema.sql`
**Depends on**: T1
**Reuses**: dependências Testcontainers PostgreSQL e Flyway já presentes no `pom.xml`.
**Requirement**: Banco-alvo validado e backend correto antes do aceite.

**Tools**:

- MCP: terminal local com Docker
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

- [x] **Step 1: Obter a confirmação operacional da migration**

Registrar que V2 só foi aplicada em H2 efêmero ou, se existir ambiente persistente, interromper a tarefa e desenhar estratégia de baseline/repair sem alterar o checksum já aplicado.

- [x] **Step 2: Escrever o teste de integração RED**

Criar `PostgreSqlMigrationIntegrationTest` com o contrato:

```java
@Testcontainers
class PostgreSqlMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void shouldApplyAllMigrationsOnEmptyPostgreSqlDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.targetSchemaVersion).isEqualTo("3");
    }
}
```

- [x] **Step 3: Executar RED com Docker ativo**

```powershell
.\mvnw.cmd "-Dtest=PostgreSqlMigrationIntegrationTest" test
```

Expected: FAIL na V2 com erro de sintaxe próximo de `AUTO_INCREMENT`.

- [x] **Step 4: Tornar a geração de IDs portável sem nova migration**

Após a confirmação do Step 1, substituir nas duas tabelas da V2:

```sql
id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY
```

- [x] **Step 5: Executar PostgreSQL e suíte completa**

```powershell
.\mvnw.cmd "-Dtest=PostgreSqlMigrationIntegrationTest" test
.\mvnw.cmd test
```

Expected: 1 teste PostgreSQL novo e pelo menos 40 testes backend no total, todos verdes.

- [x] **Step 6: Atualizar rastreabilidade e commitar**

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "fix(migration): compatibiliza schema de vistoria com PostgreSQL"
git add src/main/resources/db/migration/V2__create_vistoria_schema.sql src/test/java/br/com/vistoriapredial/PostgreSqlMigrationIntegrationTest.java .specs/features/frontend-redesign/tasks.md
git commit -m "fix(migration): compatibiliza schema de vistoria com PostgreSQL"
```

**Done when**:

- [x] Confirmação sobre histórico persistente está registrada.
- [x] Flyway aplica V1-V3 em PostgreSQL 16 vazio.
- [x] Hibernate/Flyway e a suíte completa permanecem verdes.
- [x] T2 está marcado antes do commit.

**Tests**: integração PostgreSQL
**Gate**: PostgreSQL + Backend full
**Commit**: `fix(migration): compatibiliza schema de vistoria com PostgreSQL`

## Task 3

### T3: Endurecer upload e submissão de evidências

**Status**: Complete

**What**: Consolidar o contrato backend já iniciado para endereço/lista do cliente e completar validação de protocolo, tamanho, MIME e assinatura, nome seguro, resposta do upload, evidência obrigatória e URLs reais para a IA.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/application/`
**Depends on**: T2
**Reuses**: mudanças locais verificadas de endereço/listagem, `StorageService`, `VistoriaService`, multipart de 10 MB e estados existentes.
**Requirement**: Jornada do cliente AC2-AC9 e contrato de evidências AC6.

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

- [x] **Step 1: Escrever os testes RED do validador e serviço**

O teste central de submissão deve ter este conteúdo:

```java
@Test
void shouldRejectSubmissionWithoutPersistedEvidence() {
    Vistoria vistoria = new Vistoria();
    vistoria.setId(10L);
    vistoria.setCliente(cliente);
    vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
    when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

    assertThatThrownBy(() -> vistoriaService.submeterVistoria(10L, cliente))
            .isInstanceOf(InvalidEvidenceException.class)
            .hasMessageContaining("ao menos uma evidência");

    verify(iaIntegrationService, never()).analisarImagens(anyList());
}
```

No mesmo ciclo, criar casos parametrizados que: rejeitam `SALA`, `TELHADO` e string vazia; rejeitam `image/png` com bytes de texto; rejeitam `10 * 1024 * 1024 + 1` bytes; aceitam as três assinaturas permitidas; comprovam que `analisarImagens` recebe exatamente `List.of("uploads/a.jpg", "uploads/b.webp")`.

Atualizar o teste MockMvc de upload para esperar `200` com `$.imagens[0].protocoloItem` em vez de corpo vazio.

- [x] **Step 2: Executar RED focado**

```powershell
.\mvnw.cmd "-Dtest=VistoriaServiceTest,EvidenceFileValidatorTest,VistoriaControllerTest" test
```

Expected: FAIL por classes/regras ainda inexistentes e contrato `Void` do upload.

- [x] **Step 3: Implementar protocolo e validação binária**

```java
public final class ProtocoloVistoria {
    public static final Set<String> ITEMS = Set.of(
            "SALA_PISO", "SALA_PAREDES_REVESTIMENTOS", "SALA_TETO_ILUMINACAO",
            "COZINHA_PISO", "COZINHA_PAREDES_BANCADAS", "COZINHA_INSTALACOES",
            "BANHEIRO_REVESTIMENTOS", "BANHEIRO_HIDRAULICA",
            "QUARTO_PISO", "QUARTO_PAREDES_TETO",
            "INSTALACOES_ELETRICAS", "INSTALACOES_HIDRAULICAS");
}

public record ValidatedEvidence(String extension, MediaType mediaType) {}
```

`EvidenceFileValidator.validate` deve ler cabeçalhos JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`) e WebP (`RIFF....WEBP`), rejeitar arquivo vazio/maior que 10 MiB e devolver extensão controlada.

- [x] **Step 4: Corrigir serviço e contrato do upload**

```java
String fileName = vistoriaId + "_" + UUID.randomUUID() + validated.extension();
// armazenar, associar e retornar a própria vistoria atualizada

if (vistoria.getImagens().isEmpty()) {
    throw new InvalidEvidenceException("Adicione ao menos uma evidência antes de enviar a vistoria.");
}

List<String> urls = vistoria.getImagens().stream()
        .map(ImagemVistoria::getUrl)
        .toList();
String preLaudo = iaIntegrationService.analisarImagens(urls);
```

- [x] **Step 5: Executar gate focado e completo**

Expected: pelo menos 10 casos novos e no mínimo 50 testes backend totais, todos verdes.

- [x] **Step 6: Atualizar rastreabilidade e commitar**

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(vistoria): consolida jornada de evidências do cliente"
git add src/main/java/br/com/vistoriapredial/vistoria src/test/java/br/com/vistoriapredial/vistoria src/main/resources/db/migration/V3__add_endereco_to_vistoria.sql .specs/features/frontend-redesign/tasks.md
git commit -m "feat(vistoria): consolida jornada de evidências do cliente"
```

**Done when**:

- [x] Os 12 códigos são a única allowlist aceita.
- [x] Nome original nunca compõe o destino.
- [x] Arquivos inválidos recebem ProblemDetail seguro e imagens válidas anteriores permanecem.
- [x] Submissão sem evidência falha; IA recebe URLs persistidas.
- [x] Gate focado e backend full passam com contagem não regressiva.

**Tests**: unit + integração MockMvc
**Gate**: Backend quick + Backend full
**Commit**: `feat(vistoria): consolida jornada de evidências do cliente`

## Task 4

### T4: Expor evidências autenticadas sem caminho físico

**What**: Incluir metadados de imagem no DTO, carregar coleções dentro da transação e criar a rota de conteúdo com autorização por ownership/papel/estado.
**Where**: `src/main/java/br/com/vistoriapredial/vistoria/web/`
**Depends on**: T3
**Reuses**: `StorageService`, `GlobalExceptionHandler`, `@PreAuthorize` e entidade `ImagemVistoria`.
**Requirement**: Contrato autenticado de evidências AC1-AC5 e fila do engenheiro AC2/AC10.

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

- [ ] **Step 1: Escrever RED de storage, serviço e HTTP**

```java
@Test
@WithMockUser(username = "client@test.com", roles = "CLIENTE")
void shouldReturnAuthenticatedEvidenceContent() throws Exception {
    ByteArrayResource resource = new ByteArrayResource(new byte[] {1, 2, 3});
    when(vistoriaService.buscarEvidencia(10L, 20L, cliente))
            .thenReturn(new EvidenceContent(resource, MediaType.IMAGE_JPEG, 3));

    mockMvc.perform(get("/api/vistorias/10/imagens/20/conteudo"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(content().bytes(new byte[] {1, 2, 3}));
}
```

No mesmo ciclo, criar casos completos para: leitura `@TempDir` com tipo/tamanho controlados; owner permitido; engenheiro permitido somente em `AGUARDANDO_ENGENHEIRO`; outro cliente 403; par vistoria/imagem adulterado 404; arquivo ausente 404; JSON contendo apenas `id`, `protocoloItem`, `dataUpload`, `conteudoUrl`.

- [ ] **Step 2: Executar RED focado**

Expected: FAIL por `StorageService.load`, DTO e endpoint inexistentes.

- [ ] **Step 3: Implementar a porta e o conteúdo autorizado**

```java
public record StoredFile(Resource resource, MediaType mediaType, long length) {}
public record EvidenceContent(Resource resource, MediaType mediaType, long length) {}

public interface StorageService {
    String store(MultipartFile file, String fileName);
    StoredFile load(String relativePath);
    void delete(String relativePath);
}
```

`LocalStorageService.load` deve normalizar, garantir `startsWith(uploadDir)`, responder ausência sem caminho interno e inferir o tipo somente das extensões geradas `.jpg`, `.png`, `.webp`.

- [ ] **Step 4: Implementar DTO, fetch graph e endpoint**

```java
public record ImagemVistoriaResponseDto(
        Long id, String protocoloItem, LocalDateTime dataUpload, String conteudoUrl) {
    static ImagemVistoriaResponseDto from(Long vistoriaId, ImagemVistoria imagem) {
        return new ImagemVistoriaResponseDto(
                imagem.getId(), imagem.getProtocoloItem(), imagem.getDataUpload(),
                "/api/vistorias/" + vistoriaId + "/imagens/" + imagem.getId() + "/conteudo");
    }
}
```

O controller deve responder `ResponseEntity<Resource>` com `Content-Type`, `Content-Length` e `Cache-Control: private, no-store`.

- [ ] **Step 5: Mapear exceções tipadas**

Mapear recurso ausente para 404, acesso de outro cliente para 403, transição obsoleta para 409 e evidência inválida para 422, todos em `application/problem+json`.

- [ ] **Step 6: Executar gates e commitar**

Expected: pelo menos 8 casos novos e no mínimo 58 testes backend totais.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(vistoria): disponibiliza evidências autenticadas"
git add src/main/java/br/com/vistoriapredial/storage src/main/java/br/com/vistoriapredial/vistoria src/main/java/br/com/vistoriapredial/shared/web/error src/test/java/br/com/vistoriapredial .specs/features/frontend-redesign/tasks.md
git commit -m "feat(vistoria): disponibiliza evidências autenticadas"
```

**Done when**:

- [ ] Response nunca contém `ImagemVistoria.url` ou caminho físico.
- [ ] Owner e engenheiro pendente acessam; outro cliente recebe 403; par adulterado/arquivo ausente recebe 404.
- [ ] Listas não geram `LazyInitializationException` com `open-in-view=false`.
- [ ] Backend quick e full passam com contagem não regressiva.

**Tests**: unit + repository integration + MockMvc integration
**Gate**: Backend quick + Backend full
**Commit**: `feat(vistoria): disponibiliza evidências autenticadas`

### Phase 2: Fundação visual e autenticação

## Task 5

### T5: Criar fundação testável de sessão e HTTP

**What**: Instalar o runner aprovado, definir contratos TypeScript, persistência segura da sessão, cliente HTTP único e CORS Spring configurável para JSON, multipart, blob, ProblemDetail e 401.
**Where**: `frontend/src/lib/` e configuração CORS Spring
**Depends on**: T4
**Reuses**: `NEXT_PUBLIC_API_URL`, `fetch`, `AuthResponseDto` e DTOs backend de T4.
**Requirement**: Cliente HTTP AC1-AC9 e identidade AC2/AC7/AC8.

**Tools**:

- MCP: terminal e editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`

- [ ] **Step 1: Obter confirmação e instalar somente dependências de teste**

```powershell
npm install --save-dev vitest @vitejs/plugin-react jsdom @testing-library/react @testing-library/dom @testing-library/user-event vite-tsconfig-paths
```

Configurar `vitest.config.mts`, `src/test/setup.ts`, `test: vitest run` e `test:watch: vitest`.

- [ ] **Step 2: Escrever ao menos 8 testes RED**

```typescript
it("does not set JSON content type for FormData", async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 1 }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  }))
  vi.stubGlobal("fetch", fetchMock)
  const body = new FormData()
  body.append("file", new File(["image"], "room.jpg", { type: "image/jpeg" }))

  await apiFetch("/vistorias/1/imagens", { method: "POST", body })

  const request = fetchMock.mock.calls[0][1] as RequestInit
  expect(new Headers(request.headers).has("Content-Type")).toBe(false)
})
```

Completar o arquivo com sete testes de mesmo nível: preservar todos os campos RFC 9457 em 422; mensagem segura em 502 não JSON; limpar sessão e emitir evento em 401 autenticado; não redirecionar 401 de login com `auth: false`; devolver blob com Bearer; remover JSON de sessão corrompido; remover sessão com `ROLE_ADMIN`.

Adicionar um teste MockMvc de preflight que envia `Origin: http://localhost:3000` e `Access-Control-Request-Method: POST`, esperando `Access-Control-Allow-Origin: http://localhost:3000`; uma origem não listada não recebe o header.

- [ ] **Step 3: Executar RED**

```powershell
npm run test -- src/lib
```

Expected: FAIL por configuração, `ApiError`, `AuthSession` e helpers inexistentes.

- [ ] **Step 4: Implementar contratos estáveis**

```typescript
export type UserRole = "ROLE_CLIENTE" | "ROLE_ENGENHEIRO"
export const SESSION_KEY = "vistoria.session"
export const SESSION_EXPIRED_EVENT = "vistoria:session-expired"

export interface ApiRequestInit extends RequestInit {
  auth?: boolean
}

export class ApiError extends Error {
  constructor(public readonly problem: ProblemDetails) {
    super(problem.detail)
  }
}
```

`apiFetch` deve anexar Bearer apenas com sessão válida, omitir `Content-Type` para `FormData`, tratar `204`, analisar ProblemDetail defensivamente e nunca fazer retry automático de mutação.

Configurar `app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}` e construir `CorsConfigurationSource` com a lista configurada, métodos `GET`, `POST`, `OPTIONS` e headers `Authorization`, `Content-Type`.

- [ ] **Step 5: Executar frontend full e commitar**

Expected: no mínimo 8 testes frontend, lint e build verdes.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(integracao): conecta frontend à API com sessão tipada"
git add frontend/package.json frontend/package-lock.json frontend/vitest.config.mts frontend/eslint.config.mjs frontend/next.config.ts frontend/postcss.config.mjs frontend/tsconfig.json frontend/Dockerfile frontend/src/lib frontend/src/test src/main/java/br/com/vistoriapredial/config/security/SecurityConfig.java src/main/resources/application.properties src/test/java/br/com/vistoriapredial/config/security .specs/features/frontend-redesign/tasks.md
git commit -m "feat(integracao): conecta frontend à API com sessão tipada"
```

**Done when**:

- [ ] Sessão corrompida/papel inválido é removida.
- [ ] Multipart mantém boundary do navegador; blobs levam Bearer.
- [ ] ProblemDetail e respostas sem JSON geram feedback tipado e seguro.
- [ ] Preflight da origem configurada passa e origem não listada permanece bloqueada.
- [ ] No mínimo 8 testes, lint e build passam.

**Tests**: unit TypeScript + integração MockMvc CORS
**Gate**: Frontend focused + Frontend full + Backend full
**Commit**: `feat(integracao): conecta frontend à API com sessão tipada`

## Task 6

### T6: Implementar identidade visual, cadastro e proteção por papel

**What**: Aplicar os tokens Vistor.IA, metadata pt-BR, login/cadastro real, layouts protegidos e shell compartilhado com variação de densidade por papel.
**Where**: `frontend/src/features/auth/`
**Depends on**: T5
**Reuses**: referências visuais aprovadas, `lucide-react`, rotas `/login`, `/client`, `/engineer` e serviços de T5.
**Requirement**: Identidade AC1-AC8 e sistema visual AC1-AC5/AC7/AC8.

**Tools**:

- MCP: editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:image-to-code`

- [ ] **Step 1: Escrever ao menos 8 testes RED de autenticação e guard**

```tsx
it("routes an engineer from the API profile without inspecting the email", async () => {
  vi.mocked(login).mockResolvedValue({
    token: "jwt", usuarioId: 2, nome: "Ana", perfil: "ROLE_ENGENHEIRO",
  })
  const user = userEvent.setup()
  render(<AuthForm mode="login" />)

  await user.type(screen.getByLabelText("E-mail"), "ana@exemplo.com")
  await user.type(screen.getByLabelText("Senha"), "segredo123")
  await user.click(screen.getByRole("button", { name: "Entrar" }))

  await waitFor(() => expect(replace).toHaveBeenCalledWith("/engineer"))
})
```

Completar a suíte com: `ROLE_CLIENTE -> /client`; CREA visível/obrigatório só para engenheiro; payload cliente sem `crea`; campos não sensíveis preservados em 409/422; visitante redirecionado antes do filho montar; papel errado enviado à própria home; aviso `aria-live` de sessão expirada.

- [ ] **Step 2: Executar RED focado**

Expected: FAIL pela inexistência de `AuthForm`, `ProtectedArea`, `/register` e shell novo.

- [ ] **Step 3: Implementar design tokens e metadata**

Em `globals.css`, declarar os tokens aprovados e foco consistente:

```css
:root {
  --concrete: #f3efe7;
  --limestone: #ddd4c6;
  --graphite: #252a2e;
  --blueprint: #17324d;
  --clay: #a95736;
  --structural: #2f6657;
  --safety: #c8912f;
}

:focus-visible { outline: 3px solid var(--safety); outline-offset: 3px; }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; transition: none !important; } }
```

`layout.tsx` deve usar `lang="pt-BR"`, title `Vistor.IA | Vistoria predial inteligente` e descrição sem MedFlow.

- [ ] **Step 4: Implementar formulários e guards**

```tsx
<ProtectedArea allowedRole="ROLE_CLIENTE">
  <DashboardShell role="ROLE_CLIENTE">{children}</DashboardShell>
</ProtectedArea>
```

O submit usa `perfil` da resposta para `writeSession` e `router.replace(roleHome(perfil))`; `ProtectedArea` só monta `children` depois da validação cliente.

- [ ] **Step 5: Executar gates, buscar MedFlow e commitar**

```powershell
rg -n "MedFlow|medflow" frontend/src
npm run test
npm run lint
npm run build
```

Expected: busca sem resultados, pelo menos 16 testes frontend totais e gates verdes.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(frontend): implementa identidade e acesso Vistor.IA"
git add frontend/src/app frontend/src/components frontend/src/features/auth .specs/features/frontend-redesign/tasks.md
git commit -m "feat(frontend): implementa identidade e acesso Vistor.IA"
```

**Done when**:

- [ ] Login e cadastro direcionam pelo `perfil` real.
- [ ] CREA é condicional e senha/token não aparecem em logs.
- [ ] Área errada não monta nem dispara chamada protegida.
- [ ] Marca, metadata, paleta, foco e movimento reduzido obedecem a spec.
- [ ] Pelo menos 16 testes frontend, lint e build passam.

**Tests**: component + unit
**Gate**: Frontend focused + Frontend full
**Commit**: `feat(frontend): implementa identidade e acesso Vistor.IA`

### Phase 3: Jornadas de produto

## Task 7

### T7: Implementar painel do cliente e criação de rascunho

**What**: Listar vistorias reais ordenadas, comunicar estados/erros/vazio e criar um único rascunho por endereço antes de navegar ao protocolo.
**Where**: `frontend/src/features/inspections/client/client-dashboard.tsx`
**Depends on**: T6
**Reuses**: `apiFetch`, `StatusBadge`, `AsyncState`, shell cliente e DTO `Inspection`.
**Requirement**: Jornada do cliente AC1-AC2 e cliente HTTP AC4-AC6.

**Tools**:

- MCP: editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:image-to-code`

- [ ] **Step 1: Escrever ao menos 6 testes RED**

```tsx
it("creates a draft without submitting it", async () => {
  vi.mocked(createInspection).mockResolvedValue(draftInspection)
  const user = userEvent.setup()
  render(<NewInspectionForm />)

  await user.type(screen.getByLabelText("Endereço do imóvel"), "Rua das Obras, 10")
  await user.dblClick(screen.getByRole("button", { name: "Criar rascunho" }))

  await waitFor(() => expect(createInspection).toHaveBeenCalledTimes(1))
  expect(submitInspection).not.toHaveBeenCalled()
  expect(replace).toHaveBeenCalledWith(`/client/vistorias/${draftInspection.id}`)
})
```

Completar a suíte com: ordenação decrescente por `dataCriacao`; endereço/status/próxima ação; vazio acionável; erro inicial com retry bem-sucedido; endereço preservado após ProblemDetail; botão ocupado durante a única criação.

- [ ] **Step 2: Executar RED e implementar API de inspeções**

```typescript
export const listMyInspections = () => apiFetch<Inspection[]>("/vistorias/minhas")
export const getMyInspection = async (id: number) => {
  const inspection = (await listMyInspections()).find((item) => item.id === id)
  if (!inspection) throw new ApiError({ status: 404, detail: "Vistoria não encontrada." })
  return inspection
}
export const createInspection = (endereco: string) =>
  apiFetch<Inspection>("/vistorias", {
    method: "POST",
    body: JSON.stringify({ endereco: endereco.trim() }),
  })
```

- [ ] **Step 3: Implementar painel e rota de nova vistoria**

O formulário fica em `/client/vistorias/nova`, exige endereço não vazio, mantém o valor em erro e, em sucesso, usa `router.replace(`/client/vistorias/${created.id}`)` sem chamar `/submeter`.

- [ ] **Step 4: Executar frontend full e commitar**

Expected: pelo menos 22 testes frontend totais, lint e build verdes.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(cliente): cria painel e rascunho de vistoria"
git add frontend/src/app/(dashboard)/client frontend/src/features/inspections frontend/src/components/ui .specs/features/frontend-redesign/tasks.md
git commit -m "feat(cliente): cria painel e rascunho de vistoria"
```

**Done when**:

- [ ] Lista usa `dataCriacao`, `endereco`, `status` e próxima ação reais.
- [ ] Erro inicial tem retry e vazio tem uma única ação válida.
- [ ] Duplo clique cria uma requisição e rascunho não é auto-submetido.
- [ ] Pelo menos 22 testes frontend, lint e build passam.

**Tests**: component
**Gate**: Frontend focused + Frontend full
**Commit**: `feat(cliente): cria painel e rascunho de vistoria`

## Task 8

### T8: Implementar protocolo guiado, upload e submissão

**What**: Renderizar os cinco grupos/doze itens, reconstruir progresso da API, validar e subir imagens por item, permitir retry e submeter uma única vez nos estados permitidos.
**Where**: `frontend/src/features/inspections/client/inspection-workflow.tsx`
**Depends on**: T7
**Reuses**: protocolo compartilhado, `EvidenceImage`, `ApiError`, rota dinâmica Next 16 e endpoint de T3/T4.
**Requirement**: Jornada do cliente AC3-AC12 e edge cases de upload/refresh/duplo clique.

**Tools**:

- MCP: editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:image-to-code`

- [ ] **Step 1: Escrever ao menos 8 testes RED**

```tsx
it("submits once and renders the returned status", async () => {
  vi.mocked(getMyInspection).mockResolvedValue(draftWithEvidence)
  vi.mocked(submitInspection).mockResolvedValue(pendingEngineerInspection)
  const user = userEvent.setup()
  render(<InspectionWorkflow inspectionId={10} />)

  await screen.findByText("1 de 12 itens documentados")
  await user.dblClick(screen.getByRole("button", { name: "Enviar para análise" }))

  await waitFor(() => expect(submitInspection).toHaveBeenCalledTimes(1))
  expect(await screen.findByText("Aguardando revisão do engenheiro")).toBeDefined()
})
```

Completar a suíte com: cinco grupos/doze códigos; progresso por itens únicos persistidos; arquivo vazio/grande/MIME inválido sem fetch; FormData com código exato; retry preservando evidências; bloqueio sem evidência; parecer e edição em `DEVOLVIDA_CLIENTE`; retry somente em `FALHA_IA` usando o mesmo id.

- [ ] **Step 2: Executar RED e implementar helpers puros**

```typescript
export const MAX_EVIDENCE_BYTES = 10 * 1024 * 1024
export const ACCEPTED_EVIDENCE_TYPES = ["image/jpeg", "image/png", "image/webp"] as const

export function calculateProgress(evidence: Evidence[]): number {
  return new Set(evidence.map((item) => item.protocoloItem)).size
}
```

- [ ] **Step 3: Implementar upload e estados**

```typescript
const body = new FormData()
body.append("protocoloItem", item.code)
body.append("file", file)
return apiFetch<Inspection>(`/vistorias/${id}/imagens`, { method: "POST", body })
```

Cada item mantém erro/ocupado local; envio final exige evidência confirmada, exibe revisão e bloqueia clique concorrente. Estados não editáveis renderizam acompanhamento somente leitura.

- [ ] **Step 4: Implementar rota dinâmica conforme Next 16**

```tsx
export default async function Page({ params }: PageProps<"/client/vistorias/[id]">) {
  const { id } = await params
  return <InspectionWorkflow inspectionId={Number(id)} />
}
```

- [ ] **Step 5: Executar frontend full e commitar**

Expected: pelo menos 30 testes frontend totais, lint e build verdes.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(cliente): implementa protocolo guiado de evidências"
git add frontend/src/app/(dashboard)/client/vistorias frontend/src/features/inspections/client frontend/src/features/inspections/shared .specs/features/frontend-redesign/tasks.md
git commit -m "feat(cliente): implementa protocolo guiado de evidências"
```

**Done when**:

- [ ] Cinco grupos/doze itens e progresso vêm do contrato compartilhado.
- [ ] Upload inválido não chama API; falha não apaga rascunho/evidências.
- [ ] Submissão é única e somente após evidência confirmada.
- [ ] Retorno, falha IA e acompanhamento respeitam estados do backend.
- [ ] Pelo menos 30 testes frontend, lint e build passam.

**Tests**: component + unit
**Gate**: Frontend focused + Frontend full
**Commit**: `feat(cliente): implementa protocolo guiado de evidências`

## Task 9

### T9: Implementar fila e revisão técnica do engenheiro

**What**: Listar pendências ordenadas, abrir workspace responsivo, carregar evidências autenticadas, estruturar somente linhas reais do pré-laudo e aprovar/devolver com parecer obrigatório.
**Where**: `frontend/src/features/inspections/engineer/`
**Depends on**: T8
**Reuses**: visual de revisão aprovado, `fetchEvidenceBlob`, `StatusBadge`, `PreReport` e endpoint `/analisar`.
**Requirement**: Fila e revisão AC1-AC10, Human-in-the-Loop e edge case concorrente.

**Tools**:

- MCP: editor local
- Skills: `tlc-spec-driven`, `superpowers:test-driven-development`, `product-design:image-to-code`

- [ ] **Step 1: Escrever ao menos 8 testes RED**

```tsx
it("keeps both decisions disabled for a blank technical opinion", async () => {
  vi.mocked(listPendingInspections).mockResolvedValue([pendingInspection])
  const user = userEvent.setup()
  render(<EngineerReview inspectionId={pendingInspection.id} />)

  await screen.findByText("Pré-laudo da IA")
  await user.type(screen.getByLabelText("Parecer técnico"), "   ")

  expect((screen.getByRole("button", { name: "Aprovar vistoria" }) as HTMLButtonElement).disabled).toBe(true)
  expect((screen.getByRole("button", { name: "Devolver ao cliente" }) as HTMLButtonElement).disabled).toBe(true)
})
```

Completar a suíte com: ordenação por data/id; somente linhas/evidências da API; ausência de severidade/confiança; duas mensagens Human-in-the-Loop; aprovação única com `true`; devolução única com `false` e remoção; 409 bloqueando/refazendo fila; foto indisponível isolada; `URL.revokeObjectURL` na troca e desmontagem.

- [ ] **Step 2: Executar RED e implementar parsing sem inferência**

```typescript
export function splitPreReport(value: string | null): string[] {
  if (!value?.trim()) return []
  return value.split(/\r?\n/)
    .map((line) => line.trim().replace(/^[-*•]\s*/, ""))
    .filter(Boolean)
}
```

- [ ] **Step 3: Implementar imagem autenticada com cleanup**

```tsx
useEffect(() => {
  let active = true
  let objectUrl: string | undefined
  fetchEvidenceBlob(evidence.conteudoUrl)
    .then((blob) => {
      if (!active) return
      objectUrl = URL.createObjectURL(blob)
      setSource(objectUrl)
    })
    .catch(() => active && setUnavailable(true))
  return () => {
    active = false
    if (objectUrl) URL.revokeObjectURL(objectUrl)
  }
}, [evidence.conteudoUrl])
```

- [ ] **Step 4: Implementar fila, workspace e decisão**

O desktop usa evidência à esquerda, pré-laudo ao centro e decisão à direita; tablet/móvel empilham as regiões sem overflow. Parecer `trim()` vazio desabilita ambas as ações. Resposta 409 fecha edição, refaz a fila e informa processamento concorrente.

- [ ] **Step 5: Executar frontend full e commitar**

Expected: pelo menos 38 testes frontend totais, lint e build verdes.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "feat(engenheiro): implementa revisão técnica de vistorias"
git add frontend/src/app/(dashboard)/engineer frontend/src/features/inspections/engineer frontend/src/features/inspections/shared .specs/features/frontend-redesign/tasks.md
git commit -m "feat(engenheiro): implementa revisão técnica de vistorias"
```

**Done when**:

- [ ] Fila e detalhe usam somente DTO real e ordem determinística.
- [ ] Pré-laudo não cria severidade/confiança e contém os dois avisos profissionais.
- [ ] Parecer é obrigatório; aprovação/devolução são únicas e reconciliadas com servidor.
- [ ] Falha de uma foto não bloqueia o caso e object URLs são revogadas.
- [ ] Pelo menos 38 testes frontend, lint e build passam.

**Tests**: component + unit
**Gate**: Frontend focused + Frontend full
**Commit**: `feat(engenheiro): implementa revisão técnica de vistorias`

### Phase 4: Integração e aceite

## Task 10

### T10: Validar backend, comunicação e experiência ponta a ponta

**What**: Executar todos os gates em estado fresco, provar CORS/REST com backend e frontend reais, realizar UAT acessível/responsivo, aplicar sensor de discriminação e emitir `validation.md` PASS antes do push.
**Where**: `.specs/features/frontend-redesign/validation.md`
**Depends on**: T9
**Reuses**: suítes, navegador controlado, spec, design, tarefas e scripts TLC.
**Requirement**: Todos os critérios de aceitação e definição de pronto.

**Tools**:

- MCP: terminal local e navegador controlado
- Skills: `tlc-spec-driven`, `browser:control-in-app-browser`, `superpowers:verification-before-completion`, `product-design:audit`

- [ ] **Step 1: Executar gates backend atuais**

```powershell
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=PostgreSqlMigrationIntegrationTest" test
```

Expected: no mínimo 58 testes backend, contexto Spring/Flyway/Security verde e migration PostgreSQL verde.

- [ ] **Step 2: Executar gates frontend atuais**

```powershell
npm run test
npm run lint
npm run build
```

Expected: no mínimo 38 testes frontend, zero erro ESLint/TypeScript e build Next 16 verde.

- [ ] **Step 3: Subir os dois processos e testar o contrato real**

Backend usa porta 8080 e diretório de upload temporário; frontend usa `NEXT_PUBLIC_API_URL=http://localhost:8080/api`. Criar por API/UI um cliente e um engenheiro exclusivos do UAT, sem serviço externo pago.

- [ ] **Step 4: Executar UAT cliente**

Validar: cadastro/login por perfil; lista vazia; criação de um único rascunho; rejeição de arquivo inválido; upload JPEG válido; refresh preservando progresso; submissão única; acompanhamento; acesso de outro cliente à foto recebendo 403.

- [ ] **Step 5: Executar UAT engenheiro**

Validar: fila pendente; abertura do caso; evidência autenticada; pré-laudo preliminar sem dados inventados; parecer vazio bloqueado; devolução e complementação do cliente; nova submissão; aprovação final.

- [ ] **Step 6: Auditar acessibilidade e responsividade**

Repetir jornadas essenciais por teclado em 375, 768 e 1440 px; conferir foco visível, labels, nomes de ícone, `aria-live`, ausência de overflow horizontal e preferência de movimento reduzido.

- [ ] **Step 7: Executar verificação independente standalone e sensor**

Sem subagente autorizado, fazer uma segunda passagem de olhos frescos: mapear cada AC para `arquivo:linha` e teste; em cópia temporária isolada, mutar no mínimo redirecionamento por perfil, header FormData, ownership de evidência e parecer obrigatório. Cada mutante deve ser morto pelo gate correspondente; descartar a cópia e confirmar que `git status --porcelain` do repositório real não mudou.

- [ ] **Step 8: Escrever o relatório e validar o estado**

`validation.md` deve conter `Status: PASS`, evidência por AC, comandos/contagens, UAT, sensor e diff range. Depois executar:

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\validate_state.py frontend-redesign --root .
```

- [ ] **Step 9: Atualizar status e commitar validação**

Marcar T10, `tasks.md` e `design.md` como Done/Approved somente depois do PASS.

```powershell
python C:\Users\vine\.codex\skills\tlc-spec-driven\scripts\check_commit.py --message "test(frontend): valida experiência Vistor.IA integrada"
git add .specs/features/frontend-redesign/design.md .specs/features/frontend-redesign/tasks.md .specs/features/frontend-redesign/validation.md
git commit -m "test(frontend): valida experiência Vistor.IA integrada"
```

- [ ] **Step 10: Revisar commits e fazer push autorizado**

```powershell
git status --short
git log --oneline --decorate -12
git push origin main
```

Push só ocorre se o status não contiver mudanças da feature fora de commit e todos os gates anteriores estiverem PASS. Mudanças preexistentes alheias permanecem fora dos commits.

**Done when**:

- [ ] Backend, PostgreSQL, frontend, CORS e fluxos reais estão comprovados por execução atual.
- [ ] UAT passa em cliente/engenheiro e nas três larguras, inclusive teclado.
- [ ] Sensor mata os quatro defeitos e árvore real permanece intacta.
- [ ] `validate_state.py` retorna zero erros.
- [ ] Commit de validação e push de `main` concluem sem force-push.

**Tests**: integração + UAT + sensor de discriminação
**Gate**: Integrated + TLC completion
**Commit**: `test(frontend): valida experiência Vistor.IA integrada`

---

## Phase Execution Map

```text
Phase 1: T1 -> T2 -> T3 -> T4
Phase 2: T5 -> T6
Phase 3: T7 -> T8 -> T9
Phase 4: T10
```

Cross-phase dependencies are explicit in each task body; phases themselves execute in order.

## Task Granularity Check

| Task | Reviewable deliverable | Status |
| --- | --- | --- |
| T1 | Um script de build executável | ✅ Granular |
| T2 | Uma migration comprovada no banco-alvo | ✅ Granular |
| T3 | Um contrato coeso de entrada/submissão de evidência | ✅ Granular |
| T4 | Um endpoint autenticado de leitura de evidência | ✅ Granular |
| T5 | Um serviço compartilhado de sessão/HTTP | ✅ Granular |
| T6 | Uma fronteira coesa de identidade e acesso | ✅ Granular |
| T7 | Um painel de cliente com criação de rascunho | ✅ Granular |
| T8 | Um workflow guiado de evidências | ✅ Granular |
| T9 | Um workspace de revisão do engenheiro | ✅ Granular |
| T10 | Um aceite integrado com relatório verificável | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | início da Phase 1 | ✅ Match |
| T2 | T1 | T1 -> T2 | ✅ Match |
| T3 | T2 | T2 -> T3 | ✅ Match |
| T4 | T3 | T3 -> T4 | ✅ Match |
| T5 | T4 (cross-phase) | Phase 1 -> Phase 2 | ✅ Match |
| T6 | T5 | T5 -> T6 | ✅ Match |
| T7 | T6 (cross-phase) | Phase 2 -> Phase 3 | ✅ Match |
| T8 | T7 | T7 -> T8 | ✅ Match |
| T9 | T8 | T8 -> T9 | ✅ Match |
| T10 | T9 (cross-phase) | Phase 3 -> Phase 4 | ✅ Match |

## Test Co-location Validation

| Task | Code layer modified | Matrix requires | Task says | Status |
| --- | --- | --- | --- | --- |
| T1 | Build tooling | smoke | smoke + suíte | ✅ OK |
| T2 | Flyway/PostgreSQL | integration | integração PostgreSQL | ✅ OK |
| T3 | Domain/application/web | unit + MockMvc | unit + MockMvc | ✅ OK |
| T4 | Storage/repository/web | unit + integration | unit + repository + MockMvc | ✅ OK |
| T5 | TypeScript utilities | unit | unit TypeScript | ✅ OK |
| T6 | React/auth/config | component + build | component + unit | ✅ OK |
| T7 | React client dashboard | component | component | ✅ OK |
| T8 | React workflow/helpers | component + unit | component + unit | ✅ OK |
| T9 | React engineer workspace | component + unit | component + unit | ✅ OK |
| T10 | Integrated system | UAT + full gates | integração + UAT + sensor | ✅ OK |
