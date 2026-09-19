output "bucket_name" {
  description = "Nome do bucket de fotos de vistoria."
  value       = oci_objectstorage_bucket.this.name
}

output "namespace" {
  description = "Namespace do Object Storage do tenant."
  value       = data.oci_objectstorage_namespace.this.namespace
}
