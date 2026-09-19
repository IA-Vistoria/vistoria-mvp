# AGENTS.md — Contrato de Engenharia do Vistoria Predial

Este arquivo define como agentes devem analisar, implementar, testar e relatar mudanças neste repositório. Ele é um contrato de trabalho: deve orientar decisões sem substituir o raciocínio técnico nem transformar convenções em dogmas.

O projeto tem backend Java/Spring Boot como núcleo e segue a direção de um monólito modular por feature. O MedFlow pode servir como referência histórica de estilo, mas não é fonte de verdade deste repositório.

---

## 1. Princípios de atuação

- Trabalhe como engenheiro de software: entenda o problema, preserve o que já existe e escolha a solução pelo contexto.
- Antes de propor arquitetura ou código, verifique o estado real do repositório. Comentários, planos e nomes de testes não provam comportamento.
- Explique o porquê de decisões técnicas não triviais, incluindo alternativas e trade-offs relevantes.
- Prefira a solução mais simples que preserve segurança, clareza, testabilidade e evolução futura. Não introduza abstrações sem necessidade comprovada.
- Mantenha separação de responsabilidades, baixo acoplamento e dependências explícitas.
- Use português brasileiro nas respostas, documentação e comentários. Identificadores devem seguir a linguagem do domínio e as convenções já adotadas pela feature.
- Comentários de código explicam contexto, restrições ou decisões; não repetem o que o código já expressa.
- Não declare que algo funciona, está coberto ou está pronto sem evidência atual e reproduzível.

### Modos de colaboração

O pedido do usuário determina o modo de trabalho:

1. **Execução** — quando o usuário pedir para criar, implementar, corrigir ou refatorar, realize a mudança de ponta a ponta dentro do escopo autorizado, incluindo testes e documentação afetada.
2. **Auditoria ou diagnóstico** — quando o pedido for revisar, explicar, conferir ou diagnosticar, permaneça em leitura. Não implemente uma correção sem autorização.
3. **Mentoria ou pair programming** — quando o usuário disser que quer escrever do zero ou aprender fazendo, ele é o driver do código de produção. O agente explica a responsabilidade e o motivo, oferece apenas o próximo micro-objetivo, revisa a tentativa e cria/executa os testes combinados.

Não troque silenciosamente de modo. Em qualquer modo, evite pedir confirmações repetidas para ações locais, reversíveis e já abrangidas pelo pedido.

---

## 2. Fontes de verdade e resolução de conflitos

Cada fonte responde a uma pergunta diferente:

| Fonte | Autoridade principal |
| --- | --- |
| Solicitação atual do usuário | Objetivo, escopo e resultado esperado |
| `pom.xml`, código, migrations e testes executados | Estado técnico efetivamente existente |
| `.specs/features/<feature>/spec.md` | Comportamento e critérios de aceitação pretendidos |
| `.specs/features/<feature>/design.md` | Decisões de desenho aprovadas para a feature |
| `.specs/features/<feature>/tasks.md` | Sequência e acompanhamento da implementação |
| `.specs/STATE.md` | Handoff e decisões ativas, sempre revalidados no repositório |
| Este `AGENTS.md` | Convenções padrão de engenharia e operação |
| Repositórios ou documentos externos | Inspiração; nunca autoridade automática |

Regras de resolução:

- A intenção documentada não prova implementação; o código existente não altera silenciosamente o requisito aprovado.
- Se build, documentação e código divergirem, identifique os arquivos, o impacto e qual dimensão está incorreta: estado atual, intenção futura ou ambos.
- Corrija uma divergência documental sem nova pergunta apenas quando isso estiver claramente dentro do escopo e não mudar contrato, arquitetura ou comportamento. Em conflito material, peça uma decisão.
- Não copie arquitetura, dependência ou implementação do MedFlow sem confirmar que ela atende este domínio e esta versão da stack.
- Nunca ajuste dependências ou faça downgrade do build apenas para coincidir com documentação desatualizada.

---

## 3. Baseline técnico atual

Revalide este resumo antes de decisões sensíveis; o `pom.xml` prevalece para versões:

- Java 21.
- Spring Boot 4.1.1 no estado atual.
- Maven Wrapper como forma canônica de execução.
- Spring MVC, Spring Data JPA, Bean Validation, Spring Security e Flyway.
- H2 para execução local atual e PostgreSQL como banco-alvo.
- JUnit 5, Mockito, testes Spring e Testcontainers PostgreSQL.
- Package-base canônico: `br.com.vistoriapredial`.
- Tratamento HTTP baseado em RFC 9457 com `ProblemDetail`.
- Armazenamento local atrás da abstração `StorageService`, com futura substituição possível por armazenamento externo.

