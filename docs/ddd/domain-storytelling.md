# Domain Storytelling — Oficina Mecânica

> Descrição narrativa dos fluxos do domínio usando a linguagem pictográfica do Domain Storytelling  
> (Stefan Hofer & Henning Schwentner). Cada história descreve como **atores** colaboram usando  
> **objetos de trabalho** para atingir um objetivo de negócio, expressando as regras do domínio  
> de forma compreensível por técnicos e não-técnicos.

---

## Notação Utilizada

Em Domain Storytelling, cada "frase pictográfica" tem a forma:

```
[Ator] ──[atividade]──► [Objeto de Trabalho] ──[complemento]──► [Destinatário]
```

Cada história é composta por uma sequência numerada dessas frases, acompanhada de um fluxo
visual em ASCII e de um quadro de regras de negócio. A notação segue as convenções do livro
**"Domain Storytelling: A Collaborative, Visual, and Agile Way to Build Domain-Driven Software"**.

---

## Atores do Sistema

| Ícone | Ator                | Papel no Domínio                                                        |
|-------|---------------------|-------------------------------------------------------------------------|
| 👤    | **Recepcionista**   | Cadastra clientes e veículos, abre OS, entrega o veículo ao cliente     |
| 🔧    | **Técnico**         | Executa diagnóstico, adiciona serviços/peças, finaliza a execução       |
| 🙋    | **Cliente**         | Proprietário do veículo; aprova ou rejeita o orçamento apresentado      |
| 🛡️   | **Administrador**   | Gerencia catálogo de serviços, estoque de peças, usuários e relatórios  |
| 🖥️   | **Sistema**         | Processa comandos, persiste dados, calcula totais e envia notificações  |
| 📧    | **Servidor de E-mail** | Sistema externo (Mailpit em dev/test) que recebe e entrega e-mails   |

---

## Objetos de Trabalho

| Ícone | Objeto de Trabalho            | Descrição                                                 |
|-------|-------------------------------|-----------------------------------------------------------|
| 🚗    | **Veículo**                   | Automóvel identificado pela placa (padrão antigo/Mercosul)|
| 👥    | **Cliente**                   | Pessoa física (CPF) ou jurídica (CNPJ)                    |
| 📋    | **Ordem de Serviço (OS)**     | Agregado raiz; representa o ciclo completo do atendimento |
| 💰    | **Orçamento**                 | Valor total calculado automaticamente a partir dos itens  |
| 🔩    | **Peça / Insumo**             | Material consumível com estoque controlado                |
| 🛠️   | **Serviço**                   | Tipo de trabalho com preço e tempo estimado de execução   |
| 📄    | **Item de OS**                | Associação de um serviço ou peça a uma OS (com quantidade)|
| 📧    | **Notificação de Orçamento**  | E-mail enviado ao cliente com o valor total aprovado      |
| 📊    | **Relatório de Estatísticas** | Métricas gerenciais das ordens de serviço                 |
| 🔐    | **Token JWT**                 | Credencial de acesso às APIs protegidas do sistema        |

---

## História 1 — Recepção do Veículo e Abertura de OS

### Contexto

> O cliente chega à oficina com um veículo que precisa de manutenção ou reparo.
> A recepcionista inicia o processo de atendimento registrando os dados no sistema.

### Frases Pictográficas

| # | Ator                  | Atividade             | Objeto de Trabalho            | Destinatário         |
|---|-----------------------|-----------------------|-------------------------------|----------------------|
| 1 | 🙋 Cliente            | apresenta             | 🚗 veículo e documentos       | 👤 Recepcionista     |
| 2 | 👤 Recepcionista      | busca cadastro de     | 👥 Cliente (CPF/CNPJ)         | 🖥️ Sistema           |
| 3 | 🖥️ Sistema            | retorna               | 👥 dados do cliente            | 👤 Recepcionista     |
| 4 | 👤 Recepcionista      | cadastra (se novo)    | 👥 Cliente com CPF/CNPJ       | 🖥️ Sistema           |
| 5 | 👤 Recepcionista      | cadastra (se novo)    | 🚗 Veículo com placa/modelo   | 🖥️ Sistema           |
| 6 | 👤 Recepcionista      | descreve              | problema relatado pelo cliente| 🖥️ Sistema           |
| 7 | 👤 Recepcionista      | cria                  | 📋 Ordem de Serviço           | 🖥️ Sistema           |
| 8 | 🖥️ Sistema            | gera automaticamente  | número único da OS             | 📋 OS                |
| 9 | 🖥️ Sistema            | registra status       | RECEBIDA                      | 📋 OS                |
| 10| 🖥️ Sistema            | entrega               | comprovante de recebimento    | 👤 Recepcionista     |

