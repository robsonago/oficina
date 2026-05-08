# Arquitetura MVC em Camadas (Model-View-Controller)

## O que é

A arquitetura MVC organiza a aplicação em três camadas bem definidas com responsabilidades distintas. No contexto de uma
API REST com Spring Boot, o "View" é substituído pela camada de Controller que expõe endpoints HTTP, enquanto Model e
Controller se comunicam por meio da camada de Service.

**Regra fundamental:** cada camada conhece apenas a camada imediatamente abaixo dela. O Controller chama o Service; o
Service chama o Repository; o Repository acessa o banco.

---

## Diagrama das Camadas

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CONTROLLER (C)                              │
│  ClienteController  VeiculoController  OrdemServicoController  ...  │
│         │                   │                    │                   │
│         ▼                   ▼                    ▼                   │
├─────────────────────────────────────────────────────────────────────┤
│                           SERVICE (lógica de negócio)                │
│  ClienteService  VeiculoService  OrdemServicoService  AuthService   │
│         │                   │                    │                   │
│         ▼                   ▼                    ▼                   │
├─────────────────────────────────────────────────────────────────────┤
│                        REPOSITORY (acesso a dados)                   │
│  ClienteRepository  VeiculoRepository  OrdemServicoRepository  ...  │
│         │                   │                    │                   │
│         ▼                   ▼                    ▼                   │
├─────────────────────────────────────────────────────────────────────┤
│                        MODEL (entidades JPA)                         │
│         Cliente  Veiculo  OrdemServico  Peca  Servico  Usuario      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Estrutura de Pacotes

```
src/main/java/br/com/fiap/challange/oficina/
├── config/            # Configurações (Security, OpenAPI, DataInitializer)
├── controller/        # Controllers REST — recebem e respondem requisições HTTP
│   ├── AuthController.java
│   ├── ClienteController.java
│   ├── VeiculoController.java
│   ├── PecaController.java
│   ├── ServicoController.java
│   ├── OrdemServicoController.java
│   └── GlobalExceptionHandler.java
├── service/           # Lógica de negócio — regras, validações, orquestração
│   ├── AuthService.java
│   ├── ClienteService.java
│   ├── VeiculoService.java
│   ├── PecaService.java
│   ├── ServicoService.java
│   └── OrdemServicoService.java
├── repository/        # Repositórios Spring Data JPA — acesso ao banco
│   ├── ClienteRepository.java
│   ├── VeiculoRepository.java
│   ├── PecaRepository.java
│   ├── ServicoRepository.java
│   ├── OrdemServicoRepository.java
│   ├── UsuarioRepository.java
│   ├── ItemServicoOSRepository.java
│   └── ItemPecaOSRepository.java
├── model/             # Entidades JPA e agregados de domínio
│   ├── enums/         # StatusOS, TipoDocumento
│   ├── Cliente.java
│   ├── Veiculo.java
│   ├── OrdemServico.java
│   ├── ItemServicoOS.java
│   ├── ItemPecaOS.java
│   ├── Peca.java
│   ├── Servico.java
│   └── Usuario.java
├── dto/               # Objetos de transferência de dados
│   ├── request/       # Payloads de entrada (ClienteRequest, OrdemServicoRequest...)
│   └── response/      # Payloads de saída (ClienteResponse, OrdemServicoResponse...)
├── exception/         # Exceções de negócio (RecursoNaoEncontradoException...)
├── security/          # JWT (JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl)
├── filter/            # Filtros HTTP (CorrelationIdFilter)
└── validator/         # Validadores de domínio (CpfCnpjValidator, PlacaValidator)
```

---

## Responsabilidades por Camada

### Controller

- Recebe requisições HTTP e devolve respostas
- Converte DTOs de entrada e saída
- Delega toda a lógica para o Service
- Não contém regras de negócio

### Service

- Contém toda a lógica de negócio da aplicação
- Valida entradas, aplica regras de domínio, orquestra operações
- Chama os Repositories para persistência
- Gerencia transações (`@Transactional`)

### Repository

- Interface Spring Data JPA — acesso direto ao banco de dados
- Sem lógica de negócio; apenas consultas e persistência
- Métodos derivados por convenção de nome + queries `@Query` quando necessário

### Model

- Entidades JPA mapeadas para tabelas do banco
- Contém regras de negócio encapsuladas no modelo (
  ex: `OrdemServico.transicionarStatus()`, `StatusOS.podeTransicionarPara()`)
- Validadores de domínio (`CpfCnpjValidator`, `PlacaValidator`) permanecem no pacote `validator/`

---

## Fluxo de Dependências

```
Controller → Service → Repository → Model
```

- O Controller injeta o Service concreto (`@RequiredArgsConstructor`)
- O Service injeta os Repositories Spring Data diretamente
- O Model não depende de nenhuma outra camada

---

## Tabela de Componentes por Camada

### Controllers → Services

| Controller               | Service injetado      |
|--------------------------|-----------------------|
| `ClienteController`      | `ClienteService`      |
| `VeiculoController`      | `VeiculoService`      |
| `PecaController`         | `PecaService`         |
| `ServicoController`      | `ServicoService`      |
| `OrdemServicoController` | `OrdemServicoService` |
| `AuthController`         | `AuthService`         |

### Services → Repositories

| Service               | Repositories utilizados                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------------------|
| `ClienteService`      | `ClienteRepository`                                                                                       |
| `VeiculoService`      | `VeiculoRepository`, `ClienteRepository`                                                                  |
| `PecaService`         | `PecaRepository`                                                                                          |
| `ServicoService`      | `ServicoRepository`                                                                                       |
| `OrdemServicoService` | `OrdemServicoRepository`, `ClienteRepository`, `VeiculoRepository`, `ServicoRepository`, `PecaRepository` |
| `AuthService`         | `UsuarioRepository`, `JwtService`                                                                         |

---

## Benefícios Aplicados neste Projeto

1. **Simplicidade:** Estrutura direta e familiar para qualquer desenvolvedor Spring Boot — sem camadas de abstração
   adicionais (ports, adapters).
2. **Manutenibilidade:** Cada camada tem uma responsabilidade clara; fácil localizar onde cada tipo de lógica reside.
3. **Testabilidade:** Services são testados com mocks dos Repositories Spring Data (interfaces); Controllers são
   testados com `@SpringBootTest` ou `@WebMvcTest`.
4. **Produtividade:** Menos arquivos, menos indireção — o fluxo de uma operação é linear e rastreável.
