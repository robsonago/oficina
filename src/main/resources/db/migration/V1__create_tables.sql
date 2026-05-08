CREATE TABLE clientes (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(255) NOT NULL,
    documento   VARCHAR(20)  NOT NULL UNIQUE,
    tipo_documento VARCHAR(10) NOT NULL,
    email       VARCHAR(255),
    telefone    VARCHAR(20),
    endereco    VARCHAR(500),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE veiculos (
    id          BIGSERIAL PRIMARY KEY,
    placa       VARCHAR(10)  NOT NULL UNIQUE,
    marca       VARCHAR(100) NOT NULL,
    modelo      VARCHAR(100) NOT NULL,
    ano         INTEGER      NOT NULL,
    cliente_id  BIGINT       NOT NULL REFERENCES clientes (id),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE servicos (
    id                      BIGSERIAL PRIMARY KEY,
    nome                    VARCHAR(255) NOT NULL,
    descricao               TEXT,
    preco                   NUMERIC(10, 2) NOT NULL,
    tempo_estimado_minutos  INTEGER      NOT NULL DEFAULT 60,
    ativo                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE pecas (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(255)   NOT NULL,
    descricao           TEXT,
    preco_unitario      NUMERIC(10, 2) NOT NULL,
    quantidade_estoque  INTEGER        NOT NULL DEFAULT 0,
    codigo_referencia   VARCHAR(100),
    ativo               BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE ordens_servico (
    id                  BIGSERIAL PRIMARY KEY,
    numero              VARCHAR(50)    NOT NULL UNIQUE,
    cliente_id          BIGINT         NOT NULL REFERENCES clientes (id),
    veiculo_id          BIGINT         NOT NULL REFERENCES veiculos (id),
    status              VARCHAR(30)    NOT NULL,
    descricao_problema  TEXT,
    observacoes         TEXT,
    valor_total         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    data_abertura       TIMESTAMP      NOT NULL,
    data_inicio         TIMESTAMP,
    data_finalizacao    TIMESTAMP,
    data_entrega        TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE itens_servico_os (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT         NOT NULL REFERENCES ordens_servico (id),
    servico_id        BIGINT         NOT NULL REFERENCES servicos (id),
    quantidade        INTEGER        NOT NULL DEFAULT 1,
    preco_unitario    NUMERIC(10, 2) NOT NULL
);

CREATE TABLE itens_peca_os (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT         NOT NULL REFERENCES ordens_servico (id),
    peca_id           BIGINT         NOT NULL REFERENCES pecas (id),
    quantidade        INTEGER        NOT NULL DEFAULT 1,
    preco_unitario    NUMERIC(10, 2) NOT NULL
);

CREATE TABLE usuarios (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ordens_servico_status      ON ordens_servico (status);
CREATE INDEX idx_ordens_servico_cliente_id  ON ordens_servico (cliente_id);
CREATE INDEX idx_ordens_servico_veiculo_id  ON ordens_servico (veiculo_id);
CREATE INDEX idx_veiculos_cliente_id        ON veiculos (cliente_id);
CREATE INDEX idx_clientes_documento         ON clientes (documento);
