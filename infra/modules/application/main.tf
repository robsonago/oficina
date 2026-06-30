terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
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
          name = "ghcr-secret"
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
