-- Fase 4: saga orquestrada da OS, histórico de status, outbox e idempotência.

CREATE TABLE sagas_ordem_servico (
    id                  VARCHAR(36)  PRIMARY KEY,
    ordem_servico_id    BIGINT       NOT NULL REFERENCES ordens_servico (id),
    etapa               VARCHAR(30)  NOT NULL,
    orcamento_id        VARCHAR(64),
    pagamento_id        VARCHAR(64),
    link_pagamento      TEXT,
    estoque_reservado   BOOLEAN      NOT NULL DEFAULT FALSE,
    motivo_compensacao  TEXT,
    prazo               TIMESTAMP,
    criada_em           TIMESTAMP    NOT NULL,
    atualizada_em       TIMESTAMP    NOT NULL,
    versao              BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_sagas_ordem_servico ON sagas_ordem_servico (ordem_servico_id);
CREATE INDEX idx_sagas_prazo ON sagas_ordem_servico (etapa, prazo);

CREATE TABLE saga_passos (
    saga_id     VARCHAR(36)  NOT NULL REFERENCES sagas_ordem_servico (id) ON DELETE CASCADE,
    posicao     INTEGER      NOT NULL,
    etapa       VARCHAR(30)  NOT NULL,
    evento      VARCHAR(50)  NOT NULL,
    descricao   TEXT,
    ocorrido_em TIMESTAMP    NOT NULL,
    PRIMARY KEY (saga_id, posicao)
);

CREATE TABLE historico_status_os (
    id                BIGSERIAL    PRIMARY KEY,
    ordem_servico_id  BIGINT       NOT NULL REFERENCES ordens_servico (id),
    status_anterior   VARCHAR(30),
    status_novo       VARCHAR(30)  NOT NULL,
    origem            VARCHAR(100),
    observacao        TEXT,
    ocorrido_em       TIMESTAMP    NOT NULL
);

CREATE INDEX idx_historico_os ON historico_status_os (ordem_servico_id, ocorrido_em);

CREATE TABLE outbox_mensagens (
    id            VARCHAR(36)  PRIMARY KEY,
    destino       VARCHAR(50)  NOT NULL,
    tipo          VARCHAR(50)  NOT NULL,
    saga_id       VARCHAR(64),
    conteudo      TEXT         NOT NULL,
    cabecalhos    TEXT,
    criada_em     TIMESTAMP    NOT NULL,
    publicada_em  TIMESTAMP,
    tentativas    INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pendentes ON outbox_mensagens (publicada_em, criada_em);

CREATE TABLE mensagens_processadas (
    id            VARCHAR(36) PRIMARY KEY,
    tipo          VARCHAR(50) NOT NULL,
    processada_em TIMESTAMP   NOT NULL
);
