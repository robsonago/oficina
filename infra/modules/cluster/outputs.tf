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
