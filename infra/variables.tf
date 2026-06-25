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