### Fluxo Visual

```
🙋 Cliente
    │
    │ (1) apresenta veículo e documentos
    ▼
👤 Recepcionista
    │
    ├──(2) busca cadastro do Cliente ────────────────────► 🖥️ Sistema
    │◄───────────────────────────── (3) retorna dados ───┤
    │                                                     │
    ├──(4) cadastra Cliente [se não existir] ────────────►│
    │                                                     │
    ├──(5) cadastra Veículo [se não existir] ────────────►│
    │                                                     │
    ├──(6) descreve problema relatado ───────────────────►│
    │                                                     │
    └──(7) cria Ordem de Serviço ────────────────────────►│
                                         (8) gera número único da OS
                                         (9) status ← RECEBIDA
                                                          │
    ◄──────────────── (10) entrega comprovante ───────────┘
```

### Regras de Negócio

- O cliente **deve estar cadastrado** antes de abrir uma OS
- O veículo deve estar **associado ao cliente** cadastrado
- O status inicial da OS é sempre `RECEBIDA`
- O número da OS é gerado automaticamente pelo sistema (ex.: `OS-2025-A1B2C3D4`)

---

## História 2 — Diagnóstico e Composição do Orçamento

### Contexto

> Com a OS aberta, o técnico avalia fisicamente o veículo, identifica os problemas
> e monta a lista de serviços e peças necessários para o reparo.

### Frases Pictográficas

| # | Ator           | Atividade              | Objeto de Trabalho              | Destinatário      |
|---|----------------|------------------------|---------------------------------|-------------------|
| 1 | 🔧 Técnico     | consulta               | 📋 OS em status RECEBIDA        | 🖥️ Sistema        |
| 2 | 🖥️ Sistema     | exibe                  | 📋 OS com dados do veículo      | 🔧 Técnico        |
| 3 | 🔧 Técnico     | inicia                 | diagnóstico                     | 🖥️ Sistema        |
| 4 | 🖥️ Sistema     | altera status para     | EM_DIAGNOSTICO                  | 📋 OS             |
| 5 | 🔧 Técnico     | avalia fisicamente     | 🚗 veículo                      | —                 |
| 6 | 🔧 Técnico     | adiciona               | 🛠️ Serviço à OS                 | 🖥️ Sistema        |
| 7 | 🖥️ Sistema     | valida e registra      | 📄 Item de Serviço               | 📋 OS             |
| 8 | 🔧 Técnico     | adiciona               | 🔩 Peça à OS (com quantidade)   | 🖥️ Sistema        |
| 9 | 🖥️ Sistema     | verifica disponibilidade| 🔩 estoque da Peça             | 🔩 Estoque        |
| 10| 🖥️ Sistema     | registra               | 📄 Item de Peça                  | 📋 OS             |
| 11| 🔧 Técnico     | solicita geração de    | 💰 Orçamento                    | 🖥️ Sistema        |
| 12| 🖥️ Sistema     | recalcula              | 💰 Σ (preço × qtd) de cada item | 📋 OS             |
| 13| 🖥️ Sistema     | altera status para     | AGUARDANDO_APROVACAO            | 📋 OS             |

### Fluxo Visual

```
🔧 Técnico
    │
    ├──(1) consulta OS em RECEBIDA ──────────────────────► 🖥️ Sistema
    │◄─────────────────────── (2) exibe OS com veículo ──┤
    │                                                     │
    ├──(3) inicia diagnóstico ───────────────────────────►│
    │                                   (4) OS → EM_DIAGNOSTICO
    │
    │  [avalia o veículo fisicamente — etapa off-line]
    │
    ├──(6) adiciona Serviço ─────────────────────────────► 🖥️ Sistema
    │                            (7) registra Item de Serviço na OS
    │
    ├──(8) adiciona Peça (quantidade) ──────────────────► 🖥️ Sistema
    │                            (9) verifica estoque disponível
    │                            (10) registra Item de Peça na OS
    │
    │  [passos 6-10 repetidos N vezes conforme diagnóstico]
    │
    └──(11) gera Orçamento ──────────────────────────────► 🖥️ Sistema
                                (12) Σ(preço × qtd) de todos os itens
                                (13) OS → AGUARDANDO_APROVACAO
```

### Regras de Negócio

