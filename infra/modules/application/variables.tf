variable "namespace" {
  description = "Namespace Kubernetes onde os recursos serão criados"
  type        = string
}

variable "app_image" {
  description = "Imagem Docker da aplicação (ex: ghcr.io/corpp00429/oficina-app:latest)"
  type        = string
}
