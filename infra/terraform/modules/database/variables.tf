variable "compartment_id" {
  description = "OCID do compartment onde a Autonomous Database será criada."
  type        = string
}

variable "db_name" {
  description = "Nome curto do banco (letras e números, até 14 caracteres, começando com letra)."
  type        = string
  default     = "VISTORIADB"

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9]{0,13}$", var.db_name))
    error_message = "db_name deve começar com letra, conter só letras/números e ter no máximo 14 caracteres."
  }
}

variable "display_name" {
  description = "Nome de exibição da Autonomous Database no console."
  type        = string
  default     = "vistoria-mvp-db"
}

variable "admin_password" {
  description = "Senha do usuário ADMIN da Autonomous Database. Nunca versionar em texto plano."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.admin_password) >= 12 && length(var.admin_password) <= 30
    error_message = "admin_password deve ter entre 12 e 30 caracteres (requisito da Autonomous Database)."
  }
}

variable "db_version" {
  description = "Versão do Oracle Database. Confirmar no console se a região escolhida já suporta 23ai (AI Vector Search) antes de fixar — ver docs/spec-terraform.md. Não usado nesta fase (sem RAG), mas registrado para não travar decisão futura."
  type        = string
  default     = "23ai"
}
