# ADR-003 — Saga orquestrada para o fluxo da ordem de serviço

**Status:** Aceito · **Fase:** 4

## Decisão

O fluxo transacional da OS — **orçamento → aprovação → pagamento → execução** — passa por três
microsserviços com bancos próprios (OS Service, Billing Service e Execution Service). Para manter a
consistência sem uma transação distribuída, usamos o **Saga Pattern na forma orquestrada**: o
**OS Service** é o orquestrador (`OrquestradorSagaOS`), envia comandos aos participantes pelo Pub/Sub
e reage aos eventos que eles devolvem. Quando uma etapa falha, o orquestrador dispara as
**compensações** das etapas que já tinham sido concluídas, na ordem inversa.

```
OS Service (orquestrador)          Billing                    Execution
  GERAR_ORCAMENTO ───────────────►  cria orçamento
                  ◄─────────────── ORCAMENTO_GERADO
                                   cliente aprova
                  ◄─────────────── ORCAMENTO_APROVADO
  reserva peças (local)
                                   Mercado Pago confirma
                  ◄─────────────── PAGAMENTO_APROVADO
  EXECUTAR_OS ─────────────────────────────────────────────►  entra na fila
                  ◄──────────────────────────────────────────  EXECUCAO_ATUALIZADA / FINALIZADA
```

| Falha | Compensações disparadas |
|---|---|
| Billing não gera o orçamento (`ORCAMENTO_FALHOU`) | OS volta para diagnóstico |
| Cliente rejeita (`ORCAMENTO_REJEITADO`) | OS volta para diagnóstico |
| Falta peça no estoque na aprovação | `CANCELAR_ORCAMENTO`; OS cancelada |
| Pagamento recusado (`PAGAMENTO_RECUSADO`) | devolve peças ao estoque; `CANCELAR_ORCAMENTO`; OS cancelada |
| Prazo de aprovação/pagamento vencido | devolve peças; `CANCELAR_ORCAMENTO`; OS cancelada |
| Falha na execução (`EXECUCAO_FALHOU`) | devolve peças; `ESTORNAR_PAGAMENTO` (Mercado Pago); OS cancelada |

## Contexto

Na Fase 3 tudo isso era uma sequência de transições dentro do mesmo banco. Ao separar orçamento e
pagamento (Billing, MongoDB) e execução (Execution, PostgreSQL próprio), uma única transação ACID
deixou de ser possível — e um commit em duas fases (2PC) não é suportado entre Postgres, MongoDB e
Pub/Sub, além de acoplar a disponibilidade de todos os serviços.

## Alternativas consideradas

- **Coreografia** (cada serviço reage aos eventos dos outros, sem coordenador): menos acoplamento
  estrutural, mas a regra "o que vem depois de quê" e, principalmente, "o que desfazer quando algo
  falha" ficaria espalhada pelos três serviços. Com 6 etapas e 5 cenários de compensação, entender e
  depurar o fluxo exigiria juntar logs de três lugares.
- **Orquestração** (escolhida): o fluxo inteiro e as compensações ficam num lugar só, numa máquina de
  estados explícita (`EtapaSaga`), persistida (`sagas_ordem_servico` + `saga_passos`) e consultável
  pela API (`GET /api/ordens-servico/{id}/saga`). O OS Service já é o dono do agregado Ordem de
  Serviço e do status que o cliente acompanha, então é o candidato natural.

## Como a consistência é garantida

- **Transactional outbox**: o comando para o participante é gravado na tabela `outbox_mensagens` na
  mesma transação que muda a OS e a saga; um job publica no Pub/Sub. Não existe "mudou o status mas
  o comando não saiu".
- **Idempotência**: toda mensagem tem um id único; os consumidores registram o que já processaram
  (`mensagens_processadas`) e as regras de negócio também conferem o estado (ex.: orçamento já
  existe para a saga → só reenvia a resposta).
- **Guarda por etapa**: cada evento só é aceito na etapa em que faz sentido. Evento repetido ou fora
  de ordem é registrado no histórico da saga e ignorado.
- **Concorrência**: a saga tem lock otimista (`@Version`); duas réplicas tratando eventos da mesma
  saga ao mesmo tempo → uma falha, a mensagem volta para a fila e é reprocessada.
- **Prazos**: `VerificadorPrazosSaga` compensa sagas paradas esperando o cliente (aprovação 72h,
  pagamento 48h) ou o Billing (5 min), para não segurar peças do estoque indefinidamente.
- **Falhas de infraestrutura**: retry com backoff no Pub/Sub e dead letter depois de 5 tentativas
  (`oficina-saga-dlq-<ambiente>`).

## Consequências

- **Positivo**: o fluxo e as compensações estão documentados no código em um único lugar
  (`OrquestradorSagaOS`) e cobertos por cenários BDD (`src/test/resources/features`).
- **Positivo**: o estado de cada saga é auditável (passos com data/hora) e aparece na demonstração.
- **Negativo**: o OS Service conhece os comandos dos participantes (acoplamento de contrato) — mitigado
  mantendo os contratos simples (`TiposMensagem`) e versionáveis.
- **Negativo**: consistência eventual — entre o comando sair e a resposta chegar, a OS fica em um
  status de espera (`AGUARDANDO_*`). Isso é mostrado ao cliente como parte do fluxo.
