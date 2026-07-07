output "cluster_name" {
  description = "Nome do cluster kind criado"
  value       = kind_cluster.this.name
}

output "kubeconfig_path" {
  description = "Caminho do kubeconfig (padrão do kind: ~/.kube/config)"
  value       = "~/.kube/config"
}

output "endpoint" {
  description = "Endpoint do cluster Kubernetes"
  value       = kind_cluster.this.endpoint
}

output "client_certificate" {
  description = "Certificado do cliente (base64)"
  value       = kind_cluster.this.client_certificate
  sensitive   = true
}

output "client_key" {
  description = "Chave do cliente (base64)"
  value       = kind_cluster.this.client_key
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "CA do cluster (base64)"
  value       = kind_cluster.this.cluster_ca_certificate
  sensitive   = true
}
