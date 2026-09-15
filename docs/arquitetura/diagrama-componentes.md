# Diagrama de Componentes — Fase 3

Visão geral de todas as peças do sistema em nuvem (GCP) e como se conectam. Cada ambiente
(homologação/produção) tem sua própria instância de cada componente dentro do mesmo projeto GCP —
o diagrama abaixo mostra um ambiente para não duplicar visualmente.

```mermaid
flowchart TB
    subgraph Clientes["Clientes da API"]
        Funcionario["Funcionário<br/>(usuário/senha)"]
        ClienteFinal["Cliente da oficina<br/>(autentica por CPF)"]
    end

    Gateway["API Gateway<br/>(Google API Gateway)<br/>repo: oficina-infra-k8s"]

    subgraph GKE["Cluster GKE — namespace por ambiente"]
        Ingress["Ingress (GCLB)<br/>+ ManagedCertificate"]
        App["oficina-app<br/>Spring Boot / Java 21<br/>2-5 réplicas (HPA, 70% CPU)"]
        Ingress --> App
    end

    AuthFn["oficina-auth-function<br/>Cloud Function (Java 21)<br/>valida CPF, emite JWT"]
    NotifFn["oficina-notification-function<br/>Cloud Function (Java 21)<br/>consome fila, envia e-mail (SMTP)"]
    PubSub["Pub/Sub<br/>tópico de notificações"]
    CloudSQL["Cloud SQL<br/>PostgreSQL 16<br/>repo: oficina-infra-db"]

    subgraph NewRelic["New Relic (observabilidade)"]
        APM["Agente APM<br/>(embutido no Dockerfile)"]
        Prometheus["nri-prometheus<br/>(Helm, no cluster)<br/>CPU/memória + métricas de negócio"]
        Dashboards["3 Dashboards +<br/>Alertas NRQL +<br/>Synthetic Monitors (uptime)"]
    end

    ClienteFinal -->|"POST /auth {cpf}"| AuthFn
    AuthFn -->|"consulta cliente"| CloudSQL
    AuthFn -->|"JWT (role=CLIENTE)"| ClienteFinal

    Funcionario -->|"login usuário/senha"| Gateway
    ClienteFinal -->|"requisições com JWT"| Gateway
    Gateway --> Ingress

    App -->|"JDBC"| CloudSQL
    App -->|"publica evento"| PubSub
    PubSub --> NotifFn
    NotifFn -->|"SMTP"| EmailExt["Provedor de e-mail externo"]

    App -.->|"traces/erros"| APM
    App -.->|"/actuator/prometheus"| Prometheus
    APM --> Dashboards
    Prometheus --> Dashboards
    Dashboards -.->|"checagem de uptime<br/>/actuator/health"| Ingress

    subgraph CICD["CI/CD (GitHub Actions) — 4 repositórios"]
        R1["oficina"]
        R2["oficina-infra-k8s"]
        R3["oficina-infra-db"]
        R4["oficina-auth-function"]
    end
    R1 -.->|"build + push imagem +<br/>kubectl set image"| App
    R2 -.->|"terraform apply"| GKE
    R2 -.->|"terraform apply"| Gateway
    R3 -.->|"terraform apply"| CloudSQL
    R4 -.->|"gcloud functions deploy"| AuthFn
    R4 -.->|"gcloud functions deploy"| NotifFn
```

## Legenda

- **Linha sólida**: chamada síncrona (HTTP/JDBC).
- **Linha pontilhada**: comunicação assíncrona/observação (deploy, métricas, healthcheck).
- Cada repositório de infraestrutura (`oficina-infra-k8s`, `oficina-infra-db`) provisiona sua parte
  via Terraform; `oficina` e `oficina-auth-function` publicam artefatos (imagem Docker / functions)
  que o Kubernetes/Cloud Functions passam a rodar.

## Relacionado

- [`docs/arquitetura/diagrama-sequencia.md`](diagrama-sequencia.md) — o passo a passo de duas
  interações específicas (autenticação por CPF e abertura de OS).
- [`docs/arquitetura/infraestrutura.md`](infraestrutura.md) — decisões técnicas de cada componente.
- [`docs/arquitetura/hexagonal.md`](hexagonal.md) — o que tem *dentro* do componente `oficina-app`.
