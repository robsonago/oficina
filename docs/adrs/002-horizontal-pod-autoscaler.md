# ADR-002 — Uso do HorizontalPodAutoscaler (HPA)

**Status:** Aceito (decidido na Fase 2, formalizado aqui) · **Fase:** 2/3

## Decisão

A aplicação roda no Kubernetes com um `HorizontalPodAutoscaler` configurado para **2 a 5 réplicas**,
escalando por **70% de utilização de CPU** média entre os pods, em cada ambiente
(homologação/produção).

```yaml
minReplicas: 2
maxReplicas: 5
targetCPUUtilizationPercentage: 70
```

## Contexto

O enunciado exige que o sistema suporte picos de demanda sem intervenção manual ("Cluster Kubernetes
com escalabilidade"). A carga de uma oficina mecânica é naturalmente irregular — concentrada em
horário comercial, com picos em torno de aprovações de orçamento e abertura de OS — o que torna
autoscaling mais adequado do que um número fixo de réplicas dimensionado para o pico (desperdício na
maior parte do tempo) ou para a média (indisponibilidade no pico).

## Por que HPA por CPU (e não por outra métrica)

- A aplicação é majoritariamente **CPU-bound** nas operações mais frequentes (serialização
  JSON, validação, queries) — não há filas internas nem I/O de longa duração que tornassem uma métrica
  customizada (ex.: requisições em fila) mais representativa do gargalo real.
- CPU é uma métrica nativa do `metrics-server` do GKE, sem depender de infraestrutura adicional
  (diferente de escalar por métrica customizada do Prometheus, que exigiria o
  Prometheus Adapter rodando no cluster).

## Por que 2 réplicas mínimas (não 1)

Uma única réplica significa indisponibilidade completa durante o tempo de um rolling update ou de uma
falha de pod — inaceitável mesmo fora de pico. Duas réplicas garantem que sempre há capacidade
servindo tráfego durante deploys e falhas pontuais de um nó.

## Por que 5 réplicas máximas (não mais)

Limitado pelo tamanho real do cluster (1 nó `e2-standard-4`, ver
[RFC-001](../rfcs/001-escolha-da-nuvem.md)) e pelo limite de conexões do Cloud SQL `db-f1-micro` — mais
réplicas do que isso esgotaria o pool de conexões do banco antes de qualquer ganho de capacidade (ver
[RFC-002](../rfcs/002-escolha-banco-de-dados.md)), motivo pelo qual o pool do Hikari por pod também foi
reduzido (`SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3`).

## Consequências

- Em pico sustentado além da capacidade de 5 réplicas, a aplicação degrada (latência maior) em vez de
  cair — não há um circuito de proteção adicional além do próprio HPA.
- O teto de 5 réplicas está acoplado ao dimensionamento atual do banco e do cluster; alterar um sem
  revisar o outro pode reintroduzir o gargalo de conexões que motivou o limite atual.
