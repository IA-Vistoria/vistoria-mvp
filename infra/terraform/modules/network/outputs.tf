output "vcn_id" {
  description = "OCID da VCN."
  value       = oci_core_vcn.this.id
}

output "subnet_id" {
  description = "OCID da subnet pública."
  value       = oci_core_subnet.public.id
}

output "nsg_id" {
  description = "OCID do Network Security Group público (80/443/22)."
  value       = oci_core_network_security_group.public.id
}
