# Spec: ambiente de cada desenvolvedor

## Objetivo

Deixar claro o passo a passo que cada pessoa do time segue para conseguir rodar a aplicação localmente conectada ao ambiente real de desenvolvimento e teste na OCI (não um ambiente mockado, não uma cópia pessoal de infraestrutura).

## Passo a passo esperado

1. O administrador do projeto adiciona o usuário OCI da pessoa ao grupo `vistoria-mvp-devs` (criado pelo Terraform, ver spec-terraform.md), pelo console ou por um comando único de CLI. Isso não é automatizado por Terraform porque a lista de pessoas muda com frequência maior que a infraestrutura em si.
2. A pessoa gera sua própria API key na sua conta OCI (console: perfil do usuário, "chaves de API"), o que gera um par de chaves e um arquivo `~/.oci/config` no computador dela.
3. A pessoa clona o repositório da aplicação e copia `.env.example` para `.env`, preenchendo o caminho do seu `~/.oci/config` e o `OCID` do compartment (esse OCID é o mesmo para todo o time, está documentado no output do Terraform).
4. A pessoa roda `docker compose up`. O backend sobe já autenticado contra o ambiente compartilhado de desenvolvimento.
5. A pessoa acessa `http://localhost` (porta publicada pelo Nginx local) e testa o fluxo.

## O que cada dev NÃO deveria precisar fazer

- Não precisa rodar nenhum modelo de IA localmente.
- Não precisa subir sua própria Autonomous DB, bucket, ou instância de Vision. Todos usam o mesmo ambiente compartilhado.
- Não precisa ter acesso de administrador do tenant OCI, só a permissão de grupo definida na policy.

## Cuidado a documentar no README do repositório

A chave de API pessoal nunca deve ser commitada, nem colocada dentro da imagem Docker durante o build (só montada como volume em tempo de execução). Adicionar `.oci/` e `*.pem` ao `.gitignore` do projeto como primeiro commit do repositório, antes de qualquer outro código.

## Critério de pronto

- Uma pessoa nova no time, seguindo só este documento, consegue sair de "conta OCI criada" para "vendo o frontend local com o backend respondendo" sem precisar perguntar nada a mais para o resto do time.
