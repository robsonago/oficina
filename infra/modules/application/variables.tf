variable "namespace" {
  description = "Namespace Kubernetes onde os recursos serão criados"
  type        = string
}

variable "app_image" {
  description = "Imagem Docker da aplicação (ex: ghcr.io/corpp00429/oficina-app:latest)"
  type        = string
}

variable "ghcr_username" {
  description = "Usuário do GitHub Container Registry"
  type        = string
  default     = "corpp00429"
}

variable "ghcr_token" {
  description = "Personal Access Token do GHCR (read:packages). Deixe vazio para uso local com 'kind load docker-image'."
  type        = string
  sensitive   = true
  default     = ""
}
