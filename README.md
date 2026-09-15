# Oficina Mecânica — Sistema Integrado de Atendimento

Back-end do sistema de gestão de uma oficina mecânica, desenvolvido para o Tech Challenge da
Pós-Tech SOAT (FIAP). Este repositório cobre as três fases do projeto:

- **Fase 1** — MVP: gestão de ordens de serviço, clientes, veículos e peças, com API REST em
  arquitetura MVC.
- **Fase 2** — evolução da aplicação para **Arquitetura Hexagonal**, com containerização,
  orquestração via **Kubernetes**, provisionamento como código via **Terraform** e pipeline de
  **CI/CD**, preparando o sistema para escalar em picos de demanda.
- **Fase 3** (este documento) — a aplicação passa a rodar em **nuvem real (GCP)**, atrás de um
  **API Gateway**, com **autenticação de clientes por CPF via function serverless**, notificações
  assíncronas via **Pub/Sub**, **observabilidade real** (New Relic: APM, dashboards, alertas,
  uptime) e infraestrutura dividida em **4 repositórios** com CI/CD próprio cada um — ver
  [seção 3.4](#34-os-4-repositórios-do-projeto).

---

## Índice

1. [Objetivos desta fase](#1-objetivos-desta-fase)
   - [Como testar agora (resumo)](#como-testar-agora-resumo)
2. [Documentação completa do projeto](#2-documentação-completa-do-projeto)
3. [Arquitetura](#3-arquitetura)
   - [3.4 Os 4 repositórios do projeto](#34-os-4-repositórios-do-projeto)
4. [Modelagem de domínio (DDD)](#4-modelagem-de-domínio-ddd)
5. [Tecnologias](#5-tecnologias)
6. [Pré-requisitos](#6-pré-requisitos)
7. [Como executar](#7-como-executar)
   - [7.2 Executando a aplicação já publicada na GCP](#72-executando-a-aplicação-já-publicada-na-gcp)
8. [CI/CD](#8-cicd)
9. [Documentação da API](#9-documentação-da-api)
10. [Autenticação](#10-autenticação)
11. [Endpoints principais](#11-endpoints-principais)
12. [Testes](#12-testes)
13. [Segurança](#13-segurança)

---

## 1. Objetivos desta fase

A Fase 2 deixou a aplicação containerizada, orquestrada via Kubernetes e com CI/CD — mas ainda rodando
num cluster local (`kind`). A Fase 3 eleva o projeto a um nível corporativo, saindo do ambiente local
para uma **nuvem real (GCP)**, com a arquitetura agora distribuída em componentes independentes:

- **API Gateway** (Google API Gateway) como porta de entrada pública única, na frente da aplicação.
- **Autenticação de clientes por CPF via function serverless** (`oficina-auth-function`): o cliente da
  oficina (diferente do funcionário, que usa usuário/senha) se autentica só com o CPF, sem senha — a
  function valida o CPF, consulta o cliente no banco e emite um JWT compatível com o da aplicação
  principal. Ver [RFC-003](docs/rfcs/003-estrategia-autenticacao.md).
- **Notificações assíncronas via Pub/Sub**: a aplicação publica um evento em vez de mandar e-mail
  diretamente; uma segunda function (`oficina-notification-function`) consome a fila e envia o e-mail.
  Ver [ADR-001](docs/adrs/001-padrao-comunicacao-notificacoes.md).
- **Banco de Dados Gerenciado** (Cloud SQL/PostgreSQL) no lugar do Postgres em container. Ver
  [RFC-002](docs/rfcs/002-escolha-banco-de-dados.md).
- **Observabilidade real** via New Relic: agente APM (latência, erros de transação), métricas de
  CPU/memória do cluster, métricas de negócio customizadas, logs estruturados correlacionados,
  dashboards, alertas e checagem de uptime — ver [seção 3.3](#33-observabilidade).
- **4 repositórios independentes**, cada um com seu próprio pipeline de CI/CD e deploy automático para
  homologação e produção — ver [seção 3.4](#34-os-4-repositórios-do-projeto).
- **Documentação arquitetural formal**: diagrama de componentes, diagramas de sequência, RFCs e ADRs —
  ver [seção 2](#2-documentação-completa-do-projeto).

As decisões da Fase 2 que continuam válidas (Arquitetura Hexagonal, containerização, HPA) estão
documentadas nas seções abaixo e em [`docs/arquitetura/`](docs/arquitetura/).

### Como testar agora (resumo)

A aplicação **já está publicada e rodando na nuvem** — dá pra testar sem instalar nada, só chamando
os endpoints abaixo. Passo a passo completo na [seção 7.2](#72-executando-a-aplicação-já-publicada-na-gcp).

```bash
# Login como funcionário (usuário administrador padrão)
TOKEN=$(curl -s -X POST https://oficina-gateway-producao-b0ob3sbi.ue.gateway.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")

# Chamada a um endpoint protegido, usando o token
curl -s https://oficina-gateway-producao-b0ob3sbi.ue.gateway.dev/api/clientes \
  -H "Authorization: Bearer $TOKEN"
```

Outras formas de testar, todas contra o mesmo ambiente:

| Como | Onde | Detalhe |
|---|---|---|
| Swagger UI (interface visual) | `https://136.68.248.181.nip.io/swagger-ui.html` | [seção 7.2](#72-executando-a-aplicação-já-publicada-na-gcp) |
| Postman (collection pronta, fluxo completo de uma OS) | [`collection/oficina-api.postman_collection.json`](collection/oficina-api.postman_collection.json) | [seção 9](#9-documentação-da-api) |
| Autenticação de **cliente** por CPF (sem senha, via Cloud Function) | `https://oficina-auth-producao-rqwoyvcqha-rj.a.run.app` | [seção 9.2](#92-autenticação-de-clientes-por-cpf-fase-3) |
| Observabilidade (dashboards, alertas, uptime) | New Relic, conta `8508624` | [seção 3.3](#33-observabilidade) |

---

## 2. Documentação completa do projeto

Este README cobre a visão geral e as instruções de execução. Os documentos abaixo detalham cada
decisão técnica:

| Documento | Conteúdo |
|---|---|
| [`docs/arquitetura/hexagonal.md`](docs/arquitetura/hexagonal.md) | Arquitetura Hexagonal: camadas, fluxo de uma requisição, tabela de ports/adapters por bounded context |
| [`docs/arquitetura/infraestrutura.md`](docs/arquitetura/infraestrutura.md) | Docker, Kubernetes, Terraform e CI/CD — decisões técnicas e como executar cada parte |
| [`docs/arquitetura/diagrama-componentes.md`](docs/arquitetura/diagrama-componentes.md) | Diagrama de componentes da Fase 3: gateway, app no GKE, functions, Cloud SQL, New Relic e como se conectam |
| [`docs/arquitetura/diagrama-sequencia.md`](docs/arquitetura/diagrama-sequencia.md) | Diagramas de sequência: autenticação por CPF e abertura de uma ordem de serviço |
| [`docs/rfcs/`](docs/rfcs/) | RFCs — propostas técnicas discutidas (nuvem, banco de dados, autenticação) |
| [`docs/adrs/`](docs/adrs/) | ADRs — decisões de arquitetura já fechadas (comunicação assíncrona, HPA) |
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

### 3.3 Observabilidade

Ferramenta: **New Relic** (ver [ADR](docs/adrs/) e justificativa de escolha). Três frentes:

- **Agente APM** (Java, embutido no `Dockerfile`) — latência e erros de cada transação HTTP, sem
  código extra na aplicação.
- **Métricas de infraestrutura e negócio** — `nri-prometheus` (instalado no cluster via Helm, repo
  `oficina-infra-k8s`) coleta CPU/memória dos pods e faz scrape do endpoint
  `/actuator/prometheus`, que expõe métricas de negócio customizadas via Micrometer
  (`NegocioMetrics`): volume diário de OS abertas, tempo médio de execução por status, e contagem de
  erros de integração (e-mail/Pub-Sub).
- **Alertas e uptime** — uma condição NRQL dispara quando qualquer chamada em
  `/api/ordens-servico` retorna erro (`TransactionError`), notificando por e-mail; dois Synthetic
  Monitors batem em `/actuator/health` de cada ambiente a cada 5 minutos.

Três dashboards (Volume de OS, Tempo Médio de Execução por Status, Erros e Falhas de Integração) —
configurados via API (NerdGraph) no repositório `oficina-infra-k8s`.

### 3.4 Os 4 repositórios do projeto

O projeto é dividido em 4 repositórios independentes, cada um com seu próprio pipeline de CI/CD e
deploy automático (homologação a partir da branch `homolog`, produção a partir da `main`):

| Repositório | Responsabilidade | Tecnologia principal |
|---|---|---|
| [`oficina`](https://github.com/robsonago/oficina) (este) | Aplicação principal (API REST) | Spring Boot / Java 21 |
| [`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s) | Cluster GKE, namespaces, API Gateway, Pub/Sub, coletor New Relic | Terraform |
| [`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db) | Banco de Dados Gerenciado (Cloud SQL/PostgreSQL) | Terraform |
| [`oficina-auth-function`](https://github.com/robsonago/oficina-auth-function) | Autenticação de clientes por CPF e disparo de notificações | Cloud Functions (Java 21) |

### 3.5 Fluxo de deploy (CI/CD)

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

### 7.2 Executando a aplicação já publicada na GCP

A aplicação **já está publicada e rodando na nuvem** — não é preciso provisionar nada pra usá-la, só
chamar os endpoints. O cluster GKE, banco Cloud SQL e API Gateway são provisionados pelos
repositórios dedicados de infraestrutura ([`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s)
e [`oficina-infra-db`](https://github.com/robsonago/oficina-infra-db) — consulte-os só se for
recriar/alterar a infra do zero).

**Ambiente ativo (produção):**

| O quê | URL |
|---|---|
| API (rotas de negócio, via Gateway) | `https://oficina-gateway-producao-b0ob3sbi.ue.gateway.dev` |
| Swagger UI / `/api-docs` (via Ingress — não passa pelo Gateway) | `https://136.68.248.181.nip.io/swagger-ui.html` |

> Os hosts acima mudam a cada recriação da infraestrutura (IP novo a cada `terraform apply` do
> `oficina-infra-k8s`) — se algum link estiver fora do ar, confira o IP/URL atual no README de
> [`oficina-infra-k8s`](https://github.com/robsonago/oficina-infra-k8s#9-ambiente-ativo).

**Teste rápido (login + chamada autenticada), contra o ambiente real:**

```bash
# 1. Login — usuário administrador padrão
TOKEN=$(curl -s -X POST https://oficina-gateway-producao-b0ob3sbi.ue.gateway.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['token'])")

# 2. Chamada a um endpoint protegido, usando o token
curl -s https://oficina-gateway-producao-b0ob3sbi.ue.gateway.dev/api/clientes \
  -H "Authorization: Bearer $TOKEN"
```

Fluxo completo (criar cliente/veículo, abrir e conduzir uma OS até a entrega) na
[seção 9.1](#91-exemplo-de-uso-ponta-a-ponta) — os mesmos exemplos funcionam trocando
`http://localhost:8080` pela URL do Gateway acima. Um roteiro de teste mais completo (Swagger,
Postman, autenticação por CPF, observabilidade) está em `atividades/Fase3/guia-apresentacao-professor.md`
(anotação local, fora deste repositório).

---

## 8. CI/CD

O workflow [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) roda automaticamente a cada
`git push` (qualquer branch) e em Pull Requests para `main` — ver [seção 3.5](#35-fluxo-de-deploy-cicd)
para o detalhamento dos jobs.

---

## 9. Documentação da API

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (local) · em nuvem, direto pelo host do
  Ingress (não pelo Gateway) — ver [seção 7.2](#72-executando-a-aplicação-já-publicada-na-gcp)
- **Collection Postman completa**: [`collection/oficina-api.postman_collection.json`](collection/oficina-api.postman_collection.json)
- Tabela completa de endpoints: [seção 11](#11-endpoints-principais)

### 9.1 Exemplo de uso ponta a ponta

Fluxo completo — autenticar, cadastrar cliente e veículo, abrir uma OS e consultar o status — com
requisição e resposta de cada chamada (local, `http://localhost:8080`):

**1. Login (funcionário) e captura do token:**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)
```

**2. Cadastrar um cliente:**

```bash
curl -s -X POST http://localhost:8080/api/clientes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"João Silva","documento":"52998224725","email":"joao@example.com","telefone":"11999999999"}'
```

```json
{"id": 1, "nome": "João Silva", "documento": "529.982.247-25", "email": "joao@example.com", ...}
```

**3. Cadastrar um veículo para esse cliente:**

```bash
curl -s -X POST http://localhost:8080/api/veiculos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"placa":"ABC1D23","modelo":"Onix","marca":"Chevrolet","ano":2022,"clienteId":1}'
```

**4. Abrir uma ordem de serviço** (identifica cliente e veículo por documento/placa, não por ID):

```bash
curl -s -X POST http://localhost:8080/api/ordens-servico \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"documentoCliente":"52998224725","placaVeiculo":"ABC1D23","descricaoProblema":"Barulho no motor"}'
```

```json
{"id": 1, "numero": "OS-2026-A1B2C3D4", "status": "RECEBIDA", "statusDescricao": "Recebida", "valorTotal": 0, ...}
```

**5. Consultar status da OS — endpoint público, sem token (é o que o cliente usa pra acompanhar):**

```bash
curl -s http://localhost:8080/api/ordens-servico/1/status
```

```json
{"status": "RECEBIDA", "ordemServicoId": "1"}
```

A partir daí, o fluxo segue pelos endpoints de [seção 11](#11-endpoints-principais): iniciar
diagnóstico → gerar orçamento (dispara notificação) → aprovar/rejeitar → finalizar → entregar.

### 9.2 Autenticação de clientes por CPF (Fase 3)

Diferente do funcionário (usuário/senha, endpoint acima), o cliente final se autentica só com o CPF,
através da function serverless `oficina-auth-function` (fora deste repositório, ver
[RFC-003](docs/rfcs/003-estrategia-autenticacao.md)):

```bash
curl -s -X POST https://<url-da-cloud-function-oficina-auth> \
  -H "Content-Type: application/json" \
  -d '{"cpf":"52998224725"}'
```

A function valida o CPF, consulta o cliente no banco e devolve um JWT com a claim `role=CLIENTE`,
aceito pelos mesmos endpoints protegidos desta aplicação (`JwtAuthenticationFilter` reconhece esse
token sem passar pelo `UserDetailsService` de funcionários).

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

### 12.1 Organização

`src/test/java/.../oficina/` é dividido por tipo, refletindo a Arquitetura Hexagonal:

| Pacote | O que testa | Estilo |
|---|---|---|
| `domain/` | Modelos e regras de domínio puras (`OrdemServico`, `StatusOS`, validadores) | Unitário, sem Spring |
| `service/` | Casos de uso (`*UseCase`) | Unitário, com Mockito mockando os *ports* de saída |
| `controller/` | Tratamento de erro dos controllers (`GlobalExceptionHandler`) | Unitário, sem Spring |
| `security/` | `JwtService`, `JwtAuthenticationFilter` | Unitário |
| `filter/` | `CorrelationIdFilter` | Unitário |
| `integration/` (`*IT.java`) | Fluxo HTTP completo de cada controller, ponta a ponta | `@SpringBootTest` + `MockMvc`, banco H2 em memória via `@ActiveProfiles("test")` |

Convenção de nomes: `*Test.java` para unitários, `*IT.java` para integração — ambos rodam juntos no
mesmo `./mvnw test` (configurado no `maven-surefire-plugin` do `pom.xml`), não é preciso um comando
separado para cada tipo.

### 12.2 Como executar

```bash
# Todos os testes (unitários + integração)
./mvnw test

# Só uma classe ou um pacote
./mvnw test -Dtest=OrdemServicoUseCaseTest
./mvnw test -Dtest='integration.*'

# Relatório de cobertura (gerado em target/site/jacoco/index.html)
./mvnw test jacoco:report
```

Os testes de integração usam H2 em memória (perfil `test`), não dependem de um Postgres real nem de
credenciais — rodam isolados, inclusive no pipeline de CI/CD.

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
