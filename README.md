# Oficina Mecânica — Sistema Integrado de Atendimento

Back-end do sistema de gestão de uma oficina mecânica, desenvolvido para o Tech Challenge da
Pós-Tech SOAT (FIAP). Este repositório cobre as duas fases do projeto:

- **Fase 1** — MVP: gestão de ordens de serviço, clientes, veículos e peças, com API REST em
  arquitetura MVC.
- **Fase 2** (este documento) — evolução da aplicação para **Arquitetura Hexagonal**, com
  containerização, orquestração via **Kubernetes**, provisionamento como código via **Terraform** e
  pipeline de **CI/CD** completo, preparando o sistema para escalar em picos de demanda.

---

## Índice

1. [Objetivos desta fase](#1-objetivos-desta-fase)
2. [Documentação completa do projeto](#2-documentação-completa-do-projeto)
3. [Arquitetura](#3-arquitetura)
4. [Modelagem de domínio (DDD)](#4-modelagem-de-domínio-ddd)
5. [Tecnologias](#5-tecnologias)
6. [Pré-requisitos](#6-pré-requisitos)
7. [Como executar](#7-como-executar)
8. [CI/CD](#8-cicd)
9. [Documentação da API](#9-documentação-da-api)
10. [Autenticação](#10-autenticação)
11. [Endpoints principais](#11-endpoints-principais)
12. [Testes](#12-testes)
13. [Segurança](#13-segurança)

---

## 1. Objetivos desta fase

Após a implantação do MVP (Fase 1), a oficina precisava reduzir riscos operacionais, automatizar o
provisionamento e o deploy do ambiente, melhorar a organização do código e se preparar para suportar
grandes volumes de ordens de serviço em horários de pico. A Fase 2 endereça isso com:

- **Refatoração para Arquitetura Hexagonal** (Ports & Adapters), isolando o domínio de frameworks e
  detalhes técnicos, com testes automatizados cobrindo os fluxos críticos.
- **Containerização** completa via Docker (multi-stage build) e `docker-compose` para desenvolvimento
  local.
- **Orquestração via Kubernetes**: Deployments, Services, ConfigMaps/Secrets e um
  `HorizontalPodAutoscaler` que escala a aplicação automaticamente por consumo de CPU.
- **Infraestrutura como Código via Terraform**: um único `terraform apply` provisiona cluster, banco,
  serviço de e-mail e aplicação — usado tanto localmente quanto pelo próprio pipeline de CI/CD.
- **Pipeline de CI/CD** (GitHub Actions): compila, testa, publica a imagem Docker e faz o deploy
  completo via Terraform a cada `git push`.
- **Notificação por e-mail** e endpoint unificado de aprovação/rejeição de orçamento.

---

## 2. Documentação completa do projeto

Este README cobre a visão geral e as instruções de execução. Os documentos abaixo detalham cada
decisão técnica:

| Documento | Conteúdo |
|---|---|
| [`docs/arquitetura/hexagonal.md`](docs/arquitetura/hexagonal.md) | Arquitetura Hexagonal: camadas, fluxo de uma requisição, tabela de ports/adapters por bounded context |
| [`docs/arquitetura/infraestrutura.md`](docs/arquitetura/infraestrutura.md) | Docker, Kubernetes, Terraform e CI/CD — decisões técnicas e como executar cada parte |
| [`docs/ddd/contextos-delimitados.md`](docs/ddd/contextos-delimitados.md) | Bounded contexts do domínio |
| [`docs/ddd/agregados-e-entidades.md`](docs/ddd/agregados-e-entidades.md) | Agregados, entidades e objetos de valor |
| [`docs/ddd/event-storming.md`](docs/ddd/event-storming.md) | Eventos de domínio, comandos, atores e políticas |
| [`docs/ddd/domain-storytelling.md`](docs/ddd/domain-storytelling.md) | Narrativa pictográfica dos fluxos do domínio |
| [`docs/ddd/linguagem-ubiqua.md`](docs/ddd/linguagem-ubiqua.md) | Glossário completo da linguagem ubíqua |
| [`docs/vulnerabilidades/relatorio-vulnerabilidades.md`](docs/vulnerabilidades/relatorio-vulnerabilidades.md) | Relatório de análise de vulnerabilidades |

---

## 3. Arquitetura

### 3.1 Componentes da aplicação — Arquitetura Hexagonal (Ports & Adapters)

O domínio ocupa o centro da aplicação e não depende de nenhuma outra camada; toda comunicação com o
mundo externo (HTTP, banco de dados, e-mail) passa por interfaces (**ports**) implementadas por
classes de infraestrutura (**adapters**). Regra de dependência: **Infrastructure → Application →
Domain**, nunca o contrário.

![Diagrama das Camadas Hexagonais](docs/arquitetura/images/hexagonal-camadas.png)

```
src/main/java/br/com/fiap/challange/oficina/
├── domain/
│   ├── model/         # POJOs puros (sem JPA/Hibernate), Value Objects (AuthToken, Estatisticas), enums
│   ├── port/
│   │   ├── in/        # Input Ports (contratos dos casos de uso) + commands/
│   │   └── out/        # Output Ports (ClienteRepositoryPort, EmailPort, TokenPort, PasswordEncoderPort, AuthenticationPort, ...)
│   ├── exception/      # Exceções de domínio
│   └── validator/      # CpfCnpjValidator, PlacaValidator
├── application/
│   └── usecase/        # Implementações dos casos de uso (AuthUseCase, OrdemServicoUseCase, ...)
├── infrastructure/
│   ├── adapter/
│   │   ├── in/rest/     # Controllers REST
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── entity/    # Entidades JPA (*JpaEntity), isoladas do domínio
│   │       │   ├── mapper/    # MapStruct: conversão domínio ↔ entidade JPA
│   │       │   └── (repositórios Spring Data + *RepositoryAdapter, package-private)
│   │       └── email/        # JavaMailSenderEmailAdapter / NoOpEmailAdapter
│   ├── config/          # SecurityConfig, OpenApiConfig, DataInitializer
│   ├── filter/          # CorrelationIdFilter
│   └── security/        # JwtService, JwtAuthenticationFilter, PasswordEncoderAdapter, AuthenticationAdapter
└── dto/                 # DTOs de entrada/saída
    ├── request/
    └── response/
```

Detalhes completos (fluxo de uma requisição ponta a ponta, tabela de ports/adapters por bounded
context, benefícios aplicados) em [`docs/arquitetura/hexagonal.md`](docs/arquitetura/hexagonal.md).

### 3.2 Infraestrutura provisionada

![Diagrama de Visão Geral da Infraestrutura](docs/arquitetura/images/infraestrutura-visao-geral.png)

- **Docker**: `Dockerfile` multi-stage (build com JDK, runtime com JRE, ~180MB) e `docker-compose.yml`
  para desenvolvimento local (app + Postgres + Mailpit).
- **Kubernetes**: cluster GKE, namespaces de homologação/produção e manifestos de
  `Deployment`/`Service`/`HorizontalPodAutoscaler` (min 2 / max 5 réplicas, 70% CPU) no repositório
  [`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s).
- **Terraform**: infraestrutura de cluster/rede/gateway em `oficina-infra-k8s` e do banco gerenciado
  (Cloud SQL) em [`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db) — cada
  repositório provisiona sua parte com um único `terraform apply`.

> As pastas `infra/` e `k8s/` que existiam neste repositório (setup local via `kind`) foram removidas;
> o histórico desse setup fica registrado em
> [`docs/arquitetura/infraestrutura.md`](docs/arquitetura/infraestrutura.md).

### 3.3 Fluxo de deploy (CI/CD)

![Diagrama do Pipeline de CI/CD](docs/arquitetura/images/infraestrutura-pipeline-cicd.png)

A cada `git push` (qualquer branch) ou Pull Request para `main`, o workflow
[`ci-cd.yml`](.github/workflows/ci-cd.yml) executa:

1. **Build e Testes** — `./mvnw verify` (compila e roda os testes automatizados).
2. **Build e Push da Imagem Docker** — publica em `ghcr.io/<owner>/oficina-app` (só em push).

O deploy em nuvem (GKE) é feito pelo pipeline de
[`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s), a partir da imagem publicada
aqui.

---

## 4. Modelagem de domínio (DDD)

### Fluxo das Ordens de Serviço

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                     ↓ (rejeição)
                                EM_DIAGNOSTICO
```

### Linguagem Ubíqua (resumo)

| Termo                     | Descrição                                                                   |
|---------------------------|-----------------------------------------------------------------------------|
| **Ordem de Serviço (OS)** | Agregado raiz que representa um serviço completo desde abertura até entrega |
| **Cliente**               | Pessoa física (CPF) ou jurídica (CNPJ) proprietária do veículo              |
| **Veículo**               | Automóvel cadastrado pertencente a um cliente                               |
| **Serviço**               | Tipo de trabalho realizado (ex: troca de óleo, alinhamento)                 |
| **Peça/Insumo**           | Material consumível com controle de estoque                                 |
| **Orçamento**             | Valor calculado automaticamente da soma de serviços + peças da OS           |
| **Diagnóstico**           | Fase de avaliação técnica do veículo                                        |

Glossário completo, bounded contexts, event storming e domain storytelling em
[`docs/ddd/`](docs/ddd/) (ver [seção 2](#2-documentação-completa-do-projeto)).

---

## 5. Tecnologias

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
| Mailpit           | latest | Servidor SMTP fake para testar notificações por e-mail sem envio real          |
| Docker            | —      | Empacotamento da aplicação (multi-stage build)                                |
| Kubernetes (GKE)  | —      | Orquestração dos containers (Deployments, Services, HPA) — `oficina-infra-k8s` |
| Terraform         | ≥1.6   | Infraestrutura como Código — cluster/gateway em `oficina-infra-k8s`, banco em `oficina-infra-db` |
| GitHub Actions    | —      | Pipeline de CI/CD — build, testes e imagem Docker                             |

**Por que PostgreSQL?** ACID compliance garante consistência nas transações de ordens de serviço;
suporte nativo a tipos avançados; excelente desempenho em queries com JOINs; maturidade e suporte da
comunidade.

---

## 6. Pré-requisitos

| Ferramenta | Necessária para |
|---|---|
| Java 21+ e Maven 3.8+ | Rodar/compilar a aplicação sem Docker |
| Docker e Docker Compose | Execução local e build da imagem |

Para provisionar/alterar a infraestrutura em nuvem, ver os pré-requisitos de
[`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s) e
[`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db).

---

## 7. Como executar

### 7.1 Execução local (docker-compose)

```bash
# Copie o arquivo de exemplo e ajuste se necessário
cp .env.oficina.example .env.oficina

docker-compose --env-file .env.oficina up -d --build
```

- API: http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html · Mailpit:
  http://localhost:8025

**Sem Docker:** suba o Postgres separadamente e rode `./mvnw spring-boot:run`.

### 7.2 Deploy em nuvem (GKE)

O deploy em nuvem (cluster GKE, banco Cloud SQL, API Gateway) é provisionado pelos repositórios
dedicados de infraestrutura, não a partir deste repositório:

- [`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s) — cluster, namespaces,
  manifests da aplicação e API Gateway.
- [`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db) — Cloud SQL (Postgres
  gerenciado).

Consulte o README de cada um para instruções de execução.

---

## 8. CI/CD

O workflow [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) roda automaticamente a cada
`git push` (qualquer branch) e em Pull Requests para `main` — ver [seção 3.3](#33-fluxo-de-deploy-cicd)
para o detalhamento dos jobs.

---

## 9. Documentação da API

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (local) — link do ambiente em nuvem no
  README de [`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s)
- **Collection Postman completa**: [`collection/oficina-api.postman_collection.json`](collection/oficina-api.postman_collection.json)

---

## 10. Autenticação

Um usuário administrador é criado automaticamente na primeira execução:

```
Username: admin
Password: admin123
```

1. Login para obter o token JWT:

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

**Nota:** o endpoint `GET /api/ordens-servico/{id}/status` é público — o cliente pode consultar sem
autenticação.

---

## 11. Endpoints principais

### Autenticação

| Método | Endpoint              | Descrição             |
|--------|-----------------------|-----------------------|
| POST   | `/api/auth/login`     | Obter token JWT       |
| POST   | `/api/auth/registrar` | Criar usuário (ADMIN) |

### Clientes

| Método | Endpoint                             | Descrição           |
|--------|---------------------------------------|---------------------|
| POST   | `/api/clientes`                      | Criar cliente       |
| GET    | `/api/clientes`                      | Listar clientes     |
| GET    | `/api/clientes/{id}`                 | Buscar por ID       |
| GET    | `/api/clientes/documento/{cpf-cnpj}` | Buscar por CPF/CNPJ |
| PUT    | `/api/clientes/{id}`                 | Atualizar cliente   |
| DELETE | `/api/clientes/{id}`                 | Desativar cliente   |

### Veículos

| Método | Endpoint                      | Descrição              |
|--------|-------------------------------|-------------------------|
| POST   | `/api/veiculos`               | Cadastrar veículo      |
| GET    | `/api/veiculos`               | Listar veículos        |
| GET    | `/api/veiculos/placa/{placa}` | Buscar por placa       |
| GET    | `/api/veiculos/cliente/{id}`  | Veículos de um cliente |
| PUT    | `/api/veiculos/{id}`          | Atualizar veículo      |
| DELETE | `/api/veiculos/{id}`          | Desativar veículo      |

### Ordens de Serviço

| Método | Endpoint                                       | Descrição                                                                | Auth    |
|--------|-------------------------------------------------|---------------------------------------------------------------------------|---------|
| POST   | `/api/ordens-servico`                          | Abrir OS (retorna identificação única)                                   | Sim     |
| GET    | `/api/ordens-servico`                          | Listar OS ativas por prioridade (EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA), mais antigas primeiro, excluindo FINALIZADA/ENTREGUE | Sim     |
| GET    | `/api/ordens-servico/ativas`                   | Mesma listagem acima (alias explícito)                                   | Sim     |
| GET    | `/api/ordens-servico/{id}`                     | Detalhes da OS                                                           | Sim     |
| GET    | `/api/ordens-servico/{id}/status`              | Consultar status da OS                                                   | **Não** |
| GET    | `/api/ordens-servico/status/{status}`          | Filtrar por status                                                       | Sim     |
| POST   | `/api/ordens-servico/{id}/iniciar-diagnostico` | Iniciar diagnóstico                                                      | Sim     |
| POST   | `/api/ordens-servico/{id}/gerar-orcamento`     | Gerar orçamento e enviar para aprovação (dispara e-mail)                 | Sim     |
| POST   | `/api/ordens-servico/{id}/aprovacao-orcamento` | Aprovar ou rejeitar orçamento via body (`{"aprovado": true}` ou `false`) | Sim     |
| POST   | `/api/ordens-servico/{id}/finalizar`           | Finalizar execução                                                       | Sim     |
| POST   | `/api/ordens-servico/{id}/entregar`            | Registrar entrega do veículo                                             | Sim     |
| POST   | `/api/ordens-servico/{id}/servicos`            | Adicionar serviço à OS                                                   | Sim     |
| POST   | `/api/ordens-servico/{id}/pecas`               | Adicionar peça à OS                                                      | Sim     |
| GET    | `/api/ordens-servico/estatisticas`             | Tempo médio + contagens                                                  | Sim     |

### Peças e Insumos

| Método | Endpoint                  | Descrição         |
|--------|---------------------------|-------------------|
| POST   | `/api/pecas`              | Criar peça        |
| GET    | `/api/pecas`              | Listar peças      |
| PUT    | `/api/pecas/{id}`         | Atualizar peça    |
| PATCH  | `/api/pecas/{id}/estoque` | Atualizar estoque |
| DELETE | `/api/pecas/{id}`         | Desativar peça    |

---

## 12. Testes

```bash
# Executar todos os testes (unitários + integração)
./mvnw verify

# Relatório de cobertura (gerado em target/site/jacoco/index.html)
./mvnw test jacoco:report
```

Testes de integração (`*IT.java`) usam H2 em memória via `@ActiveProfiles("test")`, sem depender de
um Postgres real.

---

## 13. Segurança

- Autenticação via **JWT (Bearer Token)** — sem estado (stateless)
- **BCrypt** para hash de senhas
- Validação de **CPF/CNPJ** com algoritmo oficial da Receita Federal
- Validação de **placa de veículo** (padrão antigo e Mercosul)
- Soft delete para clientes, veículos, serviços e peças (não remove dados históricos)
- Spring Security filtra todas as rotas administrativas
- Análise de vulnerabilidades documentada em
  [`docs/vulnerabilidades/relatorio-vulnerabilidades.md`](docs/vulnerabilidades/relatorio-vulnerabilidades.md)
- **Nota:** os defaults de `JWT_SECRET`/senha do banco em `application.yaml` são só para
  desenvolvimento local, nunca para produção — em nuvem, esses valores vêm de Secrets do Kubernetes
  populados a partir do Secret Manager (ver
  [`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db)). Segredo de fato (credenciais
  de e-mail) fica fora do Git via `.env.oficina` (`.gitignore`).

**Controle de estoque:** o estoque das peças é debitado automaticamente quando o cliente **aprova o
orçamento** (transição para `EM_EXECUCAO`). Caso o estoque seja insuficiente, a aprovação é bloqueada
com erro `422 Unprocessable Entity`.
