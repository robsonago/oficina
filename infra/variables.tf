variable "cluster_name" {
  description = "Nome do cluster kind"
  type        = string
  default     = "oficina"
}

variable "namespace" {
  description = "Namespace Kubernetes onde os recursos serão criados"
  type        = string
  default     = "oficina"
}

variable "db_username" {
  description = "Usuário do banco de dados PostgreSQL"
  type        = string
  default     = "oficina"
}

variable "db_password" {
  description = "Senha do banco de dados PostgreSQL"
  type        = string
  sensitive   = true
  default     = "oficina123"
}

variable "jwt_secret" {
  description = "Chave secreta para assinatura dos tokens JWT (base64)"
  type        = string
  sensitive   = true
  default     = "bXlTdXBlclNlY3JldEtleUZvckpXVFN5c3RlbU9maWNpbmFNZWNhbmljYTIwMjQ="
}

variable "app_image" {
  description = "Imagem Docker da aplicação"
  type        = string
  default     = "ghcr.io/robsonago/oficina-app:latest"
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
