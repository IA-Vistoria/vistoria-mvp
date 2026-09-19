output "compartment_id" {
  description = "OCID do compartment do projeto."
  value       = oci_identity_compartment.this.id
}

output "compartment_name" {
  description = "Nome do compartment do projeto."
  value       = oci_identity_compartment.this.name
}

output "devs_group_id" {
  description = "OCID do grupo de desenvolvedores."
  value       = oci_identity_group.devs.id
}

output "dynamic_group_id" {
  description = "OCID do dynamic group da VM (para conferência manual da policy)."
  value       = oci_identity_dynamic_group.vm.id
}

output "dynamic_group_name" {
  description = "Nome do dynamic group da VM."
  value       = oci_identity_dynamic_group.vm.name
}
