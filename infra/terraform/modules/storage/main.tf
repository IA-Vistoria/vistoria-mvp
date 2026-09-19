data "oci_objectstorage_namespace" "this" {
  compartment_id = var.compartment_id
}

# Sem replicação cross-region nesta fase (não necessário para o MVP de teste).
resource "oci_objectstorage_bucket" "this" {
  # checkov:skip=CKV_OCI_9: sem vault/KMS neste MVP (fora do escopo de
  #   docs/spec-terraform.md); o bucket já é criptografado em repouso com a
  #   chave gerenciada pela Oracle por padrão.
  compartment_id        = var.compartment_id
  namespace             = data.oci_objectstorage_namespace.this.namespace
  name                  = var.bucket_name
  storage_tier          = "Standard"
  access_type           = "NoPublicAccess"
  versioning            = "Enabled"
  object_events_enabled = true
}
