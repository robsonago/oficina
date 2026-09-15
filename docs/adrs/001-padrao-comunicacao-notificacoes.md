# ADR-001 — Comunicação assíncrona (Pub/Sub) para notificações

**Status:** Aceito · **Fase:** 3

## Decisão

As notificações por e-mail (aprovação de orçamento, etc.) deixam de ser enviadas de forma síncrona
pela própria aplicação (Fase 2: `JavaMailSenderEmailAdapter` chamando o SMTP diretamente dentro da
requisição HTTP) e passam a ser **publicadas como evento no Pub/Sub**, consumidas por uma Cloud
Function dedicada (`oficina-notification-function`) que faz o envio de fato.

```
App → publica em tópico Pub/Sub → Cloud Function consome → envia e-mail (SMTP)
```

## Contexto

Enviar e-mail é uma chamada de rede para um serviço externo (SMTP) que pode estar lento ou
indisponível. Fazer isso de forma síncrona, dentro da mesma requisição HTTP que gera o orçamento,
significa que uma falha ou lentidão do provedor de e-mail vira uma falha ou lentidão visível para o
funcionário usando o sistema — mesmo a operação principal (gerar o orçamento) tendo sido concluída
com sucesso no banco.

## Alternativas consideradas

- **Manter síncrono, com retry local**: mais simples, mas ainda acopla a disponibilidade do SMTP à
  latência percebida da API, e um retry em processo se perde se o pod reiniciar no meio.
- **Fila gerenciada (Pub/Sub) + consumidor assíncrono** (escolhida): desacopla completamente — a API
  responde assim que o evento é publicado (operação rápida e confiável dentro do mesmo projeto GCP),
  e o consumidor lida com toda a complexidade de retry/backoff do envio de e-mail, sem impactar a
  latência percebida pelo funcionário.

## Consequências

- **Positivo**: a criação/aprovação de OS não fica mais lenta nem menos confiável por causa do
  provedor de e-mail; falhas de envio (registradas via `NegocioMetrics.registrarErroIntegracao`) não
  derrubam a requisição original.
- **Positivo**: o consumidor (`oficina-notification-function`) escala e falha independente da
  aplicação principal — um pico de notificações não compete por CPU/memória com o tráfego de API.
- **Negativo**: a confirmação de que o e-mail foi enviado não é mais imediata nem visível na resposta
  da API — se o envio falhar, isso só aparece nas métricas/alertas do New Relic, não numa mensagem de
  erro para o funcionário.
- **Negativo**: mais uma peça de infraestrutura para operar (tópico Pub/Sub por ambiente, mais uma
  Cloud Function) e mais uma superfície de configuração (dois adapters de e-mail coexistindo no
  código — `PubSubEmailAdapter` e `JavaMailSenderEmailAdapter` — selecionados por uma flag dedicada,
  não por presença de propriedade, depois de um bug de ambiguidade de bean descoberto em produção).
