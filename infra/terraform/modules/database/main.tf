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
    ignore_changes  = [admin_password]
  }
}
