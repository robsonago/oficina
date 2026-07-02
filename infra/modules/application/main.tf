terraform {
  required_providers {
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

# ──────────────────────────────────────────
# Carrega a imagem da aplicação no cluster kind
# antes do Deployment ser criado.
#
# - build_local_image = true  (padrão, uso local):
#   builda a imagem a partir do Dockerfile local e
#   carrega no kind. Elimina a dependência do GHCR.
#   Espelha o que o scripts/kind-setup.sh já faz
#   manualmente.
# - build_local_image = false (uso no CI/CD): a
#   imagem já foi buildada e publicada no GHCR por
#   um job anterior do pipeline — aqui só é feito o
#   pull e o carregamento no kind, sem depender de
#   pull em tempo de execução pelos nós do cluster.
# ──────────────────────────────────────────
locals {
  build_image_cmd = <<-EOT
    set -e
    docker build -t "${var.app_image}" "${path.root}/.."
    kind load docker-image "${var.app_image}" --name "${var.cluster_name}"
  EOT

  pull_image_cmd = <<-EOT
    set -e
    docker pull "${var.app_image}"
    kind load docker-image "${var.app_image}" --name "${var.cluster_name}"
  EOT
}

resource "null_resource" "build_and_load_image" {
  triggers = {
    always_run = timestamp()
  }

  provisioner "local-exec" {
    command = var.build_local_image ? local.build_image_cmd : local.pull_image_cmd
  }
}

# ──────────────────────────────────────────
# Secret de autenticação no GHCR
# Necessário para pull da imagem em ambientes
# sem pré-carregamento via 'kind load'.
# Com token vazio, o secret existe mas não
# autentica funciona se a imagem já estiver
# carregada localmente (IfNotPresent).
# ──────────────────────────────────────────
resource "kubernetes_secret" "ghcr" {
  metadata {
    name      = "ghcr-secret"
    namespace = var.namespace
  }

  type = "kubernetes.io/dockerconfigjson"

  data = {
    ".dockerconfigjson" = jsonencode({
      auths = {
        "ghcr.io" = {
          username = var.ghcr_username
          password = var.ghcr_token
          auth     = base64encode("${var.ghcr_username}:${var.ghcr_token}")
        }
      }
    })
  }
}

# ──────────────────────────────────────────
# Mailpit — servidor SMTP fake para testes
# ──────────────────────────────────────────
resource "kubernetes_deployment" "mailpit" {
  metadata {
    name      = "mailpit"
    namespace = var.namespace
  }

  spec {
    replicas = 1

    selector {
      match_labels = { app = "mailpit" }
    }

    template {
      metadata {
        labels = { app = "mailpit" }
      }

      spec {
        container {
          name  = "mailpit"
          image = "axllent/mailpit:latest"

          port { container_port = 1025 }
          port { container_port = 8025 }

          readiness_probe {
            http_get {
              path = "/"
              port = 8025
            }
            initial_delay_seconds = 5
            period_seconds        = 5
            failure_threshold     = 5
          }

          liveness_probe {
            http_get {
              path = "/"
              port = 8025
            }
            period_seconds    = 30
            failure_threshold = 3
          }

          resources {
            requests = { cpu = "50m", memory = "64Mi" }
            limits   = { cpu = "500m", memory = "128Mi" }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "mailpit" {
  metadata {
    name      = "mailpit"
    namespace = var.namespace
  }

  spec {
    selector = { app = "mailpit" }

    port {
      name        = "smtp"
      port        = 1025
      target_port = 1025
    }

    port {
      name        = "ui"
      port        = 8025
      target_port = 8025
      node_port   = 30825
    }

    type = "NodePort"
  }
}

# ──────────────────────────────────────────
# Aplicação — oficina-app
# ──────────────────────────────────────────
resource "kubernetes_deployment" "app" {
  metadata {
    name      = "oficina-app"
    namespace = var.namespace
  }

  spec {
    replicas = 2

    selector {
      match_labels = { app = "oficina-app" }
    }

    template {
      metadata {
        labels = { app = "oficina-app" }
      }

      spec {
        image_pull_secrets {
          name = kubernetes_secret.ghcr.metadata[0].name
        }

        container {
          name              = "oficina-app"
          image             = var.app_image
          image_pull_policy = "IfNotPresent"

          port { container_port = 8080 }

          env_from {
            config_map_ref { name = "oficina-config" }
          }
          env_from {
            secret_ref { name = "oficina-secret" }
          }

          startup_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            failure_threshold = 60
            period_seconds    = 10
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            period_seconds    = 30
            failure_threshold = 3
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            period_seconds    = 10
            failure_threshold = 3
          }

          resources {
            requests = { cpu = "250m", memory = "384Mi" }
            limits   = { cpu = "1000m", memory = "640Mi" }
          }
        }
      }
    }
  }

  depends_on = [kubernetes_secret.ghcr, null_resource.build_and_load_image]
}

resource "kubernetes_service" "app" {
  metadata {
    name      = "oficina-app"
    namespace = var.namespace
  }

  spec {
    selector = { app = "oficina-app" }

    port {
      port        = 8080
      target_port = 8080
      node_port   = 30080
    }

    type = "NodePort"
  }
}

# ──────────────────────────────────────────
# HPA — escalonamento automático por CPU
# ──────────────────────────────────────────
resource "kubernetes_horizontal_pod_autoscaler_v2" "app" {
  metadata {
    name      = "oficina-app-hpa"
    namespace = var.namespace
  }

  spec {
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.app.metadata[0].name
    }

    min_replicas = 2
    max_replicas = 5

    metric {
      type = "Resource"

      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = 70
        }
      }
    }
  }
}
