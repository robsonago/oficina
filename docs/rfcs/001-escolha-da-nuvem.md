# RFC-001 — Escolha do provedor de nuvem

**Status:** Aceito · **Fase:** 3

## Contexto

A Fase 3 exige mover a aplicação de um cluster Kubernetes local (`kind`) para um provedor de nuvem
real, com banco de dados gerenciado, orquestração de containers, autenticação serverless e
observabilidade. O enunciado do Tech Challenge não obriga um provedor específico — a escolha ficou
em aberto entre os três grandes: AWS, Azure e GCP.

## Opções consideradas

| Critério | AWS | Azure | GCP |
|---|---|---|---|
| Kubernetes gerenciado | EKS — cobra pela control plane (~US$0,10/h) além dos nós | AKS — control plane gratuita | **GKE — control plane gratuita**, só se paga pelos nós |
| Banco gerenciado equivalente | RDS PostgreSQL | Azure Database for PostgreSQL | Cloud SQL PostgreSQL |
| API Gateway gerenciado | API Gateway (bom, mas integração mais verbosa com IAM) | API Management (robusto, porém mais caro/complexo pra um projeto pequeno) | **API Gateway** — simples de integrar com Cloud Functions/GKE |
| Funções serverless | Lambda | Functions | Cloud Functions |
| Familiaridade da equipe | Baixa | Baixa | Maior (uso prévio em outras disciplinas do curso) |
| Crédito/trial disponível | Trial de 12 meses com limites de serviço | Trial de 30 dias | Trial de 90 dias, US$300 em crédito, sem limitar quais serviços usar |

## Decisão

**GCP**, pelos seguintes motivos, em ordem de peso:

1. **GKE não cobra pela control plane** — para um projeto acadêmico rodando por tempo limitado, isso
   reduz custo fixo sem abrir mão de um Kubernetes gerenciado de verdade (diferente de rodar o próprio
   control plane em VMs).
2. **Trial mais permissivo**: 90 dias e crédito aplicável a qualquer serviço, o que deu margem para
   testar/destruir a infraestrutura várias vezes durante o desenvolvimento sem se preocupar em ficar
   sem crédito no meio do caminho (ver estratégia de destruir/recriar entre sessões de trabalho, no
   runbook de infraestrutura).
3. **Integração mais direta entre os serviços usados**: Secret Manager, Cloud SQL, GKE (Workload
   Identity) e Cloud Functions compartilham o mesmo modelo de IAM e de autenticação via Workload
   Identity Federation no CI/CD — sem precisar gerenciar uma chave de service account como segredo em
   nenhum dos 4 repositórios.

## Consequências

- Todo o Terraform, os workflows de CI/CD (Workload Identity Federation) e os runbooks de operação
  são específicos de GCP — migrar de provedor exigiria reescrever a camada de infraestrutura, não só
  trocar variáveis.
- A região escolhida (`southamerica-east1`, São Paulo) reduz latência para o caso de uso real (oficina
  no Brasil), mas o **API Gateway não está disponível nessa região** — foi provisionado em `us-east1`
  (mais próxima entre as disponíveis para o produto), o que adiciona uma perna de latência extra só
  para o tráfego que passa pelo Gateway.
