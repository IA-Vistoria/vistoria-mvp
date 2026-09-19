# --- Provider / autenticação (ver docs/spec-ambiente-dev.md) ---

variable "tenancy_ocid" {
  description = "OCID do tenant OCI onde o ambiente será criado."
  type        = string
}

variable "region" {
  description = "Região OCI escolhida para o ambiente de dev/teste (não fixar no código)."
  type        = string
}

variable "oci_config_profile" {
  description = "Perfil usado em ~/.oci/config por quem roda terraform apply."
  type        = string
  default     = "DEFAULT"
}

# --- IAM ---

variable "project_name" {
  description = "Prefixo usado no nome dos recursos de rede/compute."
  type        = string
  default     = "vistoria-mvp"
}

variable "compartment_name" {
  description = "Nome do compartment dedicado ao projeto."
  type        = string
  default     = "vistoria-mvp-dev"
}

variable "devs_group_name" {
  description = "Nome do grupo de desenvolvedores."
  type        = string
  default     = "vistoria-mvp-devs"
}

variable "dynamic_group_name" {
  description = "Nome do dynamic group da VM."
  type        = string
  default     = "vistoria-mvp-vm-dg"
}

# --- Network ---

variable "vcn_cidr" {
  description = "Bloco CIDR da VCN."
  type        = string
  default     = "10.0.0.0/16"
}

variable "subnet_cidr" {
  description = "Bloco CIDR da subnet pública."
  type        = string
  default     = "10.0.1.0/24"
}

variable "ssh_allowed_cidr" {
  description = "CIDR conhecido do time autorizado a acessar a porta 22. Nunca 0.0.0.0/0."
  type        = string
}

# --- Storage ---

variable "bucket_name" {
  description = "Nome do bucket de fotos de vistoria."
  type        = string
  default     = "vistoria-fotos"
}

# --- Database ---

variable "db_name" {
  description = "Nome curto da Autonomous Database."
  type        = string
  default     = "VISTORIADB"
}

variable "db_display_name" {
  description = "Nome de exibição da Autonomous Database."
  type        = string
  default     = "vistoria-mvp-db"
}

variable "db_version" {
  description = "Versão do Oracle Database. Confirmar suporte a 23ai/AI Vector Search na região escolhida antes do apply (ver docs/spec-terraform.md)."
  type        = string
  default     = "23ai"
}

variable "db_admin_password" {
  description = "Senha do usuário ADMIN da Autonomous Database. Definir em terraform.tfvars (nunca versionado) ou via TF_VAR_db_admin_password."
  type        = string
  sensitive   = true
}

# --- Compute ---

variable "ssh_public_key" {
  description = "Chave pública SSH usada pelo cloud-init da VM, para o Ansible conseguir conectar depois."
  type        = string
}

variable "instance_display_name" {
  description = "Nome de exibição da instância Compute."
  type        = string
  default     = "vistoria-mvp-vm"
}

variable "instance_shape" {
  description = "Shape Always Free eligible (Ampere)."
  type        = string
  default     = "VM.Standard.A1.Flex"
}

variable "instance_ocpus" {
  description = "OCPUs da instância (limite combinado Always Free: 4 OCPU / 24 GB por tenant)."
  type        = number
  default     = 2
}

variable "instance_memory_in_gbs" {
  description = "Memória em GB da instância."
  type        = number
  default     = 12
}
