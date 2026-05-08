# Oficina Mecânica - Sistema Integrado de Atendimento

MVP do back-end do sistema de gestão de uma oficina mecânica, desenvolvido como Tech Challenge - Fase 1 do curso de
Pós-Tech SOAT (FIAP).

## Objetivo

Sistema para gestão completa de ordens de serviço, clientes, veículos, peças e insumos, com acompanhamento em tempo real
do status do serviço pelo cliente.

## Tecnologias

| Tecnologia        | Versão | Justificativa                                                                 |
|-------------------|--------|-------------------------------------------------------------------------------|
| Java              | 21     | LTS com records, sealed classes e performance melhorada                       |
| Spring Boot       | 3.4.1  | Framework consolidado para APIs REST em Java                                  |
| PostgreSQL        | 16     | ACID-compliant, excelente suporte a queries relacionais complexas e indexação |
| Flyway            | latest | Controle de versão do schema do banco de dados                                |
| JWT (JJWT)        | 0.12.3 | Autenticação stateless para APIs REST                                         |
| SpringDoc OpenAPI | 2.3.0  | Documentação automática das APIs via Swagger UI                               |
| Lombok            | latest | Redução de boilerplate em entidades e serviços                                |
| JaCoCo            | 0.8.11 | Relatório de cobertura de testes                                              |
| H2                | latest | Banco em memória para testes de integração                                    |

**Por que PostgreSQL?** ACID compliance garante consistência nas transações de ordens de serviço; suporte nativo a tipos
avançados; excelente desempenho em queries com JOINs; maturidade e suporte da comunidade.

## Arquitetura

Monolito seguindo **Arquitetura MVC em Camadas** com princípios de DDD.

O Controller recebe a requisição HTTP e delega ao Service. O Service contém toda a lógica de negócio e chama os
Repositories Spring Data JPA diretamente. O Model encapsula regras de domínio (transições de status, cálculo de
orçamento).

**Fluxo de dependências:**

```
Controller → Service → Repository → Model
```

```
src/main/java/br/com/fiap/challange/oficina/
├── config/        # Configurações (Security, OpenAPI, DataInitializer)
├── controller/    # Controllers REST (recebem requisições, delegam ao Service)
│   ├── AuthController.java
│   ├── ClienteController.java
│   ├── VeiculoController.java
│   ├── PecaController.java
│   ├── ServicoController.java
│   ├── OrdemServicoController.java
│   └── GlobalExceptionHandler.java
├── service/       # Lógica de negócio (regras, validações, orquestração)
│   ├── AuthService.java
│   ├── ClienteService.java
│   ├── VeiculoService.java
│   ├── PecaService.java
│   ├── ServicoService.java
│   └── OrdemServicoService.java
├── repository/    # Repositórios Spring Data JPA (acesso ao banco)
├── model/         # Entidades JPA e regras de domínio
│   └── enums/     # StatusOS, TipoDocumento
├── dto/           # Objetos de transferência de dados
│   ├── request/   # Payloads de entrada
│   └── response/  # Payloads de saída
├── exception/     # Exceções de negócio
├── security/      # JWT (JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl)
├── filter/        # Filtros HTTP
└── validator/     # Validadores de CPF/CNPJ e placa
```

## Fluxo das Ordens de Serviço

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                     ↓ (rejeição)
                                EM_DIAGNOSTICO
```

## Linguagem Ubíqua (DDD)

| Termo                     | Descrição                                                                   |
|---------------------------|-----------------------------------------------------------------------------|
| **Ordem de Serviço (OS)** | Agregado raiz que representa um serviço completo desde abertura até entrega |
| **Cliente**               | Pessoa física (CPF) ou jurídica (CNPJ) proprietária do veículo              |
| **Veículo**               | Automóvel cadastrado pertencente a um cliente                               |
| **Serviço**               | Tipo de trabalho realizado (ex: troca de óleo, alinhamento)                 |
| **Peça/Insumo**           | Material consumível com controle de estoque                                 |
| **Item de Serviço**       | Associação entre uma OS e um serviço, com quantidade e preço                |
| **Item de Peça**          | Associação entre uma OS e uma peça, com quantidade e preço                  |
| **Orçamento**             | Valor calculado automaticamente da soma de serviços + peças da OS           |
| **Diagnóstico**           | Fase de avaliação técnica do veículo                                        |

## Pré-requisitos

- Java 21+
- Maven 3.8+
- Docker e Docker Compose

## Execução local

### Com Docker (recomendado)

```bash
# Subir PostgreSQL + aplicação
docker-compose up --build

