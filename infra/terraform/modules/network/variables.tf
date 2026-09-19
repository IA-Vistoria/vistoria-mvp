variable "compartment_id" {
  description = "OCID do compartment onde a rede será criada."
  type        = string
}

variable "project_name" {
  description = "Prefixo usado no nome dos recursos de rede."
  type        = string
  default     = "vistoria-mvp"
}

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
  description = "CIDR autorizado a acessar a porta 22 (SSH) da VM. Nunca 0.0.0.0/0."
  type        = string

  validation {
    condition     = var.ssh_allowed_cidr != "0.0.0.0/0"
    error_message = "ssh_allowed_cidr não pode ser 0.0.0.0/0 — restrinja a um CIDR conhecido do time."
  }
}
