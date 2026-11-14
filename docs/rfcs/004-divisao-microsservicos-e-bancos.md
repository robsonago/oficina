# RFC-004 — Divisão em microsserviços e escolha dos bancos

**Status:** Aprovada · **Fase:** 4

## Problema

A oficina passou a operar em várias filiais, com volume maior de OS e áreas diferentes (atendimento,
diagnóstico, execução, cobrança) mexendo no mesmo sistema. O monólito hexagonal das fases anteriores
escala como um bloco só e uma falha em qualquer parte (ex.: integração de pagamento) afeta tudo.

## Proposta

Três microsserviços, um por *bounded context* que já existia no DDD da Fase 1
([`docs/ddd/contextos-delimitados.md`](../ddd/contextos-delimitados.md)):

| Serviço | Contexto | Responsabilidade | Banco |
|---|---|---|---|
| **OS Service** (`oficina`) | Atendimento + Ordem de Serviço | abertura, status, histórico, cadastros (cliente, veículo, serviços, peças/estoque), autenticação de funcionário, **orquestração da saga** | PostgreSQL (Cloud SQL) |
| **Billing Service** | Orçamento e Pagamento | gerar/enviar orçamento, aprovação, cobrança e estorno no Mercado Pago | **MongoDB** |
| **Execution Service** | Execução e Produção | fila de execução, diagnóstico/reparo, finalização | PostgreSQL (database e usuário próprios) |

### Por que essa divisão

- Segue o exemplo do enunciado e os contextos já mapeados, então a linguagem ubíqua não muda.
- Cada serviço tem um motivo diferente para mudar: regras de cobrança e gateway de pagamento mudam
  independentemente da fila de execução.
- O catálogo de peças e o estoque ficaram no OS Service porque a reserva de peças é uma etapa da saga
  que o orquestrador executa localmente (e compensa devolvendo ao estoque). Mover o estoque para outro
  serviço adicionaria mais uma etapa remota sem ganho para o volume atual.

### Por que esses bancos

- **PostgreSQL no OS Service**: dados muito relacionais (cliente → veículo → OS → itens → peça), com
  integridade referencial e a outbox/saga precisando de transação ACID.
- **MongoDB no Billing**: o orçamento é um documento autocontido — cópia dos dados do cliente, itens
  com preço do momento, dados da cobrança e histórico — sempre lido e gravado inteiro. Também guarda
  bem respostas de APIs externas (Mercado Pago) cujo formato pode mudar. Atende ao requisito de pelo
  menos um banco NoSQL.
- **PostgreSQL no Execution**: fila com ordenação por prioridade/data e outbox transacional.

### Isolamento dos dados

Nenhum serviço acessa o banco de outro: o Billing recebe tudo o que precisa no comando
`GERAR_ORCAMENTO`; o Execution recebe a lista de itens no `EXECUTAR_OS`; o OS Service só conhece
orçamento/execução pelos eventos ou pela API REST de leitura. No GCP, cada serviço tem service
account, usuário de banco e secret próprios. O banco do Execution fica na mesma instância Cloud SQL do
OS Service (database separado) para manter o custo do ambiente de estudo — numa operação real o
próximo passo seria uma instância por serviço.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem/framework | Java 21 + Spring Boot 3.4 nos três serviços (mesmo padrão hexagonal) |
| Mensageria | Google Pub/Sub (ADR-004) |
| Coordenação | Saga orquestrada (ADR-003) |
| Pagamento | Mercado Pago Checkout Pro |
| Resiliência REST | Resilience4j (circuit breaker) + timeouts |
| Testes | JUnit 5, Mockito, Cucumber (BDD), Testcontainers (MongoDB e emulador do Pub/Sub) |
| Qualidade | JaCoCo (mínimo 80% por serviço, quebra o build) + SonarCloud no CI |
| Deploy | Docker + Kustomize no GKE, um pipeline GitHub Actions por repositório |
| Observabilidade | New Relic (APM com distributed tracing, métricas, logs, alertas) — mesma stack da Fase 3 |
