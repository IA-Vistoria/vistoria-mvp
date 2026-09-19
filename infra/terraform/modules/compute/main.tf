data "oci_identity_availability_domains" "this" {
  compartment_id = var.compartment_id
}

# Imagem mais recente Always Free eligible — nunca hardcodear OCID de imagem.
data "oci_core_images" "latest" {
  compartment_id           = var.compartment_id
  operating_system         = var.operating_system
  operating_system_version = var.operating_system_version
  shape                    = var.instance_shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

# cloud-init mínimo: só usuário + chave SSH pública, para o Ansible conseguir
# conectar depois (toda a configuração de Docker/app fica a cargo do Ansible,
# ver docs/spec-ansible.md).
resource "oci_core_instance" "this" {
  # checkov:skip=CKV_OCI_4: is_pv_encryption_in_transit_enabled é setado no
  #   nível raiz (correto para create, conforme docs do provider oracle/oci —
  #   o bloco launch_options equivalente só vale para update). Checkov 3.3
  #   só reconhece o caminho legado em launch_options e não detecta isso.
  compartment_id                      = var.compartment_id
  availability_domain                 = data.oci_identity_availability_domains.this.availability_domains[0].name
  shape                               = var.instance_shape
  display_name                        = var.display_name
  is_pv_encryption_in_transit_enabled = true

  shape_config {
    ocpus         = var.instance_ocpus
    memory_in_gbs = var.instance_memory_in_gbs
  }

  source_details {
    source_type = "image"
    source_id   = data.oci_core_images.latest.images[0].id
  }

  create_vnic_details {
    subnet_id        = var.subnet_id
    assign_public_ip = true
    nsg_ids          = var.nsg_ids
  }

  # Só IMDSv2 (endpoints legados desabilitados).
  instance_options {
    are_legacy_imds_endpoints_disabled = true
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
  }

  lifecycle {
    ignore_changes = [source_details[0].source_id]
  }
}
