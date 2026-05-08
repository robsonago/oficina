# Event Storming — Oficina Mecânica

> Mapeamento completo dos eventos de domínio, comandos, atores, políticas e modelos de leitura  
> seguindo a notação de Event Storming (Alberto Brandolini).

---

## Legenda de Cores

| Cor                | Tipo                  | Descrição                                             |
|--------------------|-----------------------|-------------------------------------------------------|
| 🟠 **Laranja**     | **Evento de Domínio** | Algo que aconteceu no domínio (passado)               |
| 🔵 **Azul**        | **Comando**           | Intenção de causar uma mudança de estado              |
| 🟡 **Amarelo**     | **Ator**              | Quem dispara o comando                                |
| 🟣 **Lilás**       | **Política**          | Reação automática a um evento ("quando X, então Y")   |
| 🟢 **Verde**       | **Modelo de Leitura** | Dado consultado antes de um comando                   |
| 🔴 **Rosa/Salmão** | **Agregado**          | Entidade raiz que processa o comando e emite o evento |
| ⚠️ **Vermelho**    | **Hotspot**           | Ponto de dúvida ou risco identificado                 |

---

## Fluxo 1 — Criação e Acompanhamento da Ordem de Serviço

### Fase: Recepção do Veículo

```
🟡 Recepcionista
      │
      ▼
🔵 CadastrarCliente (se não existir)
      │
      ▼
🟠 ClienteCadastrado
      │
      ▼
🔵 CadastrarVeiculo
      │
      ▼
🟠 VeiculoCadastrado
      │
      ▼
🔵 CriarOrdemDeServico
      │ (requer: CPF/CNPJ cliente, placa veículo,
      │  descrição do problema, lista de serviços/peças)
      │
🔴 OrdemServico
      │
      ▼
🟠 OrdemDeServicoCriada   [status: RECEBIDA]
      │
      ▼
🟢 Comprovante de recebimento gerado para o cliente
```

**Hotspots:**

- ⚠️ E se o veículo não estiver cadastrado? → Validação obrigatória antes de criar a OS
- ⚠️ E se as peças não tiverem estoque suficiente? → Verificação no momento da criação

---

### Fase: Diagnóstico

```
🟡 Técnico
      │
      ▼
🟢 Consultar OS (status = RECEBIDA)
      │
      ▼
🔵 IniciarDiagnostico
      │
🔴 OrdemServico
      │
      ▼
🟠 DiagnosticoIniciado   [status: EM_DIAGNOSTICO]
      │
      ▼
🔵 AdicionarServicoNaOS   (pode ocorrer N vezes)
🔵 AdicionarPecaNaOS      (pode ocorrer N vezes)
      │
      ▼
🟠 ServicoAdicionadoNaOS
🟠 PecaAdicionadaNaOS
```

**Hotspots:**

- ⚠️ Técnico pode adicionar serviços durante o diagnóstico sem geração de orçamento ainda
- ⚠️ Estoque deve ser verificado ao adicionar peça, mas debitado apenas na aprovação

---

### Fase: Orçamento e Aprovação

```
🟡 Técnico
      │
      ▼
🔵 GerarOrcamento
      │ (recalcula valorTotal = Σ itens)
      │
🔴 OrdemServico
      │
      ▼
🟠 OrcamentoGerado   [status: AGUARDANDO_APROVACAO]
      │
      ▼
🟣 Política: Enviar notificação do orçamento ao cliente
      │
      ▼
🟡 Cliente
      │
      ├──[Aprova]──►🔵 AprovarOrcamento
      │                    │
      │               🔴 OrdemServico
      │                    │
      │                    ▼
      │               🟠 OrcamentoAprovado   [status: EM_EXECUCAO]
      │                    │
      │               🟣 Política: DebitarEstoqueDasPecas
      │                    │
      │                    ▼
      │               🟠 EstoqueDebitado
      │                    │
      │               🟢 dataInicio registrada automaticamente
      │
      └──[Rejeita]──►🔵 RejeitarOrcamento
                           │
                      🔴 OrdemServico
                           │
                           ▼
                      🟠 OrcamentoRejeitado   [status: EM_DIAGNOSTICO]
                           │
                      (volta para fase de diagnóstico)
```

**Hotspots:**

- ⚠️ Se o estoque for insuficiente no momento da aprovação → bloquear com erro 422
- ⚠️ Canal de notificação ao cliente (MVP usa API pull; futuro: e-mail/SMS)