# Somente o banco de dados (para desenvolvimento)
docker-compose up postgres
```

### Sem Docker (Maven)

```bash
# Subir PostgreSQL separadamente, depois:
./mvnw spring-boot:run
```

A aplicação inicia na porta `8080`.

## Credenciais padrão

Um usuário administrador é criado automaticamente na primeira execução:

```
Username: admin
Password: admin123
```

## Documentação da API (Swagger UI)

```
http://localhost:8080/swagger-ui.html
```

## Autenticação

1. Realize login para obter o token JWT:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

2. Use o token nas requisições:

```bash
curl http://localhost:8080/api/clientes \
  -H "Authorization: Bearer <seu-token>"
```

**Nota:** O endpoint `GET /api/ordens-servico/{id}/status` é público — o cliente pode consultar sem autenticação.

## Endpoints principais

### Autenticação

| Método | Endpoint              | Descrição             |
|--------|-----------------------|-----------------------|
| POST   | `/api/auth/login`     | Obter token JWT       |
| POST   | `/api/auth/registrar` | Criar usuário (ADMIN) |

### Clientes

| Método | Endpoint                             | Descrição           |
|--------|--------------------------------------|---------------------|
| POST   | `/api/clientes`                      | Criar cliente       |
| GET    | `/api/clientes`                      | Listar clientes     |
| GET    | `/api/clientes/{id}`                 | Buscar por ID       |
| GET    | `/api/clientes/documento/{cpf-cnpj}` | Buscar por CPF/CNPJ |
| PUT    | `/api/clientes/{id}`                 | Atualizar cliente   |
| DELETE | `/api/clientes/{id}`                 | Desativar cliente   |

### Veículos

| Método | Endpoint                      | Descrição              |
|--------|-------------------------------|------------------------|
| POST   | `/api/veiculos`               | Cadastrar veículo      |
| GET    | `/api/veiculos`               | Listar veículos        |
| GET    | `/api/veiculos/placa/{placa}` | Buscar por placa       |
| GET    | `/api/veiculos/cliente/{id}`  | Veículos de um cliente |
| PUT    | `/api/veiculos/{id}`          | Atualizar veículo      |
| DELETE | `/api/veiculos/{id}`          | Desativar veículo      |

### Ordens de Serviço

| Método | Endpoint                                       | Descrição               | Auth    |
|--------|------------------------------------------------|-------------------------|---------|
| POST   | `/api/ordens-servico`                          | Criar OS                | Sim     |
| GET    | `/api/ordens-servico`                          | Listar OS               | Sim     |
| GET    | `/api/ordens-servico/{id}`                     | Detalhes da OS          | Sim     |
| GET    | `/api/ordens-servico/{id}/status`              | Consultar status        | **Não** |
| GET    | `/api/ordens-servico/status/{status}`          | Filtrar por status      | Sim     |
| POST   | `/api/ordens-servico/{id}/iniciar-diagnostico` | Iniciar diagnóstico     | Sim     |
| POST   | `/api/ordens-servico/{id}/gerar-orcamento`     | Gerar orçamento         | Sim     |
| POST   | `/api/ordens-servico/{id}/aprovar`             | Cliente aprova          | Sim     |
| POST   | `/api/ordens-servico/{id}/rejeitar`            | Cliente rejeita         | Sim     |
| POST   | `/api/ordens-servico/{id}/finalizar`           | Finalizar execução      | Sim     |
| POST   | `/api/ordens-servico/{id}/entregar`            | Entregar veículo        | Sim     |
| POST   | `/api/ordens-servico/{id}/servicos`            | Adicionar serviço       | Sim     |
| POST   | `/api/ordens-servico/{id}/pecas`               | Adicionar peça          | Sim     |
| GET    | `/api/ordens-servico/estatisticas`             | Tempo médio + contagens | Sim     |

### Peças e Insumos

| Método | Endpoint                  | Descrição         |
|--------|---------------------------|-------------------|
| POST   | `/api/pecas`              | Criar peça        |
| GET    | `/api/pecas`              | Listar peças      |
| PUT    | `/api/pecas/{id}`         | Atualizar peça    |
| PATCH  | `/api/pecas/{id}/estoque` | Atualizar estoque |
| DELETE | `/api/pecas/{id}`         | Desativar peça    |

## Testes

```bash
# Executar todos os testes
./mvnw test

# Relatório de cobertura (gerado em target/site/jacoco/index.html)
./mvnw test jacoco:report
```

## Segurança

- Autenticação via **JWT (Bearer Token)** — sem estado (stateless)
- **BCrypt** para hash de senhas
- Validação de **CPF/CNPJ** com algoritmo oficial da Receita Federal
- Validação de **placa de veículo** (padrão antigo e Mercosul)
- Soft delete para clientes, veículos, serviços e peças (não remove dados históricos)
- Spring Security filtra todas as rotas administrativas

## Controle de Estoque

O estoque das peças é debitado automaticamente quando o cliente **aprova o orçamento** (transição para `EM_EXECUCAO`).
Caso o estoque seja insuficiente, a aprovação é bloqueada com erro `422 Unprocessable Entity`.
