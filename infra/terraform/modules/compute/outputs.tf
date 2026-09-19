output "instance_id" {
  description = "OCID da instância Compute."
  value       = oci_core_instance.this.id
}

output "public_ip" {
  description = "IP público da instância Compute (usado pelo inventário do Ansible)."
  value       = oci_core_instance.this.public_ip
}
