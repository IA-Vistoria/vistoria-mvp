#!/usr/bin/env bash
# Gera inventory/dev.ini a partir do output do Terraform, para o IP da VM
# nunca precisar ser digitado à mão (ver docs/spec-ansible.md).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TF_DIR="$SCRIPT_DIR/../../terraform/environments/dev"

SSH_USER="${ANSIBLE_SSH_USER:-ubuntu}"
SSH_KEY="${ANSIBLE_SSH_PRIVATE_KEY_FILE:-~/.ssh/id_ed25519}"

if ! command -v terraform >/dev/null 2>&1; then
  echo "Erro: terraform CLI não encontrado no PATH." >&2
  exit 1
fi

IP="$(terraform -chdir="$TF_DIR" output -raw instance_public_ip)"

cat > "$SCRIPT_DIR/dev.ini" <<EOF
# Gerado automaticamente por generate-dev-ini.sh a partir do output do
# Terraform. Não editar à mão — rode este script de novo após terraform apply.
[app_servers]
vistoria-mvp-vm ansible_host=$IP

[app_servers:vars]
ansible_user=$SSH_USER
ansible_ssh_private_key_file=$SSH_KEY
EOF

echo "inventory/dev.ini gerado com IP $IP"
