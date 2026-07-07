variable "namespace" {
  description = "Namespace Kubernetes onde os recursos serão criados"
  type        = string
}

variable "app_image" {
  description = "Imagem Docker da aplicação (ex: ghcr.io/robsonago/oficina-app:latest)"
  type        = string
}

variable "ghcr_username" {
  description = "Usuário do GitHub Container Registry"
  type        = string
  default     = "robsonago"
}

variable "ghcr_token" {
  description = "Personal Access Token do GHCR (read:packages). Deixe vazio para uso local com 'kind load docker-image'."
  type        = string
  sensitive   = true
  default     = ""
}

variable "cluster_name" {
  description = "Nome do cluster kind (usado pelo 'kind load docker-image' quando build_local_image = true)"
  type        = string
  default     = "oficina"
}

variable "build_local_image" {
  description = "Se true, builda a imagem a partir do Dockerfile local e carrega no cluster via 'kind load docker-image', eliminando a dependência do GHCR. Se false, assume que a imagem já está publicada em 'app_image' e depende de pull real (requer 'ghcr_token' válido se o pacote for privado)."
  type        = bool
  default     = true
}