### Alertas já identificados

Antes de trabalhar nas áreas abaixo, revalide e trate a divergência no escopo correto:

- Parte de `.specs` ainda menciona Spring Boot 3.x e `com.vistoriapredial`; o build usa Spring Boot 4.1.1 e `br.com.vistoriapredial`.
- A migration `V1__create_initial_schema.sql` declara compatibilidade com PostgreSQL, mas usa `AUTO_INCREMENT`; não presuma portabilidade sem teste no PostgreSQL.
- Existe fallback de segredo JWT para desenvolvimento; produção deve falhar de forma segura se o segredo real não estiver configurado.
- `VistoriaPredialApplicationTests.contextLoads()` atualmente não sobe o contexto Spring; um teste vazio não conta como evidência de inicialização.
- O projeto ainda está no início: `storage` e `shared.web.error` existem, mas os módulos de negócio descritos nas especificações ainda não estão todos implementados.

Esses alertas não autorizam correções fora da tarefa atual.

---

## 4. Antes de alterar qualquer arquivo

1. Leia a solicitação e defina o comportamento observável que deve mudar.
2. Confira branch, `git status` e mudanças preexistentes.
3. Leia os arquivos de produção, testes, migrations e especificações diretamente relacionados.
4. Confirme versões e APIs na fonte local da dependência; consulte documentação oficial quando houver incerteza relevante.
5. Escolha a menor alteração coerente com o desenho existente.
6. Defina como a mudança será comprovada antes de implementá-la.

Preserve o trabalho alheio:

- Não reverta, reformate, mova, inclua em commit ou sobrescreva mudanças que não pertencem à tarefa.
- Não faça refatoração oportunista sem relação com o objetivo.
- Se uma mudança preexistente sobrepuser a área necessária, pare e explique o conflito.
- Arquivos gerados, `target/`, uploads, segredos e temporários não entram em commits.

---

## 5. Arquitetura e dependências

### Direção arquitetural

Módulos de negócio devem seguir monólito modular por feature:

```text
br.com.vistoriapredial/
├── VistoriaPredialApplication.java
├── <feature>/
│   ├── domain/                 # Entidades, enums, value objects e invariantes
│   ├── persistence/            # Repositories e detalhes de persistência da feature
│   ├── application/            # Casos de uso e orquestração
│   │   ├── dto/                # Contratos imutáveis de entrada e saída
│   │   └── exception/          # Exceções específicas da feature
│   └── web/                    # Controllers e adaptação HTTP
├── shared/
│   └── web/error/              # Contrato transversal de erros HTTP
├── storage/                    # Porta e adaptadores de armazenamento
├── integration/<provider>/     # Clientes de serviços externos
└── config/                     # Segurança e infraestrutura compartilhada
```

Essa é a arquitetura-alvo dos módulos de negócio, não uma afirmação de que todos já existem.

### Regras de dependência

- `web` conhece contratos e casos de uso de `application`; não acessa repository diretamente.
- `application` coordena domínio, persistência e portas de infraestrutura.
- `domain` não depende de controller, DTO HTTP, configuração Spring ou cliente externo.
- `persistence` não contém regra de negócio.
- Integrações externas ficam atrás de interfaces ou adaptadores com contratos testáveis.
- Uma feature não acessa internals de outra feature por conveniência. Prefira caso de uso público, identificador, contrato explícito ou evento interno quando houver benefício real.
- Código só deve ir para `shared` quando for genuinamente transversal e estável. Não use `shared` como depósito genérico.

Arquitetura em camadas, hexagonal, eventos internos ou outras técnicas são ferramentas. Adote complexidade adicional somente quando o problema justificar e registre a decisão relevante em `.specs/STATE.md` ou ADR.

---

## 6. Domínio e persistência JPA

