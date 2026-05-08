# Contextos Delimitados (Bounded Contexts)

---

## Visão Geral

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        OFICINA MECÂNICA - SISTEMA                       │
│                                                                         │
│  ┌──────────────────────┐     ┌───────────────────────────────────────┐ │
│  │  CLIENTES E VEÍCULOS │     │       ORDENS DE SERVIÇO               │ │
│  │                      │────►│                                       │ │
│  │  • Cliente (CPF/CNPJ)│     │  • OrdemServico (Agregado Raiz)       │ │
│  │  • Veiculo (placa)   │     │  • ItemServicoOS                      │ │
│  │                      │     │  • ItemPecaOS                         │ │
│  │  Entidades:          │     │  • StatusOS (enum)                    │ │
│  │  - Cliente           │     │                                       │ │
│  │  - Veiculo           │     │  Fluxo: RECEBIDA → EM_DIAGNOSTICO     │ │
│  └──────────────────────┘     │         → AGUARDANDO_APROVACAO        │ │
│            ▲                  │         → EM_EXECUCAO                 │ │
│            │                  │         → FINALIZADA → ENTREGUE       │ │
│            │                  └───────────────────────────────────────┘ │
│  ┌──────────────────────┐              │                                 │
│  │  CATÁLOGO DE SERVIÇOS│              │ usa                             │
│  │                      │◄─────────────┤                                 │
│  │  • Servico           │              │                                 │
│  │  • nome, preço,      │     ┌────────▼──────────────────────────────┐ │
│  │    tempoEstimado     │     │     ESTOQUE DE PEÇAS E INSUMOS        │ │
│  └──────────────────────┘     │                                       │ │
│                               │  • Peca (nome, preço, qtdEstoque)    │ │
│  ┌──────────────────────┐     │  • Controle de débito automático      │ │
│  │  IDENTIDADE E ACESSO │     │    na aprovação do orçamento          │ │
│  │                      │     └───────────────────────────────────────┘ │
│  │  • Usuario           │                                               │
│  │  • JWT (Bearer)      │                                               │
│  │  • Roles: ADMIN,     │                                               │
│  │    TECNICO           │                                               │
│  └──────────────────────┘                                               │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Detalhamento por Contexto

### 1. Clientes e Veículos

**Responsabilidade:** Cadastro e manutenção dos clientes (PF/PJ) e seus veículos.

| Elemento           | Tipo                              | Descrição                                          |
|--------------------|-----------------------------------|----------------------------------------------------|
| `Cliente`          | Entidade                          | Identificada por CPF ou CNPJ                       |
| `Veiculo`          | Entidade                          | Pertence a um `Cliente`, identificada por placa    |
| `TipoDocumento`    | Enum                              | CPF ou CNPJ                                        |
| `CpfCnpjValidator` | Value Object / Serviço de domínio | Valida CPF e CNPJ com algoritmo da Receita Federal |
| `PlacaValidator`   | Value Object / Serviço de domínio | Valida placa padrão antigo e Mercosul              |

**Invariantes:**

- Documento (CPF/CNPJ) deve ser único no sistema
- Placa de veículo deve ser única no sistema
- CPF/CNPJ deve ser matematicamente válido

---

### 2. Gestão de Ordens de Serviço

**Responsabilidade:** Ciclo de vida completo de um atendimento — do recebimento à entrega.

| Elemento        | Tipo                         | Descrição                                      |
|-----------------|------------------------------|------------------------------------------------|
| `OrdemServico`  | **Agregado Raiz**            | Contém todos os dados do atendimento           |
| `ItemServicoOS` | Entidade (parte do agregado) | Serviço incluído na OS com preço snapshot      |
| `ItemPecaOS`    | Entidade (parte do agregado) | Peça incluída na OS com preço snapshot         |
| `StatusOS`      | Enum                         | 6 estados com regras de transição encapsuladas |
| `numero`        | Value Object                 | Identificador legível da OS (ex: OS-2025-A1B2) |

**Invariantes:**

- `valorTotal` é sempre recalculado antes da transição para `AGUARDANDO_APROVACAO`
- Transições de status seguem um fluxo definido (ver `StatusOS.podeTransicionarPara()`)
- `dataInicio` é registrada automaticamente na transição para `EM_EXECUCAO`
- `dataFinalizacao` é registrada automaticamente na transição para `FINALIZADA`
- `dataEntrega` é registrada automaticamente na transição para `ENTREGUE`
- Preços dos itens são capturados (snapshot) no momento da inclusão na OS

