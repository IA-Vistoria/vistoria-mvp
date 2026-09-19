terraform {
  required_version = ">= 1.8, < 2.0"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 9.0"
    }
  }
}

# Credenciais do provider NÃO ficam no código: em ~/.oci/config local de quem
# roda o apply (ver docs/spec-ambiente-dev.md). Na VM, o backend usa instance
# principal, não o Terraform CLI.
provider "oci" {
  region              = var.region
  config_file_profile = var.oci_config_profile
}

module "iam" {
  source = "../../modules/iam"

  tenancy_ocid       = var.tenancy_ocid
  compartment_name   = var.compartment_name
  devs_group_name    = var.devs_group_name
  dynamic_group_name = var.dynamic_group_name
  bucket_name        = var.bucket_name
}

module "network" {
  source = "../../modules/network"

  compartment_id   = module.iam.compartment_id
  project_name     = var.project_name
  vcn_cidr         = var.vcn_cidr
  subnet_cidr      = var.subnet_cidr
  ssh_allowed_cidr = var.ssh_allowed_cidr
}

module "storage" {
  source = "../../modules/storage"

  compartment_id = module.iam.compartment_id
  bucket_name    = var.bucket_name
}

module "database" {
  source = "../../modules/database"

  compartment_id = module.iam.compartment_id
  db_name        = var.db_name
  display_name   = var.db_display_name
  admin_password = var.db_admin_password
  db_version     = var.db_version
}

module "compute" {
  source = "../../modules/compute"

  compartment_id         = module.iam.compartment_id
  subnet_id              = module.network.subnet_id
  nsg_ids                = [module.network.nsg_id]
  display_name           = var.instance_display_name
  instance_shape         = var.instance_shape
  instance_ocpus         = var.instance_ocpus
  instance_memory_in_gbs = var.instance_memory_in_gbs
  ssh_public_key         = var.ssh_public_key
}
