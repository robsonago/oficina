output "app_service" {
  description = "Nome do Service da aplicação"
  value       = kubernetes_service.app.metadata[0].name
}

output "mailpit_service" {
  description = "Nome do Service do Mailpit"
  value       = kubernetes_service.mailpit.metadata[0].name
}