- Serviços e peças só podem ser adicionados quando a OS está `RECEBIDA` ou `EM_DIAGNOSTICO`
- O estoque é **verificado** ao adicionar a peça, mas **não é debitado** ainda
- O valor do orçamento é recalculado automaticamente a cada item incluído
- A transição para `AGUARDANDO_APROVACAO` é disparada ao solicitar a geração do orçamento

---

## História 3 — Notificação por E-mail e Aprovação do Orçamento

### Contexto

> Com o orçamento calculado, o sistema notifica o cliente por e-mail.
> O cliente decide aprovar ou rejeitar o orçamento apresentado.

### Frases Pictográficas

| # | Ator                  | Atividade             | Objeto de Trabalho              | Destinatário           |
|---|-----------------------|-----------------------|---------------------------------|------------------------|
| 1 | 🖥️ Sistema            | detecta               | OS em AGUARDANDO_APROVACAO      | —                      |
| 2 | 🖥️ Sistema            | compõe                | 📧 Notificação de Orçamento     | —                      |
| 3 | 🖥️ Sistema            | envia via SMTP        | 📧 e-mail com valor total       | 📧 Servidor de E-mail  |
| 4 | 📧 Servidor de E-mail | entrega               | 📧 mensagem na caixa de entrada | 🙋 Cliente             |
| 5 | 🙋 Cliente            | lê                   | 💰 Orçamento recebido           | —                      |
| 6a| 🙋 Cliente            | **aprova**            | 💰 Orçamento                    | 🖥️ Sistema             |
| 7a| 🖥️ Sistema            | verifica e debita     | 🔩 estoque de cada peça da OS   | 🔩 Estoque             |
| 8a| 🖥️ Sistema            | registra              | dataInicio na OS                | 📋 OS                  |
| 9a| 🖥️ Sistema            | altera status para    | EM_EXECUCAO                     | 📋 OS                  |
| 6b| 🙋 Cliente            | **rejeita**           | 💰 Orçamento                    | 🖥️ Sistema             |
| 7b| 🖥️ Sistema            | altera status para    | EM_DIAGNOSTICO                  | 📋 OS                  |

### Fluxo Visual

```
                               🖥️ Sistema
                                   │
              (1) detecta OS → AGUARDANDO_APROVACAO
              (2) compõe e-mail com detalhes do orçamento
                                   │
              (3) envia SMTP ──────────────────────────► 📧 Servidor de E-mail
                                                                  │
                                               (4) entrega e-mail na caixa do cliente
                                                                  │
                                                                  ▼
                                                             🙋 Cliente
                                                                  │
                                               (5) lê e avalia o orçamento recebido
                                                                  │
                                          ┌───────────────────────┤
                                          │                       │
                                      [APROVA]               [REJEITA]
                                          │                       │
                                          ▼                       ▼
                                     🖥️ Sistema             🖥️ Sistema
                                          │                       │
             (7a) verifica e debita estoque            (7b) OS → EM_DIAGNOSTICO
             (8a) registra dataInicio                  [volta para revisão]
             (9a) OS → EM_EXECUCAO
```

### Regras de Negócio

- A notificação por e-mail é **disparada automaticamente** ao gerar o orçamento (política de domínio)
- O envio usa o **EmailPort** (porta de saída) para desacoplar o domínio do servidor SMTP
- Em ambiente de desenvolvimento/teste, o e-mail é capturado pelo **Mailpit**
- Se o estoque for insuficiente no momento da aprovação, erro `HTTP 422 Unprocessable Entity`
- A aprovação registra `dataInicio` automaticamente na OS
- A rejeição retorna a OS para `EM_DIAGNOSTICO`, permitindo revisão e novo orçamento

---

## História 4 — Execução dos Serviços e Entrega do Veículo

### Contexto

> Com a OS aprovada, o técnico executa os serviços. Concluído o trabalho,
> a recepcionista entrega o veículo ao cliente e fecha o ciclo de atendimento.

### Frases Pictográficas