**Política crítica:**
> Quando `OrcamentoAprovado` → debitar estoque de cada `ItemPecaOS`  
> Se estoque insuficiente → bloquear aprovação (`EstoqueInsuficienteException`)

---

### 3. Catálogo de Serviços

**Responsabilidade:** Tabela de preços e catálogo dos serviços oferecidos pela oficina.

| Elemento  | Tipo     | Descrição                              |
|-----------|----------|----------------------------------------|
| `Servico` | Entidade | Nome, descrição, preço, tempo estimado |

**Invariantes:**

- `preco` deve ser maior que zero
- `tempoEstimadoMinutos` mínimo de 1 minuto
- Soft delete: serviços inativados permanecem para manter histórico das OS

---

### 4. Estoque de Peças e Insumos

**Responsabilidade:** Controle de peças e insumos, com saldo de estoque.

| Elemento | Tipo     | Descrição                                                         |
|----------|----------|-------------------------------------------------------------------|
| `Peca`   | Entidade | Nome, preço unitário, quantidade em estoque, código de referência |

**Invariantes:**

- `quantidadeEstoque` nunca pode ficar negativo
- Débito ocorre apenas na aprovação do orçamento (não no diagnóstico)
- Soft delete: peças inativadas preservam histórico das OS

---

### 5. Identidade e Acesso

**Responsabilidade:** Autenticação e controle de acesso às APIs administrativas.

| Elemento                 | Tipo                                 | Descrição                                       |
|--------------------------|--------------------------------------|-------------------------------------------------|
| `Usuario`                | Entidade                             | username, senha (BCrypt), role                  |
| `JwtService`             | Serviço de Domínio de Infraestrutura | Gera e valida tokens JWT HS256                  |
| `UserDetailsServiceImpl` | Adaptador                            | Integra Spring Security com `UsuarioRepository` |

**Roles:**
| Role | Acesso |
|---|---|
| `ADMIN` | Acesso completo + gestão de usuários |
| `TECNICO` | Acesso às OS e CRUD operacional |
| *(público)* | Somente `GET /api/ordens-servico/{id}/status` |

---

## Mapa de Relacionamentos Entre Contextos

```
IDENTIDADE E ACESSO
        │ (protege)
        ▼
CLIENTES E VEÍCULOS ──────────────────────► ORDENS DE SERVIÇO
                                                    │
                    CATÁLOGO DE SERVIÇOS ───────────┤
                                                    │
                    ESTOQUE DE PEÇAS ───────────────┘
                         ▲
                         │ (débito automático via Política)
                         └── ORDENS DE SERVIÇO (OrcamentoAprovado)
```

**Tipo de integração:** Todos os contextos coexistem no mesmo monolito.  
Comunicação via chamadas diretas de serviço Java (não há mensageria neste MVP).  
Em uma evolução futura para microsserviços, os bounded contexts seriam os candidatos naturais à separação.

---

## Mapeamento para Arquitetura MVC

Cada bounded context é implementado seguindo o padrão MVC em camadas: Controller → Service → Repository.

| Bounded Context      | Controller                               | Service                            | Repository                               |
|----------------------|------------------------------------------|------------------------------------|------------------------------------------|
| Clientes e Veículos  | `ClienteController`, `VeiculoController` | `ClienteService`, `VeiculoService` | `ClienteRepository`, `VeiculoRepository` |
| Ordens de Serviço    | `OrdemServicoController`                 | `OrdemServicoService`              | `OrdemServicoRepository`                 |
| Catálogo de Serviços | `ServicoController`                      | `ServicoService`                   | `ServicoRepository`                      |
| Estoque de Peças     | `PecaController`                         | `PecaService`                      | `PecaRepository`                         |
| Identidade e Acesso  | `AuthController`                         | `AuthService`                      | `UsuarioRepository`, `JwtService`        |

**Princípio aplicado:** Cada camada tem responsabilidade única. Os Controllers recebem requisições HTTP e delegam ao
Service. Os Services contêm toda a lógica de negócio e chamam os Repositories Spring Data JPA diretamente. O Model
encapsula as regras de domínio (transições de status, cálculo de orçamento).

Para mais detalhes sobre a arquitetura MVC, consulte [docs/arquitetura/mvc.md](../arquitetura/mvc.md).
