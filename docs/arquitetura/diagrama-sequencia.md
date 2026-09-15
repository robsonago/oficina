# Diagramas de Sequência — Fase 3

## 1. Autenticação de um cliente por CPF

Fluxo do endpoint serverless `oficina-auth-function` (repositório dedicado) — diferente do login de
funcionário (usuário/senha), que acontece direto na aplicação principal (`/api/auth/login`).

```mermaid
sequenceDiagram
    actor Cliente
    participant Fn as oficina-auth-function<br/>(Cloud Function)
    participant DB as Cloud SQL

    Cliente->>Fn: POST / {"cpf": "52998224725"}
    Fn->>Fn: valida formato/dígito verificador do CPF
    alt CPF inválido
        Fn-->>Cliente: 400 Bad Request
    end
    Fn->>DB: SELECT id, ativo FROM clientes WHERE documento = ?
    alt cliente não encontrado
        DB-->>Fn: 0 linhas
        Fn-->>Cliente: 404 Not Found
    else cliente inativo
        DB-->>Fn: ativo = false
        Fn-->>Cliente: 403 Forbidden
    else cliente ativo
        DB-->>Fn: id, ativo = true
        Fn->>Fn: gera JWT (claim role=CLIENTE, subject=CPF,<br/>mesma chave HMAC da aplicação principal)
        Fn-->>Cliente: 200 OK {"token": "..."}
    end

    Note over Cliente: usa o token nas próximas<br/>chamadas à API principal
    Cliente->>+App: GET /api/ordens-servico/{id}<br/>Authorization: Bearer <token>
    App->>App: JwtAuthenticationFilter valida assinatura/expiração,<br/>lê role=CLIENTE (sem consultar UserDetailsService)
    App-->>-Cliente: 200 OK
```

## 2. Abertura de uma Ordem de Serviço

Fluxo de `POST /api/ordens-servico`, do funcionário autenticado até a OS persistida.

```mermaid
sequenceDiagram
    actor Funcionario as Funcionário
    participant GW as API Gateway
    participant App as oficina-app
    participant UC as OrdemServicoUseCase
    participant DB as Cloud SQL

    Funcionario->>GW: POST /api/ordens-servico<br/>{documentoCliente, placaVeiculo, descricaoProblema, ...}
    GW->>App: encaminha requisição (path preservado)
    App->>App: JwtAuthenticationFilter valida token de funcionário
    App->>UC: criar(AbrirOrdemServicoCommand)

    UC->>DB: findByDocumento(cliente)
    alt cliente não encontrado
        DB-->>UC: vazio
        UC-->>App: RecursoNaoEncontradoException
        App-->>Funcionario: 404 Not Found
    end
    DB-->>UC: Cliente

    UC->>DB: findByPlaca(veículo)
    alt veículo não encontrado
        DB-->>UC: vazio
        UC-->>App: RecursoNaoEncontradoException
        App-->>Funcionario: 404 Not Found
    end
    DB-->>UC: Veículo

    UC->>UC: gera número da OS,<br/>monta OrdemServico (status=RECEBIDA, dataAbertura=agora)
    opt itens de serviço/peça informados
        UC->>DB: busca cada Servico/Peca por id
        UC->>UC: adiciona ItemServicoOS/ItemPecaOS
    end

    UC->>DB: save(OrdemServico)
    DB-->>UC: OrdemServico persistida (id gerado)
    UC-->>App: OrdemServico
    App-->>Funcionario: 201 Created {id, numero, status: "RECEBIDA", ...}

    Note over App,DB: no próximo scrape do New Relic, o gauge<br/>oficina_os_abertas_hoje já reflete essa OS
```

## Relacionado

- [`docs/arquitetura/diagrama-componentes.md`](diagrama-componentes.md) — visão estática de todos os
  componentes envolvidos.
- [`docs/rfcs/003-estrategia-autenticacao.md`](../rfcs/003-estrategia-autenticacao.md) — por que a
  autenticação por CPF é uma function separada em vez de um endpoint na aplicação principal.
