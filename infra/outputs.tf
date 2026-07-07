# ──────────────────────────────────────────
# Recursos criados pelo Terraform:
#
# [cluster]
#   - Cluster kind com 1 nó control-plane
#   - Port mappings: 30080 (API) e 30825 (Mailpit UI)
#
# [database]
#   - Namespace: oficina
#   - Secret: oficina-secret (DB_USERNAME, DB_PASSWORD, JWT_SECRET)
#   - ConfigMap: oficina-config (DB_URL, MAIL_*, SERVER_PORT)
#   - PersistentVolumeClaim: postgres-pvc (1Gi)
#   - Deployment: postgres (imagem postgres:16-alpine)
#   - Service: postgres (ClusterIP, porta 5432)
#
# [application]
#   - Deployment: mailpit (axllent/mailpit:latest)
#   - Service: mailpit (NodePort 30825)
#   - Deployment: oficina-app (2 réplicas)
#   - Service: oficina-app (NodePort 30080)
#   - HPA: oficina-app-hpa (min 2, max 5, target CPU 70%)
# ──────────────────────────────────────────

output "cluster_name" {
  description = "Nome do cluster kind provisionado"
  value       = module.cluster.cluster_name
}

output "kubeconfig_path" {
  description = "Caminho do kubeconfig gerado pelo kind"
  value       = module.cluster.kubeconfig_path
}

output "namespace" {
  description = "Namespace Kubernetes criado"
  value       = module.database.namespace
}

output "postgres_service" {
  description = "Nome do Service do PostgreSQL dentro do cluster"
  value       = module.database.postgres_service
}

output "app_service" {
  description = "Nome do Service da aplicação"
  value       = module.application.app_service
}

output "mailpit_service" {
  description = "Nome do Service do Mailpit"
  value       = module.application.mailpit_service
}

output "next_steps" {
  description = "URLs de acesso após o terraform apply"
  value       = <<-EOT
    Ambiente completo provisionado pelo Terraform!

    Acesso:
      API:        http://localhost:30080
      Swagger UI: http://localhost:30080/swagger-ui.html
      Mailpit UI: http://localhost:30825

    Para destruir o ambiente:
      terraform destroy
  EOT
}