| # | Ator                 | Atividade              | Objeto de Trabalho           | Destinatário       |
|---|----------------------|------------------------|------------------------------|--------------------|
| 1 | 🔧 Técnico           | consulta               | 📋 OS em EM_EXECUCAO         | 🖥️ Sistema         |
| 2 | 🖥️ Sistema           | exibe                  | 📋 OS com itens aprovados    | 🔧 Técnico         |
| 3 | 🔧 Técnico           | executa                | 🛠️ serviços e usa 🔩 peças   | 🚗 veículo         |
| 4 | 🔧 Técnico           | finaliza               | execução dos serviços        | 🖥️ Sistema         |
| 5 | 🖥️ Sistema           | registra               | dataFinalização na OS        | 📋 OS              |
| 6 | 🖥️ Sistema           | altera status para     | FINALIZADA                   | 📋 OS              |
| 7 | 👤 Recepcionista     | consulta               | 📋 OS em FINALIZADA          | 🖥️ Sistema         |
| 8 | 🙋 Cliente           | retorna                | à oficina para buscar 🚗     | —                  |
| 9 | 👤 Recepcionista     | entrega                | 🚗 veículo reparado          | 🙋 Cliente         |
| 10| 👤 Recepcionista     | registra entrega       | 📋 OS                         | 🖥️ Sistema         |
| 11| 🖥️ Sistema           | registra               | dataEntrega na OS            | 📋 OS              |
| 12| 🖥️ Sistema           | altera status para     | ENTREGUE (status terminal)   | 📋 OS              |

### Fluxo Visual

```
🔧 Técnico
    │
    ├──(1) consulta OS em EM_EXECUCAO ───────────────────► 🖥️ Sistema
    │◄──────────────────────── (2) exibe itens aprovados ┤
    │
    │  (3) executa serviços e aplica peças no 🚗 veículo
    │      [etapa física — off-line]
    │
    ├──(4) finaliza execução ────────────────────────────► 🖥️ Sistema
                                        (5) registra dataFinalização
                                        (6) OS → FINALIZADA
                                                          │
                         ┌────────────────────────────────┘
                         │
                    👤 Recepcionista
                         │
                         ├──(7) consulta OS FINALIZADA ──► 🖥️ Sistema
                         │
                    🙋 Cliente retorna à oficina (8)
                         │
                         ├──(9) entrega 🚗 ao Cliente ────────────────► 🙋 Cliente
                         │
                         └──(10) registra entrega ───────► 🖥️ Sistema
                                            (11) registra dataEntrega
                                            (12) OS → ENTREGUE  [fim do ciclo]
```

### Regras de Negócio

- A finalização da execução só é permitida quando a OS está `EM_EXECUCAO`
- `dataFinalização` é registrada **automaticamente** pelo sistema
- `ENTREGUE` é o **status terminal** — nenhuma transição posterior é possível
- O tempo de execução é calculado como `dataFinalização − dataInicio` (em minutos)

---

## História 5 — Consulta de Status pelo Cliente (Endpoint Público)

### Contexto

> O cliente pode acompanhar o andamento da sua OS de qualquer lugar,
> sem necessidade de login ou autenticação.

### Frases Pictográficas

| # | Ator           | Atividade       | Objeto de Trabalho          | Destinatário    |
|---|----------------|-----------------|-----------------------------|-----------------|
| 1 | 🙋 Cliente     | acessa          | app web ou mobile           | —               |
| 2 | 🙋 Cliente     | informa         | número (id) da OS           | 🖥️ Sistema      |
| 3 | 🖥️ Sistema     | busca           | 📋 OS pelo identificador    | 📋 OS           |
| 4 | 🖥️ Sistema     | retorna         | status atual da OS          | 🙋 Cliente      |

### Fluxo Visual

```
🙋 Cliente
    │
    │ (1) acessa aplicação web ou mobile
    │
    ├──(2) informa o número/id da OS ───────────────────► 🖥️ Sistema
    │                                                          │
    │                                  (3) busca OS pelo id   │
    │                                                          │
    │◄─────────────────────────── (4) retorna status ──────── ┘

  Resposta JSON:
  ┌─────────────────────────────────────────┐
  │  {                                      │
  │    "ordemServicoId": 42,                │
  │    "status": "Em Execução"              │
  │  }                                      │
  └─────────────────────────────────────────┘
```

### Regras de Negócio

- Endpoint **público** — não requer token JWT
- Retorna apenas o status atual (sem dados financeiros ou pessoais)
- Rota: `GET /api/ordem-servico/{id}/status`
- Em caso de OS não encontrada → `HTTP 404 Not Found`

---

## História 6 — Gestão do Catálogo de Serviços e Estoque de Peças

### Contexto

> O administrador mantém o catálogo de serviços e o estoque de peças atualizado,
> garantindo que os técnicos possam compor orçamentos precisos.

### Frases Pictográficas — Autenticação

