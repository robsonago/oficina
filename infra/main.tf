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
  }
}

provider "kind" {}

# O provider kubernetes usa o contexto criado pelo módulo cluster
provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "kind-${var.cluster_name}"
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
