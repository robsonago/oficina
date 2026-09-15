# RFC-002 — Escolha do Banco de Dados Gerenciado

**Status:** Aceito · **Fase:** 3

## Contexto

A Fase 2 já usava PostgreSQL em container (`docker-compose`/`kind`). A Fase 3 exige um **Banco de
Dados Gerenciado** na nuvem — o enunciado não exige manter o mesmo motor, então cabia reavaliar
diante das opções gerenciadas da GCP (ver [RFC-001](001-escolha-da-nuvem.md)).

## Opções consideradas

| Opção | Prós | Contras para este projeto |
|---|---|---|
| **Cloud SQL (PostgreSQL)** | Mesmo motor da Fase 2 (zero retrabalho de schema/queries); suporte completo a JOINs, transações ACID e índices que o domínio já usa (ex.: consultas de OS por status/prioridade); tier `db-f1-micro` barato o suficiente para uso acadêmico | Menos "nativo à nuvem" que Firestore; escala verticalmente, não horizontalmente |
| **Firestore** (NoSQL documento) | Serverless, escala automática, sem tier fixo | Exigiria reescrever todo o modelo relacional (OS ↔ Cliente ↔ Veículo ↔ Serviços/Peças, com agregação de totais) para documentos; perderia transações multi-entidade e JOINs que o domínio depende (ex.: `findFinalizadasComTempo`, filtros por status com ordenação) |
| **AlloyDB** | Compatível com PostgreSQL, mais performático | Preço mínimo bem acima do `db-f1-micro`; overkill para o volume de dados de um projeto acadêmico |
| **Cloud Spanner** | Escala horizontal global, forte consistência | Custo mínimo alto (nós dedicados), complexidade de modelagem (interleaving) desnecessária pro domínio |

## Decisão

**Cloud SQL para PostgreSQL 16**, mantendo o mesmo motor da Fase 2.

Justificativa central: o domínio (Ordem de Serviço como agregado raiz, com Cliente, Veículo, itens de
Serviço/Peça e transições de status) é fundamentalmente relacional — depende de integridade
referencial e de queries com JOIN e ordenação (ex.: listar OS ativas por prioridade de status). Trocar
para um banco não-relacional neste ponto do projeto significaria reescrever a camada de persistência
inteira sem nenhum ganho funcional correspondente, já que o volume de dados e a carga esperada não
justificam a escala horizontal de um Firestore/Spanner.

## Consequências

- Tier `db-f1-micro` tem limite baixo de conexões simultâneas — o pool do Hikari precisou ser reduzido
  (`SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3`) para não estourar o limite com múltiplas réplicas
  do HPA rodando ao mesmo tempo.
- Um único Cloud SQL hospeda os dois bancos (`oficina_homolog`, `oficina_producao`) como databases
  separados na mesma instância, não duas instâncias — reduz custo, mas significa que os dois ambientes
  competem pelos mesmos recursos de CPU/conexão da instância.
- A function `oficina-auth-function` acessa esse mesmo banco via Cloud SQL Connector embutido (socket
  factory), reaproveitando o mesmo usuário/senha de aplicação lido do Secret Manager — não foi
  necessário criar um segundo usuário de banco só para a function.
