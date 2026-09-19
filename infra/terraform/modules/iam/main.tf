# Compartment dedicado ao projeto, abaixo do compartment raiz do tenant.
resource "oci_identity_compartment" "this" {
  compartment_id = var.tenancy_ocid
  name           = var.compartment_name
  description    = var.compartment_description
  enable_delete  = true
}

# Grupos e dynamic groups só podem ser criados no compartment raiz do tenant.
resource "oci_identity_group" "devs" {
  compartment_id = var.tenancy_ocid
  name           = var.devs_group_name
  description    = "Desenvolvedores do VistoApto com acesso ao ambiente de dev via chave de API pessoal."
}

resource "oci_identity_dynamic_group" "vm" {
  compartment_id = var.tenancy_ocid
  name           = var.dynamic_group_name
  description    = "VM(s) do VistoApto no compartment do projeto, autenticadas via instance principal."
  matching_rule  = "ALL {instance.compartment.id = '${oci_identity_compartment.this.id}'}"
}

# NOTA: o nome exato da família de recurso do Generative AI ("generative-ai-family")
# precisa ser reconferido na documentação de IAM Policy Reference da OCI no momento
# do apply — esse nome muda conforme o serviço amadurece (ver docs/spec-terraform.md).
resource "oci_identity_policy" "devs" {
  compartment_id = var.tenancy_ocid
  name           = "${var.devs_group_name}-policy"
  description    = "Acesso do grupo de devs a storage, Generative AI e Autonomous Database no compartment do projeto."

  statements = [
    "Allow group ${oci_identity_group.devs.name} to manage objects in compartment ${oci_identity_compartment.this.name} where target.bucket.name = '${var.bucket_name}'",
    "Allow group ${oci_identity_group.devs.name} to use generative-ai-family in compartment ${oci_identity_compartment.this.name}",
    "Allow group ${oci_identity_group.devs.name} to manage autonomous-database-family in compartment ${oci_identity_compartment.this.name}",
  ]
}

resource "oci_identity_policy" "vm" {
  compartment_id = var.tenancy_ocid
  name           = "${var.dynamic_group_name}-policy"
  description    = "Acesso da VM (instance principal) a storage, Generative AI e Autonomous Database no compartment do projeto."

  statements = [
    "Allow dynamic-group ${oci_identity_dynamic_group.vm.name} to manage objects in compartment ${oci_identity_compartment.this.name} where target.bucket.name = '${var.bucket_name}'",
    "Allow dynamic-group ${oci_identity_dynamic_group.vm.name} to use generative-ai-family in compartment ${oci_identity_compartment.this.name}",
    "Allow dynamic-group ${oci_identity_dynamic_group.vm.name} to manage autonomous-database-family in compartment ${oci_identity_compartment.this.name}",
  ]
}