- Entidades têm construtor de negócio explícito e construtor sem argumentos `protected` para o JPA.
- Não use Lombok. Prefira construtores, getters e métodos de intenção explícitos.
- Evite setters públicos anêmicos. Alterações de estado devem passar por métodos que expressem a operação de domínio.
- Validação estrutural pertence ao DTO; invariantes pertencem ao domínio; autorização, transições de estado e coordenação entre componentes pertencem ao caso de uso.
- Novas violações de domínio devem usar exceções específicas. Não use `IllegalArgumentException` como atalho de contrato HTTP; o mapeamento genérico existente não deve ser expandido sem revisão.
- Mapeie nulabilidade, tamanho, precisão e unicidade explicitamente quando aplicáveis. Não aplique `length` a tipos para os quais não tem efeito.
- Relacionamentos devem declarar coluna, cardinalidade, lado proprietário e estratégia de carregamento conscientemente. Prefira `LAZY`; justifique `EAGER` e `CascadeType.ALL`.
- Enums persistidos usam `EnumType.STRING`.
- Regras de unicidade e integridade críticas também existem no banco. Uma consulta prévia não substitui constraint.
- Entidades mutáveis sujeitas a concorrência usam `@Version`. Exceções, como tabelas associativas imutáveis, devem ser justificadas.
- `createdAt` e `updatedAt` usam `Instant` e um único mecanismo consistente de auditoria. Quando o tempo influencia regra de negócio, injete `Clock` para permitir testes determinísticos.
- Entidades nunca são expostas diretamente pela API.
- Considere N+1, paginação e volume de dados ao desenhar consultas; não adicione otimizações sem medição ou caso de uso concreto.

---

## 7. DTOs, API HTTP e controllers

- Contratos HTTP são imutáveis; use Java `record` por padrão.
- Siga uma convenção consistente por feature, preferencialmente `CreateXxxRequestDto`, `UpdateXxxRequestDto` e `XxxResponseDto`.
- Requests declaram Bean Validation com mensagens claras em português.
- Responses expõem apenas dados do contrato; nunca senha, hash, segredo, token interno ou detalhes de infraestrutura.
- Faça mapeamento explícito entre entidade e DTO por construtor, factory ou mapper pequeno. Não introduza framework de mapeamento sem necessidade e aprovação.
- Controller recebe e valida HTTP, delega ao caso de uso e monta a resposta. Não contém regra de negócio nem `try/catch` para fluxo normal.
- Use `@Valid` e, quando houver validação de parâmetros, `@Validated` de forma consciente.
- Códigos HTTP devem refletir o resultado:
  - criação: `201 Created`, preferencialmente com `Location`;
  - consulta e atualização: `200 OK`;
  - exclusão ou comando sem corpo: `204 No Content`;
  - entrada estruturalmente inválida: `422 Unprocessable Content` quando o JSON foi compreendido;
  - JSON malformado ou parâmetro inconversível: `400 Bad Request`.
- Listagens devem ter paginação e ordenação determinística. Escolha `Page`, `Slice` ou envelope paginado pelo contrato, não por conveniência interna.
- Não altere rota, payload, status ou semântica de endpoint existente sem teste de contrato e atualização da documentação correspondente.

---

## 8. Camada de aplicação, transações e infraestrutura

- Use injeção exclusivamente por construtor, com dependências `private final`.
- Um método de aplicação representa um caso de uso coeso. Ele pode ter condicionais quando elas expressam autorização, regra de estado, idempotência ou decisão real do fluxo.
- Não repita validações já garantidas pela borda ou pelo domínio, mas também não confunda ausência de duplicação com ausência de regra de aplicação.
- Resolva ausências de forma explícita, por exemplo com `orElseThrow(() -> new XxxNotFoundException(id))`.
- Escritas usam `@Transactional`; leituras usam `@Transactional(readOnly = true)` quando isso representar a fronteira correta.
- Mantenha a transação curta. Evite segurar transação de banco durante upload, chamada de IA ou outra operação de rede.
- Não capture exceções apenas para relançá-las sem contexto. Adaptadores de I/O podem traduzir checked exceptions em exceções próprias, preservando a causa.
- Nunca engula falhas, retorne sucesso falso ou use `catch (Exception)` como controle de fluxo.
- Em operações que combinam banco, arquivo e serviço externo, defina ordem, idempotência e compensação para evitar estado parcial.

---

## 9. Erros e contrato RFC 9457

Erros expostos pela API usam `application/problem+json` e `ProblemDetail`, com `type`, `title`, `status`, `detail` seguro e `instance` quando aplicável.

Matriz esperada:

| Categoria | Status esperado | Observação |
| --- | --- | --- |
| JSON malformado ou tipo incompatível | 400 | Não expor stack trace ou parser interno |
| Invariante/regra de domínio inválida | 400 ou 422 | Usar exceção tipada e convenção consistente |
| Bean Validation | 422 | Incluir todos os campos e ponteiros RFC 6901 |
| Não autenticado | 401 | Tratar também na cadeia do Spring Security |
| Sem permissão | 403 | Tratar também na cadeia do Spring Security |
| Recurso inexistente | 404 | Exceção de recurso ou hierarquia comum |
| Conflito de negócio conhecido | 409 | Apoiado por regra e constraint quando aplicável |
| Falha inesperada | 500 | Mensagem genérica ao cliente e log interno |

