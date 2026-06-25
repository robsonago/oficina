terraform {
  required_providers {
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.4"
    }
  }
}

# ──────────────────────────────────────────
# Recurso: Cluster Kubernetes local (kind)
#
# Provisiona um cluster de 1 nó com port
# mappings para acesso via localhost.
# ──────────────────────────────────────────
resource "kind_cluster" "this" {
  name = var.cluster_name

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"

      # Porta da API da aplicação
      extra_port_mappings {
        container_port = 30080
        host_port      = 30080
        protocol       = "TCP"
      }

      # Porta da UI do Mailhog
      extra_port_mappings {
        container_port = 30825
        host_port      = 30825
        protocol       = "TCP"
      }
    }
  }
}
