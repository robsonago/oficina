# Linguagem Ubíqua — Oficina Mecânica

> Glossário dos termos do domínio compartilhado entre time técnico e stakeholders.  
> Todos os nomes de classes, endpoints e campos do sistema seguem este vocabulário.

---

## Termos do Domínio

### Cliente

Pessoa física (CPF) ou jurídica (CNPJ) que traz um veículo à oficina para manutenção ou reparo.  
Identificado de forma única pelo seu **documento** (CPF ou CNPJ).  
Um cliente pode possuir múltiplos **veículos**.

### Veículo

Automóvel pertencente a um **cliente**, identificado pela **placa** (padrão antigo ou Mercosul).  
Atributos essenciais: placa, marca, modelo, ano.  
Um veículo pode ter múltiplas **ordens de serviço** ao longo do tempo.

### Ordem de Serviço (OS)

**Agregado raiz** do domínio. Representa o ciclo de vida completo de um atendimento: desde a chegada do veículo até sua
entrega ao cliente.  
Identificada por um **número** gerado automaticamente (ex: `OS-2025-A1B2C3D4`).  
Contém: cliente, veículo, lista de itens de serviço, lista de itens de peça, status e orçamento.

### Diagnóstico

Fase em que o **técnico** avalia o veículo para identificar os problemas e definir quais **serviços** e **peças** são
necessários.  
Corresponde ao status `EM_DIAGNOSTICO` na OS.

### Orçamento

Valor total calculado automaticamente com base nos **itens de serviço** e **itens de peça** incluídos na OS.  
Fórmula: `Σ (preço_unitário × quantidade)` de todos os itens.  
Enviado ao cliente para aprovação antes da execução (status `AGUARDANDO_APROVACAO`).

### Aprovação

Confirmação do cliente de que concorda com o **orçamento** e autoriza a execução dos serviços.  
Aciona a transição para `EM_EXECUCAO` e debita automaticamente o **estoque** das peças.

### Rejeição

Recusa do cliente ao orçamento apresentado.  
Retorna a OS para `EM_DIAGNOSTICO` para reavaliação.

### Serviço

Tipo de trabalho realizado na oficina (ex: troca de óleo, alinhamento, balanceamento).  
Possui **preço** e **tempo estimado de execução** (em minutos).

### Peça / Insumo

Material consumível utilizado durante a execução do serviço (ex: filtro de óleo, pastilha de freio, fluido de freio).  
Possui **preço unitário** e **quantidade em estoque** controlada pelo sistema.

### Estoque

Quantidade disponível de cada **peça** no almoxarifado da oficina.  
É debitado automaticamente quando a OS é aprovada pelo cliente e transita para `EM_EXECUCAO`.

### Item de Serviço

Associação de um **serviço** a uma **OS**, com quantidade e preço unitário registrados no momento da inclusão.

### Item de Peça

Associação de uma **peça** a uma **OS**, com quantidade e preço unitário registrados no momento da inclusão.

### Técnico

Funcionário da oficina responsável por executar o diagnóstico e os serviços autorizados.  
No sistema, corresponde ao role `TECNICO` para acesso às APIs administrativas.

### Administrador

Responsável pela gestão administrativa do sistema (cadastros, relatórios, usuários).  
Corresponde ao role `ADMIN`.

### Tempo de Execução

Intervalo entre o início da execução (`EM_EXECUCAO`) e a finalização (`FINALIZADA`) de uma OS.  
Medido em minutos. O sistema calcula o **tempo médio de execução** para fins de monitoramento.

### Entrega

Ato de devolver o veículo ao cliente após a finalização dos serviços.  
Corresponde ao status `ENTREGUE` — estado terminal da OS.

---

## Status da Ordem de Serviço

| Status                 | Descrição                              | Gatilho                |
|------------------------|----------------------------------------|------------------------|
| `RECEBIDA`             | OS criada, veículo recebido na oficina | Criação da OS          |
| `EM_DIAGNOSTICO`       | Técnico avalia o veículo               | Ação do técnico        |
| `AGUARDANDO_APROVACAO` | Orçamento gerado e enviado ao cliente  | Geração do orçamento   |
| `EM_EXECUCAO`          | Cliente aprovou; serviços em andamento | Aprovação do cliente   |
| `FINALIZADA`           | Todos os serviços concluídos           | Conclusão pelo técnico |
| `ENTREGUE`             | Veículo entregue ao cliente            | Ação de entrega        |

---

## Contextos Delimitados (Bounded Contexts)

| Contexto                          | Responsabilidade                                                   |
|-----------------------------------|--------------------------------------------------------------------|
| **Gestão de Clientes e Veículos** | Cadastro e consulta de clientes (CPF/CNPJ) e seus veículos (placa) |
| **Gestão de Ordens de Serviço**   | Ciclo de vida completo da OS, orçamento, status e timeline         |
| **Catálogo de Serviços**          | CRUD dos tipos de serviço com preço e tempo estimado               |
| **Estoque de Peças e Insumos**    | CRUD de peças com controle de quantidade em estoque                |
| **Identidade e Acesso**           | Autenticação JWT, usuários e roles (ADMIN, TECNICO)                |
