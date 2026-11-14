# ADR-004 — Pub/Sub como mensageria entre os microsserviços (com outbox)

**Status:** Aceito · **Fase:** 4

## Decisão

A comunicação assíncrona da saga usa o **Google Cloud Pub/Sub**, com um tópico de comandos por
participante e um tópico de eventos para o orquestrador:

| Tópico | Quem publica | Assinatura (quem consome) |
|---|---|---|
| `oficina-billing-comandos-<amb>` | OS Service | `...-billing` (Billing) |
| `oficina-execucao-comandos-<amb>` | OS Service | `...-execucao` (Execution) |
| `oficina-os-eventos-<amb>` | Billing e Execution | `...-os` (OS Service) |
| `oficina-saga-dlq-<amb>` | o próprio Pub/Sub, após 5 tentativas | `...-inspecao` |
| `oficina-notificacoes-<amb>` (Fase 3) | Billing | function de e-mail |

Chamadas **REST síncronas** ficam restritas a leitura: o endpoint
`GET /api/ordens-servico/{id}/acompanhamento` monta a visão consolidada da OS consultando Billing e
Execution, com timeout de 2s e circuit breaker (Resilience4j). Se um serviço estiver fora, a
consulta devolve o que tem e indica o que ficou indisponível — a saga nunca depende de REST.

## Alternativas consideradas

- **RabbitMQ ou Kafka no cluster**: mais um componente para operar (disco, réplicas, upgrade) dentro
  de um cluster de um nó só. Kafka seria exagero para o volume da oficina.
- **Pub/Sub** (escolhido): já era usado na Fase 3 (notificações), é gerenciado, tem dead letter e
  retry nativos, autenticação por IAM com Workload Identity (sem senha) e um emulador oficial que
  roda no `docker-compose` e nos testes automatizados.

## Detalhes de implementação

- Envelope comum (`Mensagem`): `id`, `tipo`, `sagaId`, `ordemServicoId`, `ocorridoEm`, `dados`.
- Atributos da mensagem: `tipo`, `correlationId`, `sagaId` e os cabeçalhos de distributed tracing da
  New Relic (`newrelic`, `traceparent`, `tracestate`), para o trace da requisição HTTP continuar no
  consumidor.
- **Outbox** no OS Service e no Execution Service (bancos relacionais): evento gravado na transação
  do negócio, publicado por `RelayOutbox`. No Billing (MongoDB standalone, sem transação
  multi-documento) a publicação acontece logo após salvar; a idempotência dos consumidores e os prazos
  da saga cobrem uma eventual perda.
- Permissões mínimas: cada service account só publica nos tópicos e consome das assinaturas que são
  dela (Terraform em `oficina-infra-k8s/fase4.tf`).

## Consequências

- **Positivo**: os serviços não precisam estar no ar ao mesmo tempo; uma queda do Billing só atrasa a
  saga, as mensagens esperam na assinatura.
- **Positivo**: o mesmo código roda local (emulador), nos testes (emulador via Testcontainers) e no GCP.
- **Negativo**: Pub/Sub não garante ordem sem ordering keys — tratado pela guarda de etapa no
  orquestrador (evento fora de ordem é ignorado e registrado).
