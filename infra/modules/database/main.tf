terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
  }
}

# ──────────────────────────────────────────
# Recurso: Namespace
# ──────────────────────────────────────────
resource "kubernetes_namespace" "this" {
  metadata {
    name = var.namespace
  }
}

# ──────────────────────────────────────────
# Recurso: Secret
# Armazena credenciais sensíveis do banco e JWT
# ──────────────────────────────────────────
resource "kubernetes_secret" "oficina" {
  metadata {
    name      = "oficina-secret"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  string_data = {
    DB_USERNAME  = var.db_username
    DB_PASSWORD  = var.db_password
    JWT_SECRET   = var.jwt_secret
    MAIL_USERNAME = ""
    MAIL_PASSWORD = ""
  }
}

# ──────────────────────────────────────────
# Recurso: ConfigMap
# Armazena variáveis de configuração não-sensíveis
# ──────────────────────────────────────────
resource "kubernetes_config_map" "oficina" {
  metadata {
    name      = "oficina-config"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  data = {
    DB_URL        = "jdbc:postgresql://postgres:5432/${var.namespace}"
    JWT_EXPIRATION = "86400000"
    MAIL_HOST     = "mailpit"
    MAIL_PORT     = "1025"
    MAIL_AUTH     = "false"
    MAIL_STARTTLS = "false"
    MAIL_FROM     = "oficina@localhost"
    SERVER_PORT   = "8080"
  }
}

# ──────────────────────────────────────────
# Recurso: PersistentVolumeClaim
# Volume persistente para os dados do PostgreSQL
# ──────────────────────────────────────────
resource "kubernetes_persistent_volume_claim" "postgres" {
  metadata {
    name      = "postgres-pvc"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  spec {
    access_modes = ["ReadWriteOnce"]

    resources {
      requests = {
        storage = var.postgres_storage
      }
    }
  }

  wait_until_bound = false
}

# ──────────────────────────────────────────
# Recurso: Deployment do PostgreSQL
# Banco de dados relacional da aplicação
# ──────────────────────────────────────────
resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app = "postgres"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = "postgres:16-alpine"

          port {
            container_port = 5432
          }

          env {
            name  = "POSTGRES_DB"
            value = var.namespace
          }

          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.oficina.metadata[0].name
                key  = "DB_USERNAME"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.oficina.metadata[0].name
                key  = "DB_PASSWORD"
              }
            }
          }

          volume_mount {
            name       = "postgres-data"
            mount_path = "/var/lib/postgresql/data"
          }

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_username]
            }
            initial_delay_seconds = 10
            period_seconds        = 5
            failure_threshold     = 5
          }

          resources {
            requests = {
              cpu    = "250m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "1000m"
              memory = "512Mi"
            }
          }
        }

        volume {
          name = "postgres-data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.postgres.metadata[0].name
          }
        }
      }
    }
  }
}

# ──────────────────────────────────────────
# Recurso: Service do PostgreSQL
# Expõe o banco internamente no cluster
# ──────────────────────────────────────────
resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  spec {
    selector = {
      app = "postgres"
    }

    port {
      port        = 5432
      target_port = 5432
    }

    type = "ClusterIP"
  }
}
