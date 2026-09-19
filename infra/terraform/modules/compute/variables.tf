variable "compartment_id" {
  description = "OCID do compartment onde a instância será criada."
  type        = string
}

variable "subnet_id" {
  description = "OCID da subnet pública onde a VM será conectada."
  type        = string
}

variable "nsg_ids" {
  description = "Lista de OCIDs de Network Security Groups a associar à VNIC da instância."
  type        = list(string)
  default     = []
}

variable "display_name" {
  description = "Nome de exibição da instância."
  type        = string
  default     = "vistoria-mvp-vm"
}

variable "instance_shape" {
  description = "Shape da instância. VM.Standard.A1.Flex (Ampere) é elegível ao Always Free."
  type        = string
  default     = "VM.Standard.A1.Flex"
}

variable "instance_ocpus" {
  description = "Quantidade de OCPUs (limite combinado Always Free: 4 OCPU / 24 GB por tenant)."
  type        = number
  default     = 2
}

variable "instance_memory_in_gbs" {
  description = "Memória em GB da instância."
  type        = number
  default     = 12
}

variable "ssh_public_key" {
  description = "Chave pública SSH injetada via cloud-init para o Ansible conseguir conectar depois."
  type        = string
}

variable "operating_system" {
  description = "Sistema operacional da imagem (Always Free eligible)."
  type        = string
  default     = "Canonical Ubuntu"
}

variable "operating_system_version" {
  description = "Versão do sistema operacional da imagem."
  type        = string
  default     = "22.04"
}
