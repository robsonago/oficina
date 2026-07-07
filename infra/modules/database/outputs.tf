output "namespace" {
  description = "Namespace criado"
  value       = kubernetes_namespace.this.metadata[0].name
}

output "postgres_service" {
  description = "Nome do Service do PostgreSQL"
  value       = kubernetes_service.postgres.metadata[0].name
}

output "secret_name" {
  description = "Nome do Secret com as credenciais"
  value       = kubernetes_secret.oficina.metadata[0].name
}

output "configmap_name" {
  description = "Nome do ConfigMap com as configurações"
  value       = kubernetes_config_map.oficina.metadata[0].name
}
