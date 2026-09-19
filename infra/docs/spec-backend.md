# Spec: backend, frontend e containers Docker (v2)

> Esta versão substitui a spec anterior. Mudanças: stack real é Java 21 + Spring Boot (não Node), banco-alvo é Autonomous DB (não Postgres), e a análise de imagem usa um único modelo multimodal do OCI Generative AI (não há mais OCI Vision customizado nem duas chamadas separadas).

## Ponto de partida: o repositório já existe

O time já tem um repositório (`vistoria-predial`) com boa parte da estrutura pronta: autenticação JWT com papéis `ROLE_CLIENTE` e `ROLE_ENGENHEIRO`, upload de imagem, fluxo de submissão e homologação, e duas interfaces de abstração já corretas:

```java
public interface StorageService {
    String store(MultipartFile file, String fileName);
    void delete(String relativePath);
}

public interface IaIntegrationService {
    String analisarImagens(List<String> imageUrls);
}
```

Este documento descreve o que falta implementar em cima dessa base, não uma reescrita.

## O que muda: análise de imagem numa chamada só

Não existe mais um serviço de detecção (Vision) separado de um serviço de redação (texto). O modelo multimodal do OCI Generative AI (Llama 3.2 90B Vision, confirmado na tabela de preços da OCI) recebe a foto diretamente e já devolve achados estruturados e descrição, no mesmo formato que o teste local com Qwen3-VL já validou (`analysis_id`, `image_quality`, `areas` com `area`, `issue_type`, `severity`, `confidence`, `recommendation`, `location`, e um `overall_summary`).

Como a interface `IaIntegrationService.analisarImagens` já recebe a lista completa de URLs de uma vistoria e devolve uma única `String` (o pré-laudo), a implementação real deve, dentro dela:

1. Para cada URL de imagem, montar uma chamada multimodal ao Generative AI (a imagem entra no payload da requisição, não como texto).
2. Guardar o JSON estruturado que cada chamada devolve.
3. Juntar os achados de todas as fotos num texto final coerente, o `preLaudo` que é salvo em `Vistoria.preLaudoIa`. Essa junção pode ser feita em código (formatando o JSON de cada foto em texto), sem precisar de outra chamada de IA. Só vale usar uma segunda chamada de texto puro ao Generative AI se o resultado formatado em código ficar didaticamente pior que um texto corrido escrito por um modelo, o que deve ser avaliado depois de ver os primeiros resultados reais, não decidido de antemão.

## Pendência a validar antes de implementar

Não há confirmação de primeira mão do formato exato de request/response multimodal da Responses API da OCI para o Llama 3.2 90B Vision (como a imagem é codificada no payload, se aceita URL do Object Storage direto ou exige base64, limite de tamanho). Antes de escrever `OciGenAiIntegrationService`, fazer uma chamada de teste isolada (um script simples, fora do Spring Boot) contra esse endpoint com uma imagem de exemplo, do mesmo jeito que já foi validado localmente com o Qwen3-VL, só que contra o serviço real da OCI.

## Implementações reais a escrever

```
storage/
  OciObjectStorageService.java   implements StorageService
vistoria/application/
  OciGenAiIntegrationService.java implements IaIntegrationService
```

Nenhuma outra classe do projeto (controllers, `VistoriaService`, DTOs) precisa mudar. A troca de `MockIaIntegrationService` e de uma futura implementação local de `StorageService` pelas versões reais é feita via Spring `@Profile` ou uma propriedade de configuração (ex: `app.ia.provider=mock` vs `app.ia.provider=oci`), do mesmo jeito que o `VISION_MODE` fazia na spec anterior, só que agora decidindo entre mock e real de um serviço só.

## Autenticação com a OCI (equivalente ao `ociAuth.js` da spec anterior)

Um único `@Configuration` centraliza a decisão de como autenticar com o SDK da OCI:

- Se a variável de ambiente `OCI_AUTH_MODE=instance_principal` estiver definida (setada pelo Ansible só na VM), o bean usa `InstancePrincipalsAuthenticationDetailsProvider`.
- Caso contrário, usa `ConfigFileAuthenticationDetailsProvider`, lendo o `~/.oci/config` montado como volume no container (rodando no laptop de um dev).

`OciObjectStorageService` e `OciGenAiIntegrationService` recebem esse provider injetado, nenhum dos dois decide sozinho qual autenticação usar.

## Banco de dados: de Postgres para Autonomous DB

Mudanças necessárias no `pom.xml` e nas migrations, além da variável `DB_URL` já prevista em `application.properties`:

- Trocar a dependência `org.postgresql:postgresql` pelo driver JDBC da Oracle (`com.oracle.database.jdbc:ojdbc11` ou equivalente).
- Trocar `flyway-database-postgresql` pelo módulo de Oracle do Flyway. **Confirmar antes se esse suporte está na edição gratuita do Flyway na versão em uso**, isso mudou de edição algumas vezes nas últimas versões e não tenho certeza do estado atual.
- Corrigir `V2__create_vistoria_schema.sql`, que usa `AUTO_INCREMENT` (sintaxe H2/MySQL, incompatível tanto com Postgres quanto com Oracle). Trocar pelo padrão `GENERATED ALWAYS AS IDENTITY` já usado em `V1`, que é compatível com Oracle 12c+.
- Trocar as colunas `TEXT` (`pre_laudo_ia`, `parecer_engenheiro`) por `CLOB`, tipo equivalente no dialeto Oracle.
- Nos testes de integração, trocar o Testcontainers de `postgresql` pela imagem de Oracle correspondente, ou manter H2 para os testes que não precisam validar SQL específico do dialeto.

## `Dockerfile` do backend

Build em dois estágios: um estágio com Maven e JDK 21 rodando `./mvnw package`, e um estágio final só com JRE 21 copiando o `.jar` gerado. Isso mantém a imagem final pequena, sem o Maven e o cache de dependências dentro dela.

## `docker-compose.yml`

Mesma lógica da spec anterior, adaptada:

- Serviços: `nginx`, `frontend` (arquivos estáticos Vanilla, servidos pelo próprio Nginx ou por um container simples), `backend` (o `.jar` Spring Boot).
- Local: monta `~/.oci/config` como volume somente leitura no container do backend.
- VM: não monta nada, usa `OCI_AUTH_MODE=instance_principal` vindo do `.env` gerado pelo Ansible.
- Nenhuma porta de `backend` publicada diretamente no host, só o `nginx` publica 80/443.

## Critério de pronto

- Um dev roda `docker compose up` no laptop, com seu `.env` pessoal, cria uma vistoria, sobe uma foto, submete, e o pré-laudo salvo em `Vistoria.preLaudoIa` reflete uma chamada real ao Generative AI multimodal (não o texto fixo do mock).
- O mesmo fluxo funciona com `app.ia.provider=mock`, sem nenhuma chamada de rede, para desenvolvimento de tela sem depender da OCI.
- O mesmo `docker-compose.yml`, sem edição, sobe corretamente na VM via Ansible, usando instance principal.
- Uma migration nova, rodada contra a Autonomous DB real, aplica sem erro de sintaxe.
