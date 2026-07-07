variable "namespace" {
  description = "Namespace Kubernetes onde os recursos serão criados"
  type        = string
}

variable "db_username" {
  description = "Usuário do banco de dados PostgreSQL"
  type        = string
}

variable "db_password" {
  description = "Senha do banco de dados PostgreSQL"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Chave secreta para assinatura dos tokens JWT"
  type        = string
  sensitive   = true
}

variable "postgres_storage" {
  description = "Tamanho do volume persistente para o PostgreSQL"
  type        = string
  default     = "1Gi"
}
