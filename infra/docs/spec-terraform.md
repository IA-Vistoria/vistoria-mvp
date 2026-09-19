# Spec: infraestrutura (Terraform)

## Objetivo

Provisionar, como código, um ambiente único de desenvolvimento e teste na OCI, compartilhado por todo o time, cobrindo: identidade e acesso, rede, armazenamento de fotos, banco de dados, e a VM que vai rodar os containers Docker. Nenhum serviço de IA (Vision, Generative AI) precisa ser criado por aqui além da concessão de permissão para usá-los, eles são consumidos via API pelo backend.

Este ambiente é o mesmo usado pelos desenvolvedores (via chave de API pessoal) e pela VM (via instance principal). Não existe, nesta fase, um compartment de produção separado.

## Pré-requisitos que o executor (Claude Code ou humano) precisa confirmar antes de rodar

- Tenant OCI e credenciais do usuário administrador que vai rodar o `terraform apply` pela primeira vez (essas credenciais não vão para o código, só para o `provider` local).
- Região OCI escolhida (definir como variável, não fixar no código).
- Confirmar no console se a Autonomous Database Always Free da região escolhida já suporta AI Vector Search (recurso 23ai). Isso não é usado nesta fase (não há RAG), mas vale registrar a confirmação para não travar decisão futura.

## Estrutura de diretórios esperada

```
infra/terraform/
  modules/
    iam/
    network/
    storage/
    database/
    compute/
  environments/
    dev/
      main.tf
      variables.tf
      outputs.tf
      terraform.tfvars.example
```

Cada módulo recebe inputs via variáveis, nenhum nome de recurso hardcoded dentro do módulo, só nos arquivos de `environments/dev`.

## Módulo `iam`

Responsável por: compartment, grupos, dynamic group, policies.

> **Atualização:** o OCI Vision saiu do escopo. A análise de imagem passou a ser feita por um modelo multimodal dentro do próprio OCI Generative AI (Llama 3.2 90B Vision), então não existe mais treinamento de modelo customizado nem policy separada para Vision. Tudo que a aplicação precisa da OCI, além de storage e banco, está coberto pela família `generative-ai-family`.

Recursos esperados (nomes de resource type do provider `oci`, conferir a versão atual do provider antes de aplicar, pois nomes de atributos mudam entre versões):

- `oci_identity_compartment` — um compartment dedicado ao projeto (ex: `vistoria-mvp-dev`), abaixo do compartment raiz do tenant.
- `oci_identity_group` — grupo `vistoria-mvp-devs`, para os desenvolvedores que vão usar chave de API pessoal.
- `oci_identity_dynamic_group` — grupo dinâmico que inclui a VM do projeto, com regra de correspondência pelo compartment (`ALL {instance.compartment.id = '<ocid do compartment>'}`).
- `oci_identity_policy` — duas policies, uma para o grupo de devs e outra para o dynamic group. Ver seção "Policies necessárias" abaixo.

### Policies necessárias

Escritas em linguagem natural de policy da OCI (o Terraform só encapsula essas strings, não valide sintaxe de cabeça, teste no console antes de commitar):

Para o dynamic group da VM (produção do próprio serviço rodando):
```
Allow dynamic-group vistoria-mvp-vm-dg to manage objects in compartment vistoria-mvp-dev where target.bucket.name = 'vistoria-fotos'
Allow dynamic-group vistoria-mvp-vm-dg to use generative-ai-family in compartment vistoria-mvp-dev
Allow dynamic-group vistoria-mvp-vm-dg to manage autonomous-database-family in compartment vistoria-mvp-dev
```

Para o grupo de desenvolvedores (mesmo escopo, mas amarrado a usuários, não à VM):
```
Allow group vistoria-mvp-devs to manage objects in compartment vistoria-mvp-dev where target.bucket.name = 'vistoria-fotos'
Allow group vistoria-mvp-devs to use generative-ai-family in compartment vistoria-mvp-dev
Allow group vistoria-mvp-devs to manage autonomous-database-family in compartment vistoria-mvp-dev
```

O nome exato da família de recurso do Generative AI (`generative-ai-family`) precisa ser conferido na documentação de IAM policy reference da OCI no momento da implementação, esse nome muda conforme o serviço amadurece e eu não tenho certeza de que está atualizado.

## Módulo `network`

- Uma VCN dedicada ao compartment, uma subnet pública (a VM precisa ser alcançável na porta 443).
- Security list ou network security group liberando entrada em 80, 443, e 22 (SSH, restrito a um CIDR conhecido de IP, nunca `0.0.0.0/0` para SSH).
- Internet gateway e route table associados.

## Módulo `storage`

- `oci_objectstorage_bucket` chamado `vistoria-fotos`, no compartment do projeto.
- Sem replicação cross-region nesta fase (não é necessário para um MVP de teste).

## Módulo `database`

- `oci_database_autonomous_database`, camada Always Free (`is_free_tier = true`), workload type transacional.
- Guardar usuário/senha de admin em variável marcada como `sensitive = true`, nunca em texto plano no `.tfvars` versionado.
- Esta instância substitui o Postgres que o código da aplicação ainda assume como banco-alvo de produção. O ajuste do lado da aplicação (driver JDBC, dialeto do Flyway, migrations) está descrito em `spec-backend.md`, não é responsabilidade deste módulo, mas o output de connection string daqui é o que alimenta a variável `DB_URL` do backend.

## Módulo `compute`

- `oci_core_instance`, shape `VM.Standard.A1.Flex` (Ampere), dentro do limite Always Free (até 4 OCPU e 24 GB combinados entre todas as instâncias Always Free do tenant).
- Imagem: Ubuntu ou Oracle Linux mais recente disponível como Always Free eligible.
- `cloud-init` mínimo, só o suficiente para o Ansible conseguir conectar depois (usuário, chave SSH pública). Toda a configuração de fato (Docker, aplicação) fica a cargo do Ansible, não do Terraform.

## Outputs esperados de `environments/dev`

- OCID do compartment
- Nome do bucket
- Connection string / OCID da Autonomous Database
- IP público da instância Compute
- OCID do dynamic group (para conferência manual da policy)

## Critério de pronto

- `terraform plan` roda sem erro e mostra exatamente os recursos acima, nada a mais.
- `terraform apply` seguido de login no console confirma: compartment criado, bucket vazio existente, Autonomous DB acessível, VM com IP público responde a ping/SSH.
- Nenhuma credencial de longa duração (API key, senha) commitada no repositório.
