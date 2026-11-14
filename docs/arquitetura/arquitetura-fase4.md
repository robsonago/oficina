# Arquitetura — Fase 4 (microsserviços)

Visão geral da arquitetura final: três microsserviços, cada um com repositório, banco, pipeline e
manifestos Kubernetes próprios, conversando por Pub/Sub (saga orquestrada) e, só para leitura, por
REST. Decisões detalhadas em [ADR-003](../adrs/003-saga-orquestrada.md),
[ADR-004](../adrs/004-mensageria-pubsub-outbox.md) e
[RFC-004](../rfcs/004-divisao-microsservicos-e-bancos.md).

## 1. Visão geral

```mermaid
flowchart TB
    cliente([Cliente / Funcionário]) --> gw[Google API Gateway]
    mp([Mercado Pago]) -- webhook --> gw
    gw --> ing[Ingress GKE<br/>HTTPS + certificado gerenciado]
    cpf[[Function auth por CPF]] -. JWT .-> cliente

    subgraph gke [GKE · namespaces oficina-homolog / oficina-producao]
        ing -- "/api/** (padrão)" --> os[OS Service<br/>oficina-app]
        ing -- "/api/orcamentos, /api/pagamentos" --> bil[Billing Service<br/>oficina-billing]
        ing -- "/api/execucoes" --> exe[Execution Service<br/>oficina-execution]
        bil --> mongo[(MongoDB<br/>StatefulSet)]
        os -. REST leitura<br/>circuit breaker .-> bil
        os -. REST leitura<br/>circuit breaker .-> exe
    end

    os -- Cloud SQL Proxy --> sqlos[(Cloud SQL<br/>oficina_*)]
    exe -- Cloud SQL Proxy --> sqlex[(Cloud SQL<br/>execucao_*)]
    bil -- preferência / consulta / estorno --> mp

    subgraph pubsub [Google Pub/Sub]
        tb[[billing-comandos]]
        te[[execucao-comandos]]
        to[[os-eventos]]
        tn[[notificacoes]]
        dlq[[saga-dlq]]
    end

    os -- outbox --> tb & te
    tb --> bil
    te --> exe
    bil --> to
    exe -- outbox --> to
    to --> os
    bil --> tn --> mail[[Function de e-mail]]
    tb & te & to -. 5 falhas .-> dlq

    os & bil & exe -. APM, traces, métricas, logs .-> nr[(New Relic)]
```

