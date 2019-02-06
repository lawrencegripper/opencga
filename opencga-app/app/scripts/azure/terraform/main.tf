variable "location" {}
variable "opencga_image" {}
variable "iva_image" {}
variable "opencga_init_image" {}
variable "batch_container_image" {}
variable "catalog_secret_key" {}
variable "opencga_admin_password" {}
variable "ssh_pub_key" {}
variable "existing_resource_group" {
  default = false
}
variable "lets_encrypt_email_address" {
  description = "This is the email address used when obtaining SSL certs for the solution. This should be a valid email for the solution admin."
}
variable "resource_group_prefix" {
  default = "opencga"
}
variable "state_storage_account_name" {}
variable "state_storage_container_name" {}
variable "state_storage_blob_name" {
  default = "terraform.tfstate"
}
variable "log_analytics_sku" {
  default = "pergb2018"
  description = "Sets the SKU to use for log analytics. See FAQ in README.md for details."
}


terraform {
  backend "azurerm" {
  }
}

data "terraform_remote_state" "state" {
  backend = "azurerm"
  config {
    storage_account_name  = "${var.state_storage_account_name}"
    container_name        = "${var.state_storage_container_name}"
    key                   = "${var.state_storage_blob_name}"
  }
}

// Pin to a version of the terraform provider to prevent breaking changes of future releases
// work should be undertaken to update this from time-to-time to track the lastest release
provider "azurerm" {
  version = "=1.21.0"
}

locals {
  hdinsight_resource_group_name = "${var.existing_resource_group ? var.resource_group_prefix : format("%s-%s", var.resource_group_prefix,"hdinsight")}"
  storage_resource_group_name = "${var.existing_resource_group ? var.resource_group_prefix : format("%s-%s", var.resource_group_prefix,"storage")}"
  batch_resource_group_name = "${var.existing_resource_group ? var.resource_group_prefix : format("%s-%s", var.resource_group_prefix,"batch")}"
  mongo_resource_group_name = "${var.existing_resource_group ? var.resource_group_prefix : format("%s-%s", var.resource_group_prefix,"mongo")}"
  web_resource_group_name = "${var.existing_resource_group ? var.resource_group_prefix : format("%s-%s", var.resource_group_prefix,"web")}"
}

resource "azurerm_resource_group" "opencga" {
  count = "${var.existing_resource_group ? 0 : 1}"
  name     = "${var.resource_group_prefix}"
  location = "${var.location}"
}

module "loganalytics" {
  source = "./loganalytics"

  location            = "${var.location}"
  resource_group_name = "${local.hdinsight_resource_group_name}"
  log_analytics_sku = "${var.log_analytics_sku}"
}

module "hdinsight" {
  source = "./hdinsight"

  virtual_network_id        = "${azurerm_virtual_network.opencga.id}"
  virtual_network_subnet_id = "${azurerm_subnet.hdinsight.id}"

  location            = "${var.location}"
  resource_group_name = "${local.hdinsight_resource_group_name}"
  create_resource_group = "${var.existing_resource_group ? 0 : 1}"
}

module "azurefiles" {
  source = "./azurefiles"

  location            = "${var.location}"
  resource_group_name = "${local.storage_resource_group_name}"
  create_resource_group = "${var.existing_resource_group ? 0 : 1}"
}

module "azurebatch" {
  source = "./azurebatch"

  location            = "${var.location}"
  resource_group_name = "${local.batch_resource_group_name}"
  create_resource_group = "${var.existing_resource_group ? 0 : 1}"

  virtual_network_subnet_id = "${azurerm_subnet.batch.id}"

  mount_args = "azurefiles ${module.azurefiles.storage_account_name},${module.azurefiles.share_name},${module.azurefiles.storage_key}"
}

module "mongo" {
  source = "./mongo"

  location            = "${var.location}"
  resource_group_name = "${local.mongo_resource_group_name}"
  create_resource_group = "${var.existing_resource_group ? 0 : 1}"

  virtual_network_subnet_id = "${azurerm_subnet.mongo.id}"
  admin_username            = "opencga"
  ssh_key_data              = "${var.ssh_pub_key}"

  email_address = "${var.lets_encrypt_email_address}"
  cluster_size  = 3
}

resource "random_string" "webservers_dns_prefix" {
  keepers = {
    # Generate a new id each time we switch to a new resource group
    group_name = "${local.web_resource_group_name}"
  }

  length  = 8
  upper   = false
  special = false
  number  = false
}

locals {
  webservers_url = "http://${random_string.webservers_dns_prefix.result}.${var.location}.cloudapp.azure.com"
}

module "webservers" {
  source = "./webservers"

  location            = "${var.location}"
  resource_group_name = "${local.web_resource_group_name}"
  create_resource_group = "${var.existing_resource_group ? 0 : 1}"

