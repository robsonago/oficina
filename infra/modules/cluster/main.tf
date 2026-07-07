terraform {
  required_providers {
    kind = {
      source  = "tehcyx/kind"
      version = "~> 0.4"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.0"
    }
  }
}

resource "kind_cluster" "this" {
  name = var.cluster_name

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"

      extra_port_mappings {
        container_port = 30080
        host_port      = 30080
        protocol       = "TCP"
      }

      extra_port_mappings {
        container_port = 30825
        host_port      = 30825
        protocol       = "TCP"
      }
    }
  }
}

# ──────────────────────────────────────────
# metrics-server — necessário para o HPA ler
# métricas de CPU e escalar automaticamente.
# Usa --kubelet-insecure-tls porque o kind
# não emite certificados TLS válidos para kubelet.
# ──────────────────────────────────────────
resource "null_resource" "metrics_server" {
  provisioner "local-exec" {
    command = <<-EOT
      kubectl --context "kind-${var.cluster_name}" apply \
        -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
      kubectl --context "kind-${var.cluster_name}" patch deployment metrics-server \
        -n kube-system \
        --type='json' \
        -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
    EOT
  }

  depends_on = [kind_cluster.this]
}