| Repositório | Conteúdo |
|---|---|
| [`oficina`](https://github.com/robsonago/oficina) | OS Service + orquestrador da saga + `docker-compose` do ambiente completo |
| [`oficina-billing-service`](https://github.com/robsonago/oficina-billing-service) | Billing Service (MongoDB, Mercado Pago) |
| [`oficina-execution-service`](https://github.com/robsonago/oficina-execution-service) | Execution Service (PostgreSQL, outbox) |
| [`oficina-auth-function`](https://github.com/robsonago/oficina-auth-function) | Functions de autenticação por CPF e envio de e-mail (Fase 3) |
| [`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db) | Cloud SQL: databases/usuários do OS e do Execution |
| [`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s) | GKE, Pub/Sub da saga, IAM/Workload Identity, configs/secrets, Ingress, API Gateway |

## 2. Saga — caminho feliz

```mermaid
sequenceDiagram
    autonumber
    actor M as Mecânico
    actor C as Cliente
    participant OS as OS Service (orquestrador)
    participant B as Billing
    participant MP as Mercado Pago
    participant E as Execution

    M->>OS: POST /ordens-servico/{id}/gerar-orcamento
    OS->>OS: OS → AGUARDANDO_ORCAMENTO, saga GERANDO_ORCAMENTO (outbox)
    OS-)B: GERAR_ORCAMENTO
    B->>B: grava orçamento (MongoDB), notifica cliente
    B-)OS: ORCAMENTO_GERADO
    OS->>OS: OS → AGUARDANDO_APROVACAO
    C->>B: POST /orcamentos/{id}/aprovacao {aprovado: true}
    B->>MP: cria preferência (Checkout Pro)
    B-)OS: ORCAMENTO_APROVADO (link de pagamento)
    OS->>OS: reserva peças no estoque, OS → AGUARDANDO_PAGAMENTO
    C->>MP: paga
    MP->>B: webhook (pagamento)
    B->>MP: GET /v1/payments/{id}
    B-)OS: PAGAMENTO_APROVADO
    OS->>OS: OS → AGUARDANDO_EXECUCAO (outbox)
    OS-)E: EXECUTAR_OS
    E->>E: entra na fila
    M->>E: POST /execucoes/proxima (diagnóstico) e /reparo
    E-)OS: EXECUCAO_ATUALIZADA
    OS->>OS: OS → EM_EXECUCAO
    M->>E: POST /execucoes/{id}/finalizar
    E-)OS: EXECUCAO_FINALIZADA
    OS->>OS: OS → FINALIZADA, saga CONCLUIDA
```

## 3. Saga — falha na execução (compensação)

```mermaid
sequenceDiagram
    autonumber
    actor M as Mecânico
    participant OS as OS Service (orquestrador)
    participant B as Billing
    participant MP as Mercado Pago
    participant E as Execution

    Note over OS,E: pagamento já confirmado, OS na fila de execução
    M->>E: POST /execucoes/{id}/falha {motivo}
    E-)OS: EXECUCAO_FALHOU
    OS->>OS: devolve peças ao estoque
    OS->>OS: OS → CANCELADA, saga COMPENSANDO
    OS-)B: ESTORNAR_PAGAMENTO
    B->>MP: POST /v1/payments/{id}/refunds
    B-)OS: PAGAMENTO_ESTORNADO
    OS->>OS: saga COMPENSADA
```

Os demais cenários de compensação (pagamento recusado, falta de estoque, rejeição, prazo esgotado)
estão descritos no [ADR-003](../adrs/003-saga-orquestrada.md) e automatizados em
[`saga_ordem_servico.feature`](../../src/test/resources/features/saga_ordem_servico.feature).

## 4. Estados

```mermaid
stateDiagram-v2
    [*] --> RECEBIDA
    RECEBIDA --> EM_DIAGNOSTICO
    EM_DIAGNOSTICO --> AGUARDANDO_ORCAMENTO: gerar-orcamento (inicia a saga)
    AGUARDANDO_ORCAMENTO --> AGUARDANDO_APROVACAO: ORCAMENTO_GERADO
    AGUARDANDO_ORCAMENTO --> EM_DIAGNOSTICO: ORCAMENTO_FALHOU / prazo
    AGUARDANDO_APROVACAO --> AGUARDANDO_PAGAMENTO: ORCAMENTO_APROVADO
    AGUARDANDO_APROVACAO --> EM_DIAGNOSTICO: ORCAMENTO_REJEITADO
    AGUARDANDO_APROVACAO --> CANCELADA: sem estoque / prazo
    AGUARDANDO_PAGAMENTO --> AGUARDANDO_EXECUCAO: PAGAMENTO_APROVADO
    AGUARDANDO_PAGAMENTO --> CANCELADA: PAGAMENTO_RECUSADO / prazo
    AGUARDANDO_EXECUCAO --> EM_EXECUCAO: EXECUCAO_ATUALIZADA
    AGUARDANDO_EXECUCAO --> CANCELADA: EXECUCAO_FALHOU
    EM_EXECUCAO --> FINALIZADA: EXECUCAO_FINALIZADA
    EM_EXECUCAO --> CANCELADA: EXECUCAO_FALHOU
    FINALIZADA --> ENTREGUE
```

## 5. Observabilidade dos fluxos distribuídos

- **Distributed tracing (New Relic)**: o agente Java instrumenta HTTP automaticamente; nas mensagens
  do Pub/Sub os cabeçalhos de trace são copiados para os atributos na publicação (inclusive via
  outbox) e aceitos no consumo (`Rastreamento.continuar`), então uma OS aparece como um trace que
  atravessa OS Service → Billing → OS Service → Execution.
- **Logs**: JSON com `service`, `correlationId` e `sagaId` em todos os serviços. Consulta no New Relic
  Logs: `sagaId:"<id>"` mostra a saga inteira, de todos os serviços, em ordem.
- **Métricas de negócio** (`/actuator/prometheus`, coletadas pelo `nri-prometheus`):
  `oficina_saga_iniciadas_total`, `oficina_saga_concluidas_total`,
  `oficina_saga_compensadas_total{motivo}`, `oficina_billing_eventos_total{tipo}`,
  `oficina_execucao_fila{status}`, `oficina_outbox_pendentes`, `oficina_integracoes_erros_total`.
- **Consultas NRQL sugeridas para o dashboard da saga**:

```sql
-- sagas por desfecho (últimas 24h)
SELECT sum(oficina_saga_iniciadas_total) AS 'iniciadas', sum(oficina_saga_concluidas_total) AS 'concluídas',
       sum(oficina_saga_compensadas_total) AS 'compensadas' FROM Metric SINCE 1 day ago

-- compensações por motivo
SELECT sum(oficina_saga_compensadas_total) FROM Metric FACET motivo SINCE 1 day ago TIMESERIES

-- tamanho da fila de execução
SELECT latest(oficina_execucao_fila) FROM Metric FACET status TIMESERIES

-- eventos parados na outbox (alerta se > 0 por 5 minutos)
SELECT max(oficina_outbox_pendentes) FROM Metric FACET service TIMESERIES
```