  virtual_network_subnet_id = "${azurerm_subnet.web.id}"

  mount_args = "azurefiles ${module.azurefiles.storage_account_name},${module.azurefiles.share_name},${module.azurefiles.storage_key}"

  admin_username = "opencga"
  ssh_key_data   = "${var.ssh_pub_key}"

  opencga_image = "${var.opencga_image}"
  iva_image     = "${var.iva_image}"

  dns_prefix = "${random_string.webservers_dns_prefix.result}"
}

data "template_file" "opencga_init_cmd" {
  template = <<EOF
docker run --mount type=bind,src=/media/primarynfs,dst=/opt/volume
-e OPENCGA_PASS=$${opencga_password}
-e HBASE_SSH_DNS=$${hdinsight_ssh_dns}
-e HBASE_SSH_USER=$${hdinsight_ssh_username}
-e HBASE_SSH_PASS="$${hdinsight_ssh_password}"
-e SEARCH_HOSTS=$${solr_hosts_csv}
-e CELLBASE_HOSTS=$${mongo_hosts_csv} 
-e CLINICAL_HOSTS=$${solr_hosts_csv}
-e CATALOG_DATABASE_HOSTS=$${mongo_hosts_csv}
-e CATALOG_DATABASE_USER=$${mongo_user}
-e CATALOG_DATABASE_PASSWORD="$${mongo_password}"
-e CATALOG_SEARCH_HOSTS=$${solr_hosts_csv}
-e CATALOG_SEARCH_USER=$${solr_user}
-e CATALOG_SEARCH_PASSWORD=$${solr_password}
-e REST_HOST="$${rest_host}"
-e GRPC_HOST="$${grpc_host}"
-e BATCH_EXEC_MODE=AZUREbarr
-e BATCH_ACCOUNT_NAME=$${batch_account_name}
-e BATCH_ACCOUNT_KEY="$${batch_account_key}"
-e BATCH_ENDPOINT=$${batch_account_endpoint}
-e BATCH_POOL_ID=$${batch_account_pool_id}
-e BATCH_DOCKER_ARGS="$${batch_docker_args}"
-e BATCH_DOCKER_IMAGE=$${batch_container_image}
-e BATCH_MAX_CONCURRENT_JOBS=1
 $${opencga_init_image} $${catalog_secret_key}
      EOF

  vars {
    opencga_password       = "${var.opencga_admin_password}"
    hdinsight_ssh_dns      = "${module.hdinsight.cluster_dns}"
    hdinsight_ssh_username = "${module.hdinsight.cluster_username}"
    hdinsight_ssh_password = "${module.hdinsight.cluster_password}"
    solr_hosts_csv         = "todo"
    solr_user              = "todo"
    solr_password          = "todo"
    mongo_hosts_csv        = "${join(",", module.mongo.replica_dns_names)}"
    mongo_user             = "${module.mongo.mongo_username}"
    mongo_password         = "${module.mongo.mongo_password}"
    rest_host              = "${local.webservers_url}"
    grpc_host              = "${local.webservers_url}"
    batch_account_name     = "${module.azurebatch.batch_account_name}"
    batch_account_key      = "${module.azurebatch.batch_account_key}"
    batch_account_endpoint = "${module.azurebatch.batch_account_endpoint}"
    batch_account_pool_id  = "${module.azurebatch.batch_account_pool_id}"
    batch_docker_args      = "--mount type=bind,src=/media/primarynfs/conf,dst=/opt/opencga/conf,readonly --mount type=bind,src=/media/primarynfs/sessions,dst=/opt/opencga/sessions --mount type=bind,src=/media/primarynfs/variants,dst=/opt/opencga/variants --rm"
    batch_container_image  = "${var.batch_container_image}"
    opencga_init_image     = "${var.opencga_init_image}"
    catalog_secret_key     = "${var.catalog_secret_key}"
  }
}

module "daemonvm" {
  source = "./daemonvm"

  location            = "${var.location}"
  resource_group_name = "${var.resource_group_prefix}"
  create_resource_group = "${var.existing_resource_group ? 0 : 1}"
  log_analytics_workspace_id = "${module.loganalytics.workspace_id}"
  virtual_network_subnet_id = "${azurerm_subnet.daemonvm.id}"

  mount_args = "azurefiles ${module.azurefiles.storage_account_name},${module.azurefiles.share_name},${module.azurefiles.storage_key}"

  admin_username = "opencga"
  ssh_key_data   = "${var.ssh_pub_key}"

  opencga_image          = "${var.opencga_image}"
  opencga_init_image     = "${var.opencga_init_image}"
  init_cmd               = "${replace(data.template_file.opencga_init_cmd.rendered, "/\\n/", " ")}"
  opencga_admin_password = "${var.opencga_admin_password}"
}
