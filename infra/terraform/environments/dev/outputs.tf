output "compartment_id" {
  description = "OCID do compartment do projeto."
  value       = module.iam.compartment_id
}

output "dynamic_group_id" {
  description = "OCID do dynamic group da VM (para conferência manual da policy)."
  value       = module.iam.dynamic_group_id
}

output "bucket_name" {
  description = "Nome do bucket de fotos de vistoria."
  value       = module.storage.bucket_name
}

output "autonomous_database_id" {
  description = "OCID da Autonomous Database."
  value       = module.database.autonomous_database_id
}

output "db_connection_strings" {
  description = "Connection strings da Autonomous Database (alimenta DB_URL do backend)."
  value       = module.database.connection_strings
  sensitive   = true
}

output "instance_public_ip" {
  description = "IP público da instância Compute (usado pelo inventário do Ansible)."
  value       = module.compute.public_ip
}
