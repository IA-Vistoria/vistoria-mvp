output "autonomous_database_id" {
  description = "OCID da Autonomous Database."
  value       = oci_database_autonomous_database.this.id
}

output "db_name" {
  description = "Nome curto do banco."
  value       = oci_database_autonomous_database.this.db_name
}

output "connection_strings" {
  description = "Connection strings da Autonomous Database (alimenta a variável DB_URL do backend, ver docs/spec-backend.md)."
  value       = oci_database_autonomous_database.this.connection_strings
  sensitive   = true
}