- `AuthenticationEntryPoint` e `AccessDeniedHandler` devem manter o mesmo formato RFC 9457; `@ControllerAdvice` sozinho não cobre a cadeia de segurança.
- Só traduza `DataIntegrityViolationException` para 409 quando a constraint conhecida puder ser identificada com segurança. Falhas desconhecidas não viram conflito por padrão.
- Prefira uma hierarquia coerente de exceções a um handler repetido para cada classe sem necessidade.
- Logs podem conter contexto operacional e identificador de correlação, mas nunca senha, JWT, segredo, conteúdo integral de imagem, payload sensível da IA ou dados pessoais desnecessários.

---

## 10. Segurança, uploads e IA

### Segurança

- Adote menor privilégio e política de acesso negado por padrão.
- Autorização de rota não substitui autorização sobre o recurso. Valide papel e ownership no caso de uso.
- Senhas devem ser armazenadas somente com hash forte. Na feature de autenticação, adote BCrypt salvo decisão explícita diferente e revalide o `PasswordEncoder` realmente configurado; senhas nunca trafegam de volta em response.
- Segredos vêm de variáveis de ambiente ou secret manager. Nenhum valor padrão utilizável em produção pode ficar versionado.
- Configuração de CORS, CSRF, sessão e JWT deve ser explícita e acompanhada de teste de segurança.

### Uploads e armazenamento

- Valide tamanho, tipo permitido, conteúdo esperado e nome do arquivo.
- Gere nomes de destino no servidor; não confie em caminho ou nome enviado pelo cliente.
- Normalize o destino e garanta que ele permaneça dentro da raiz configurada.
- Não exponha caminho físico do servidor no contrato HTTP.
- Exclusão e sobrescrita de arquivo exigem atenção a autorização, idempotência e consistência com o banco.

### IA e integrações externas

- Preserve o modelo **Human-in-the-Loop**: a saída da IA é uma sugestão preliminar e não substitui homologação do engenheiro civil.
- Trate respostas do modelo como entrada não confiável: valide formato, limites e campos obrigatórios antes de persistir ou exibir.
- Configure timeouts explícitos e retries limitados apenas para operações seguras/idempotentes.
- Registre estados de falha reais; nunca fabrique pré-laudo ou marque sucesso após timeout.
- Testes automatizados não chamam OCI, modelo pago ou serviço externo real. Use fake, stub ou servidor mock.
- Chamadas reais com credenciais, custo ou envio de imagens exigem autorização explícita.

---

## 11. Banco de dados e migrations

- Flyway é a fonte versionada do schema. `ddl-auto` permanece em `validate`; Hibernate não cria ou corrige schema em produção.
- Nunca edite migration já aplicada. Primeiro verifique se ela foi executada em algum ambiente; se foi, crie nova migration.
- Cada nova migration deve ser validada no PostgreSQL, que é o dialeto-alvo. H2 verde não comprova compatibilidade.
- Use tipos, geração de IDs e sintaxe compatíveis com o banco-alvo; comentário de arquivo não é evidência de portabilidade.
- Toda regra crítica de nulabilidade, unicidade, chave estrangeira e integridade deve aparecer no schema e ser testada.
- Adicione índices a partir de consultas e padrões de acesso reais.
- Mudanças destrutivas ou de alto risco exigem plano de migração, preservação dos dados e estratégia de rollback ou recuperação.

---

## 12. Estratégia de testes

Testes fazem parte da implementação. Todo comportamento novo ou alterado deve ter prova proporcional ao risco.

### Por camada

- **Domínio:** teste unitário puro para invariantes, transições e casos de borda, sem subir Spring.
- **Application/Service:** teste unitário com Mockito quando o objetivo for a orquestração; não mocke a própria regra que deseja provar.
- **Web:** `@WebMvcTest`/MockMvc para rota, serialização, validação, status, ProblemDetail e autorização.
- **Persistência:** `@DataJpaTest` ou integração com Testcontainers PostgreSQL para mapping, queries e constraints.
- **Migration/contexto:** teste de integração real que execute Flyway e suba o contexto. Método vazio não é teste de contexto.
- **Storage:** diretório temporário com `@TempDir`; nunca escreva em pasta real do usuário durante teste.
- **Cliente externo:** servidor mock/fake para payload, autenticação, timeout e respostas inválidas.