| # | Ator              | Atividade      | Objeto de Trabalho             | Destinatário         |
|---|-------------------|----------------|-------------------------------|----------------------|
| 1 | 🛡️ Administrador  | informa        | credenciais (usuário/senha)   | 🖥️ Sistema           |
| 2 | 🖥️ Sistema        | valida e gera  | 🔐 Token JWT (role ADMIN)     | 🛡️ Administrador     |

### Frases Pictográficas — Catálogo de Serviços

| # | Ator              | Atividade      | Objeto de Trabalho              | Destinatário      |
|---|-------------------|----------------|---------------------------------|-------------------|
| 3 | 🛡️ Administrador  | cadastra       | 🛠️ Serviço (nome/preço/tempo)  | 🖥️ Sistema        |
| 4 | 🖥️ Sistema        | persiste       | 🛠️ Serviço no catálogo         | 🛠️ Catálogo        |
| 5 | 🛡️ Administrador  | atualiza       | 🛠️ preço ou tempo estimado     | 🖥️ Sistema        |
| 6 | 🛡️ Administrador  | consulta       | 🛠️ lista de Serviços            | 🖥️ Sistema        |
| 7 | 🖥️ Sistema        | retorna        | 📊 lista paginada de serviços  | 🛡️ Administrador  |
| 8 | 🛡️ Administrador  | remove         | 🛠️ Serviço sem uso em OS ativas| 🖥️ Sistema        |

### Frases Pictográficas — Estoque de Peças

| # | Ator              | Atividade      | Objeto de Trabalho               | Destinatário      |
|---|-------------------|----------------|----------------------------------|-------------------|
| 9 | 🛡️ Administrador  | cadastra       | 🔩 Peça (nome/preço/estoque)    | 🖥️ Sistema        |
| 10| 🖥️ Sistema        | persiste       | 🔩 Peça com quantidade inicial  | 🔩 Estoque        |
| 11| 🛡️ Administrador  | atualiza       | 🔩 quantidade em estoque        | 🖥️ Sistema        |
| 12| 🖥️ Sistema        | registra       | reposição de estoque             | 🔩 Peça           |
| 13| 🛡️ Administrador  | consulta       | 🔩 relatório de estoque          | 🖥️ Sistema        |
| 14| 🖥️ Sistema        | retorna        | 📊 peças com quantidades atuais | 🛡️ Administrador  |

### Fluxo Visual

```
🛡️ Administrador
    │
    ├──(1) informa credenciais ───────────────────────────► 🖥️ Sistema
    │◄───────────────────────────── (2) emite Token JWT ──┤
    │                                                      │
    │  [usa Token JWT no header Authorization de todas as requisições]
    │
    │  ── CATÁLOGO DE SERVIÇOS ──
    │
    ├──(3) POST /api/servico  [cadastra] ───────────────► 🖥️ Sistema ──► 🛠️ Catálogo
    ├──(5) PUT  /api/servico/{id} [atualiza preço/tempo]
    ├──(6) GET  /api/servico  [lista] ──────────────────► 🖥️ Sistema
    │◄──────────────────────────── (7) lista paginada ──┤
    ├──(8) DELETE /api/servico/{id} [remove]
    │
    │  ── ESTOQUE DE PEÇAS ──
    │
    ├──(9)  POST /api/peca  [cadastra com estoque] ─────► 🖥️ Sistema ──► 🔩 Estoque
    ├──(11) PUT  /api/peca/{id} [atualiza estoque]
    ├──(13) GET  /api/peca  [relatório] ────────────────► 🖥️ Sistema
    │◄────────────────── (14) peças com quantidades ────┤
```

### Regras de Negócio

- Todas as rotas de gestão requerem autenticação JWT com role `ADMIN` ou `TECNICO`
- O estoque é **debitado automaticamente** na aprovação do orçamento (não manualmente)
- O estoque não pode ficar negativo — a aprovação é bloqueada com `HTTP 422` se insuficiente
- Ao adicionar uma peça à OS, o preço unitário vigente é **capturado no momento** da adição

---

## História 7 — Monitoramento Administrativo

### Contexto

> O administrador acompanha o desempenho operacional da oficina por meio de
> estatísticas e listagens filtradas das ordens de serviço.

### Frases Pictográficas

