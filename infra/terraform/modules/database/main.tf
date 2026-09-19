# Camada Always Free — cpu_core_count e data_storage_size_in_tbs são fixos
# em 1 pela própria OCI quando is_free_tier = true.
resource "oci_database_autonomous_database" "this" {
  compartment_id           = var.compartment_id
  db_name                  = var.db_name
  display_name             = var.display_name
  admin_password           = var.admin_password
  cpu_core_count           = 1
  data_storage_size_in_tbs = 1
  db_workload              = "OLTP"
  db_version               = var.db_version
  is_free_tier             = true
  license_model            = "LICENSE_INCLUDED"

  lifecycle {
    prevent_destroy = true
    # admin_password: nunca reaplicar o valor do tfvars por cima de uma
    # eventual rotação manual. cpu_core_count: a API do Autonomous AI
    # Database (26ai) Always Free reporta 0 de volta e rejeita qualquer
    # tentativa de "corrigir" para 1 ("feature not supported in an Always
    # Free Autonomous AI Database") — deixa a OCI gerenciar esse campo.
    ignore_changes = [admin_password, cpu_core_count]
  }
}