---

### Fase: Execução e Entrega

```
🟡 Técnico
      │
      ▼
🔵 FinalizarExecucao
      │
🔴 OrdemServico
      │
      ▼
🟠 ExecucaoFinalizada   [status: FINALIZADA]
      │
🟢 dataFinalizacao registrada automaticamente
      │
      ▼
🟡 Recepcionista
      │
      ▼
🔵 EntregarVeiculo
      │
🔴 OrdemServico
      │
      ▼
🟠 VeiculoEntregue   [status: ENTREGUE]
      │
🟢 dataEntrega registrada automaticamente
```

---

### Modelo de Leitura: Acompanhamento pelo Cliente (API Pública)

```
🟡 Cliente (via app/web)
      │
      ▼
🔵 ConsultarStatusDaOS  (endpoint público, sem autenticação)
      │
      ▼
🟢 StatusDaOS
      {
        "ordemServicoId": 42,
        "status": "Em Execução"
      }
```

---

## Fluxo 2 — Gestão de Peças e Insumos

### Cadastro de Peças

```
🟡 Administrador
      │
      ▼
🔵 CadastrarPeca
      │ (nome, descrição, preçoUnitario, quantidadeEstoque, codigoReferencia)
      │
      ▼
🟠 PecaCadastrada
      │
      ▼
🔵 AtualizarEstoque  (reposição manual de estoque)
      │
      ▼
🟠 EstoqueAtualizado
```

### Débito Automático de Estoque

```
🟠 OrcamentoAprovado
      │
      ▼
🟣 Política: ParaCadaItemPecaNaOS → DebitarQuantidadeDoEstoque
      │
      ▼
🔴 Peca (aggregate)
      │
      ▼
🟠 EstoqueDebitado
      │
      └─► Se quantidadeEstoque < quantidadeSolicitada:
              🟠 EstoqueInsuficienteDetectado  [BLOQUEIA aprovação — HTTP 422]
```

### Leitura de Estoque

```
🟡 Administrador
      │
      ▼
🔵 ListarPecas / BuscarPecaPorId
      │
      ▼
🟢 RelatorioDePecas
      {
        "id", "nome", "quantidadeEstoque", "precoUnitario", "codigoReferencia"
      }
```

---

## Fluxo 3 — Monitoramento Administrativo

```
🟡 Administrador
      │
      ▼
🔵 ConsultarEstatisticas
      │
      ▼
🟢 EstatisticasDasOS
      {
        "tempoMedioExecucaoMinutos": 180,
        "totalOSFinalizadas": 45,
        "totalOSPorStatus": {
          "recebida": 3,
          "emDiagnostico": 2,
          "aguardandoAprovacao": 1,
          "emExecucao": 5,
          "finalizada": 20,
          "entregue": 14
        }
      }
```

---

## Resumo de Eventos por Contexto

### Contexto: Ordens de Serviço

| Evento                 | Gatilho             | Status Resultante    |
|------------------------|---------------------|----------------------|
| `OrdemDeServicoCriada` | CriarOrdemDeServico | RECEBIDA             |
| `DiagnosticoIniciado`  | IniciarDiagnostico  | EM_DIAGNOSTICO       |
| `OrcamentoGerado`      | GerarOrcamento      | AGUARDANDO_APROVACAO |
| `OrcamentoAprovado`    | AprovarOrcamento    | EM_EXECUCAO          |
| `OrcamentoRejeitado`   | RejeitarOrcamento   | EM_DIAGNOSTICO       |
| `ExecucaoFinalizada`   | FinalizarExecucao   | FINALIZADA           |
| `VeiculoEntregue`      | EntregarVeiculo     | ENTREGUE             |

### Contexto: Estoque

| Evento                         | Gatilho                 |
|--------------------------------|-------------------------|
| `PecaCadastrada`               | CadastrarPeca           |
| `EstoqueAtualizado`            | AtualizarEstoque        |
| `EstoqueDebitado`              | Política: pós-aprovação |
| `EstoqueInsuficienteDetectado` | Verificação no débito   |

### Contexto: Clientes e Veículos

| Evento              | Gatilho          |
|---------------------|------------------|
| `ClienteCadastrado` | CadastrarCliente |
| `ClienteAtualizado` | AtualizarCliente |
| `VeiculoCadastrado` | CadastrarVeiculo |
| `VeiculoAtualizado` | AtualizarVeiculo |
