variable "compartment_id" {
  description = "OCID do compartment onde o bucket será criado."
  type        = string
}

variable "bucket_name" {
  description = "Nome do bucket de fotos de vistoria."
  type        = string
  default     = "vistoria-fotos"
}