| # | Ator              | Atividade        | Objeto de Trabalho                | Destinatário        |
|---|-------------------|------------------|-----------------------------------|---------------------|
| 1 | 🛡️ Administrador  | solicita         | 📊 estatísticas das OS            | 🖥️ Sistema          |
| 2 | 🖥️ Sistema        | agrega           | dados de todas as OS              | 📊 Relatório        |
| 3 | 🖥️ Sistema        | calcula          | tempo médio de execução (min)     | 📊 Relatório        |
| 4 | 🖥️ Sistema        | conta            | total de OS por status            | 📊 Relatório        |
| 5 | 🖥️ Sistema        | retorna          | 📊 Relatório de Estatísticas      | 🛡️ Administrador    |
| 6 | 🛡️ Administrador  | filtra           | 📋 OS por status específico       | 🖥️ Sistema          |
| 7 | 🖥️ Sistema        | retorna ordenado | 📋 lista de OS por data criação   | 🛡️ Administrador    |

### Fluxo Visual

```
🛡️ Administrador
    │
    ├──(1) GET /api/ordem-servico/estatisticas ──────────► 🖥️ Sistema
    │                                                           │
    │                           (2) agrega dados de todas as OS │
    │                           (3) calcula tempo médio de execução
    │                           (4) conta OS agrupadas por status
    │                                                           │
    │◄─────────────────────── (5) retorna estatísticas ─────── ┘

  Resposta JSON:
  ┌────────────────────────────────────────────────────────┐
  │  {                                                     │
  │    "tempoMedioExecucaoMinutos": 180,                   │
  │    "totalOSFinalizadas": 45,                           │
  │    "totalOSPorStatus": {                               │
  │      "recebida": 3,                                    │
  │      "emDiagnostico": 2,                               │
  │      "aguardandoAprovacao": 1,                         │
  │      "emExecucao": 5,                                  │
  │      "finalizada": 20,                                 │
  │      "entregue": 14                                    │
  │    }                                                   │
  │  }                                                     │
  └────────────────────────────────────────────────────────┘
    │
    ├──(6) GET /api/ordem-servico?status=EM_EXECUCAO ─────► 🖥️ Sistema
    │◄──────────────── (7) lista filtrada, ordenada por data ┤
```

### Regras de Negócio

- O tempo médio de execução considera apenas OS no status `FINALIZADA` ou `ENTREGUE`
- A listagem por status é ordenada por `dataCriacao` (mais recentes primeiro)
- Ambos os endpoints requerem autenticação JWT

---

## Resumo das Histórias

| # | História                                | Ator Principal            | Objetivo de Negócio                              |
|---|-----------------------------------------|---------------------------|--------------------------------------------------|
| 1 | Recepção e Abertura de OS               | 👤 Recepcionista          | Registrar chegada do veículo e iniciar atendimento|
| 2 | Diagnóstico e Composição do Orçamento   | 🔧 Técnico                | Avaliar o veículo e montar a lista de trabalho   |
| 3 | Notificação por E-mail e Aprovação      | 🙋 Cliente + 🖥️ Sistema   | Comunicar o valor e obter autorização do cliente |
| 4 | Execução dos Serviços e Entrega         | 🔧 Técnico + 👤 Recepcionista | Executar o reparo e fechar o ciclo de atendimento|
| 5 | Consulta de Status (Pública)            | 🙋 Cliente                | Acompanhar a OS em tempo real, sem autenticação  |
| 6 | Gestão do Catálogo e Estoque            | 🛡️ Administrador          | Manter serviços e peças atualizados              |
| 7 | Monitoramento Administrativo            | 🛡️ Administrador          | Analisar desempenho operacional da oficina       |

---

## Granularidade das Histórias

Domain Storytelling distingue dois níveis de granularidade:

| Nível           | Características                                            | Histórias neste doc |
|-----------------|------------------------------------------------------------|---------------------|
| **Coarse-Grained** (nível de domínio) | Mostra o que acontece sem detalhar como | H1, H4, H5       |
| **Fine-Grained** (nível de aplicação)  | Detalha as interações com o sistema     | H2, H3, H6, H7   |

---

## Conexão com Outros Artefatos DDD

| Artefato DDD              | Relação com este documento                                                                  |
|---------------------------|---------------------------------------------------------------------------------------------|
| **Event Storming**        | Cada história produz os eventos de domínio mapeados no Event Storming (ex.: `OrcamentoGerado`) |
| **Agregados e Entidades** | OS, Peça e Serviço são os agregados raiz que participam de todas as histórias               |
| **Contextos Delimitados** | H1–H4 pertencem ao contexto de OS; H6 ao contexto de Catálogo e Estoque; H7 é transversal  |
| **Linguagem Ubíqua**      | Todos os termos (OS, Orçamento, Diagnóstico, Entrega) seguem o glossário da linguagem ubíqua|
