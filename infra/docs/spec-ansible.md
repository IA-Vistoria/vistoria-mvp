# Spec: configuração da VM (Ansible)

## Objetivo

Depois que o Terraform cria a VM (mas antes de qualquer avaliação usá-la), o Ansible entra para deixá-la pronta para rodar os containers: instala o Docker, sobe o `docker-compose` da aplicação, e garante que ele reinicia sozinho se a VM reiniciar.

## Pré-requisito

O IP público da VM e o caminho da chave SSH usada no `cloud-init` do Terraform (ver spec-terraform.md). O inventário do Ansible pode ler o IP direto do output do Terraform, não deveria ser digitado à mão.

## Estrutura esperada

```
infra/ansible/
  inventory/
    dev.ini              # ou script dinâmico lendo output do terraform
  playbooks/
    site.yml
  roles/
    docker/
      tasks/main.yml
    app/
      tasks/main.yml
      templates/
        docker-compose.yml.j2
        .env.j2
```

## Role `docker`

- Instala Docker Engine e o plugin `docker compose` (não o binário standalone antigo `docker-compose`, que está descontinuado).
- Adiciona o usuário de deploy ao grupo `docker`.
- Garante que o serviço `docker` está habilitado e ativo (`systemd`).

## Role `app`

- Copia o repositório da aplicação para a VM (ou faz `git pull` de um branch definido, a decidir com o time se o deploy é por `git pull` ou por imagem já publicada em um registry).
- Gera o arquivo `.env` a partir de um template, preenchendo:
  - Nome do bucket, OCID do compartment, endpoint da Autonomous DB (não sensíveis, podem vir de variável do Ansible).
  - Nenhuma chave de API aqui. A VM não usa chave de API, ela se autentica via instance principal, então o backend precisa detectar que está rodando dentro da OCI e usar esse método automaticamente (ver spec-backend.md, seção de autenticação).
- Sobe o `docker compose up -d` com o compose gerado.
- Registra um serviço `systemd` (`vistoria-app.service`) que roda `docker compose up -d` no boot, para o ambiente voltar sozinho depois de qualquer reinício da VM.

## Segurança

- A role `app` nunca deve escrever uma API key da OCI em nenhum arquivo na VM. Se em algum teste isso for necessário temporariamente, deve ser removido antes de considerar a tarefa concluída.
- Portas expostas para fora da VM: só 80 e 443 (via Nginx). As portas internas do frontend e do backend não devem ser publicadas no host, só acessíveis dentro da rede Docker interna.

## Critério de pronto

- Rodar `ansible-playbook -i inventory/dev.ini playbooks/site.yml` de uma máquina limpa termina sem erro.
- Depois de rodar, `docker compose ps` na VM mostra os três serviços (nginx, frontend, backend) com status `running`.
- Reiniciar a VM (`sudo reboot`) e, sem qualquer intervenção manual, os três serviços voltam sozinhos dentro de um ou dois minutos.
- Acessar `http://<ip-da-vm>` (ou o domínio, se já configurado) mostra o frontend.
