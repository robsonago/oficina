# ──────────────────────────────────────────
# Recursos criados pelo Terraform:
#
# [cluster]
#   - Cluster kind com 1 nó control-plane
#   - Port mappings: 30080 (API) e 30825 (Mailhog UI)
#
# [database]
#   - Namespace: oficina
#   - Secret: oficina-secret (DB_USERNAME, DB_PASSWORD, JWT_SECRET)
#   - ConfigMap: oficina-config (DB_URL, MAIL_*, SERVER_PORT)
#   - PersistentVolumeClaim: postgres-pvc (1Gi)
#   - Deployment: postgres (imagem postgres:16-alpine)
#   - Service: postgres (ClusterIP, porta 5432)
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

output "next_steps" {
  description = "Próximos passos após terraform apply"
  value       = <<-EOT
    Cluster e banco de dados provisionados!

    Para fazer o deploy da aplicação:
      kubectl apply -f ../k8s/deployment-mailhog.yaml
      kubectl apply -f ../k8s/service-mailhog.yaml
      kubectl apply -f ../k8s/deployment-app.yaml
      kubectl apply -f ../k8s/service-app.yaml
      kubectl apply -f ../k8s/hpa.yaml

    Para acessar após o deploy:
      API:         http://localhost:30080
      Swagger UI:  http://localhost:30080/swagger-ui.html
      Mailhog UI:  http://localhost:30825

    Para destruir o ambiente:
      terraform destroy
  EOT
}
