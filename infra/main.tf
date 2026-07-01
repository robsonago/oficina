terraform {
  required_version = ">= 1.6"

  required_providers {
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.4"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.0"
    }
  }
}

provider "kind" {}

# Credenciais diretas do kind_cluster — sem depender de ~/.kube/config
provider "kubernetes" {
  host                   = module.cluster.endpoint
  client_certificate     = module.cluster.client_certificate
  client_key             = module.cluster.client_key
  cluster_ca_certificate = module.cluster.cluster_ca_certificate
}

# ──────────────────────────────────────────
# Módulo: Cluster kind
# Provisiona o cluster Kubernetes local
# ──────────────────────────────────────────
module "cluster" {
  source       = "./modules/cluster"
  cluster_name = var.cluster_name
}

# ──────────────────────────────────────────
# Módulo: Banco de dados
# Provisiona namespace, secrets, configmap,
# PVC e o deployment do PostgreSQL
# ──────────────────────────────────────────
module "database" {
  source      = "./modules/database"
  namespace   = var.namespace
  db_username = var.db_username
  db_password = var.db_password
  jwt_secret  = var.jwt_secret

  depends_on = [module.cluster]
}

# ──────────────────────────────────────────
# Módulo: Aplicação
# Provisiona Mailpit, Deployment da app,
# Service da app e HPA
# ──────────────────────────────────────────
module "application" {
  source            = "./modules/application"
  namespace         = var.namespace
  app_image         = var.app_image
  ghcr_username     = var.ghcr_username
  ghcr_token        = var.ghcr_token
  cluster_name      = var.cluster_name
  build_local_image = var.build_local_image

  depends_on = [module.database]
}
