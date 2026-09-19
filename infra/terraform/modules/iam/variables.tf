variable "tenancy_ocid" {
  description = "OCID do tenant raiz da OCI. Compartment e grupos são criados sob ele (grupos e dynamic groups só podem existir no compartment raiz do tenant)."
  type        = string
}

variable "compartment_name" {
  description = "Nome do compartment dedicado ao projeto."
  type        = string
  default     = "vistoria-mvp-dev"
}

variable "compartment_description" {
  description = "Descrição do compartment do projeto."
  type        = string
  default     = "Compartment único de desenvolvimento e teste do VistoApto (vistoria predial)."
}

variable "devs_group_name" {
  description = "Nome do grupo de desenvolvedores que acessam via chave de API pessoal."
  type        = string
  default     = "vistoria-mvp-devs"
}

variable "dynamic_group_name" {
  description = "Nome do dynamic group que inclui a VM do projeto (autenticação via instance principal)."
  type        = string
  default     = "vistoria-mvp-vm-dg"
}

variable "bucket_name" {
  description = "Nome do bucket de Object Storage usado nas policies de storage (o bucket em si é criado pelo módulo storage)."
  type        = string
  default     = "vistoria-fotos"
}