### Qualidade dos testes

- Nomeie o comportamento observado e mantenha Arrange–Act–Assert legível.
- Teste resultado e contrato, não detalhes privados de implementação.
- Para bug, primeiro reproduza a falha; depois prove a correção e a ausência de regressão.
- Para TDD solicitado, confirme RED antes da produção e GREEN depois.
- Percentual de cobertura é sinal, não substituto para cenários. Cada critério de aceitação e regra de negócio relevante deve ter rastreabilidade.
- Ao entregar um teste, explique o que ele cobre e por que é unitário ou de integração, incluindo a razão de mocks versus dependências reais.

### Comandos canônicos no Windows/PowerShell

```powershell
# suíte completa
.\mvnw.cmd test

# teste focado
.\mvnw.cmd "-Dtest=NomeDoTeste" test

# compilação dos testes quando esse for o gate definido
.\mvnw.cmd clean test-compile
```

- Use o Maven Wrapper do repositório. Não substitua silenciosamente por Maven global.
- Se o wrapper ou a infraestrutura falhar, registre comando, saída e causa provável. Não transforme teste não executado em teste aprovado.
- Mudança apenas documental não exige suíte Maven, salvo quando altera comandos, configuração técnica ou afirmações sobre o build.

---

## 13. Fluxo de implementação e especificações

Quando a feature estiver governada por `.specs`:

1. Revalide `spec.md`, `design.md`, `tasks.md` e `.specs/STATE.md` contra o repositório.
2. Confirme que o critério de aceitação da tarefa é verificável.
3. Implemente uma unidade lógica por vez.
4. Execute primeiro o teste focado e depois o gate completo proporcional à mudança.
5. Atualize o status da tarefa somente depois da evidência; não marque conclusão por intenção.
6. Registre decisões arquiteturais novas e divergências relevantes.

Se `tasks.md` exigir a skill `tlc-spec-driven`, siga seu fluxo. Se a skill não estiver disponível, pare antes de implementar a tarefa governada por ela e informe o bloqueio.

---

## 14. Git, commits e documentação

- Não faça commit, push, merge, rebase ou criação de PR sem solicitação do usuário ou autorização explícita do fluxo em execução.
- Quando autorizado, cada commit representa uma mudança lógica completa e contém somente arquivos relacionados.
- Antes do commit: revise o diff, execute os testes aplicáveis e confira arquivos staged.
- Use Conventional Commits em português:
  - tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`;
  - formato: `<tipo>(<escopo>): <descrição>`;
  - descrição no imperativo, sem ponto final e com no máximo 72 caracteres.
- Não misture feature e refatoração independente no mesmo commit.
- README, OpenAPI, coleção HTTP, `.specs` e exemplos devem refletir apenas capacidades verificadas.
- Este `AGENTS.md` está atualmente ignorado pelo `.gitignore`. Não altere essa decisão silenciosamente; versioná-lo deve ser uma tarefa explícita.

---

## 15. Ações que exigem confirmação

Peça confirmação imediatamente antes de:

- remover arquivos ou dados;
- executar `DELETE`, `TRUNCATE`, `DROP` ou migration destrutiva;
- editar ou sobrescrever migration possivelmente aplicada;
- adicionar, remover ou atualizar dependência;
- executar deploy, alteração de infraestrutura ou chamada externa com custo/credenciais reais;
- expor, rotacionar ou substituir segredo;
- sobrescrever arquivo do usuário fora do escopo;
- executar operação Git destrutiva, push forçado ou alteração remota não solicitada.

Leitura, edição local autorizada e testes não destrutivos não exigem confirmação adicional.

---

## 16. Definição de pronto

Uma tarefa só está pronta quando:

- o comportamento atende ao pedido e aos critérios de aceitação aplicáveis;
- a solução respeita as fronteiras e convenções do projeto ou documenta a exceção;
- os testes adequados passaram em execução atual, com comando e resultado conferidos;
- migrations, segurança, erros e contratos HTTP foram avaliados quando afetados;
- documentação e estado da tarefa não contradizem a implementação;
- o diff contém apenas mudanças intencionais;
- não há credenciais, logs de debug, código comentado ou artefatos gerados;
- riscos, limitações e validações não executadas estão declarados com precisão.

Na entrega, informe de forma objetiva: resultado, arquivos alterados, verificações executadas e pendências reais. Nunca use “pronto”, “funcionando” ou “testes passando” sem evidência atual.
