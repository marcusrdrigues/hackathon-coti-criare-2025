-- =====================================================================
-- V1: esquema inicial do Portal Criare
-- Espelha as entidades JPA. A partir daqui, toda mudança no banco entra
-- como uma nova migração (V2__..., V3__...) e o Hibernate só valida.
-- =====================================================================

CREATE TABLE tb_perfil (
    id   UUID        NOT NULL,
    nome VARCHAR(25) NOT NULL,
    CONSTRAINT pk_perfil PRIMARY KEY (id),
    CONSTRAINT uk_perfil_nome UNIQUE (nome)
);

CREATE TABLE tb_empresa (
    id        UUID         NOT NULL,
    nome      VARCHAR(200) NOT NULL,
    cnpj      VARCHAR(18)  NOT NULL,
    email     VARCHAR(100) NOT NULL,
    senha     VARCHAR(100) NOT NULL,
    perfil_id UUID         NOT NULL,
    CONSTRAINT pk_empresa PRIMARY KEY (id),
    CONSTRAINT uk_empresa_cnpj UNIQUE (cnpj),
    CONSTRAINT uk_empresa_email UNIQUE (email),
    CONSTRAINT fk_empresa_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil (id)
);

CREATE TABLE tb_fornecedor (
    id        UUID         NOT NULL,
    nome      VARCHAR(150) NOT NULL,
    cnpj      VARCHAR(14)  NOT NULL,
    email     VARCHAR(100) NOT NULL,
    senha     VARCHAR(100) NOT NULL,
    perfil_id UUID         NOT NULL,
    CONSTRAINT pk_fornecedor PRIMARY KEY (id),
    CONSTRAINT uk_fornecedor_cnpj UNIQUE (cnpj),
    CONSTRAINT uk_fornecedor_email UNIQUE (email),
    CONSTRAINT fk_fornecedor_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil (id)
);

CREATE TABLE tb_cotacao (
    id                 UUID          NOT NULL,
    nome_servico       VARCHAR(200)  NOT NULL,
    requisitos         VARCHAR(1000) NOT NULL,
    categoria          VARCHAR(30),
    orcamento_estimado NUMERIC(15, 2),
    data_criacao       TIMESTAMP(6)  NOT NULL,
    data_limite        TIMESTAMP(6),
    status             VARCHAR(255)  NOT NULL,
    empresa_id         UUID          NOT NULL,
    CONSTRAINT pk_cotacao PRIMARY KEY (id),
    CONSTRAINT fk_cotacao_empresa FOREIGN KEY (empresa_id) REFERENCES tb_empresa (id),
    CONSTRAINT ck_cotacao_status CHECK (status IN ('ABERTA', 'EM_NEGOCIACAO', 'FECHADA', 'CANCELADA')),
    CONSTRAINT ck_cotacao_categoria CHECK (categoria IN ('TECNOLOGIA', 'MOBILIARIO', 'LIMPEZA_MANUTENCAO',
                                                         'ALIMENTOS', 'SUPRIMENTOS', 'SERVICOS', 'OUTROS'))
);

CREATE INDEX idx_cotacao_empresa ON tb_cotacao (empresa_id);
CREATE INDEX idx_cotacao_status ON tb_cotacao (status);

CREATE TABLE tb_proposta (
    id             UUID           NOT NULL,
    valor_proposto NUMERIC(15, 2) NOT NULL,
    descricao      VARCHAR(1000)  NOT NULL,
    status         VARCHAR(255)   NOT NULL,
    data_envio     TIMESTAMP(6),
    fornecedor_id  UUID           NOT NULL,
    cotacao_id     UUID           NOT NULL,
    CONSTRAINT pk_proposta PRIMARY KEY (id),
    CONSTRAINT fk_proposta_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES tb_fornecedor (id),
    CONSTRAINT fk_proposta_cotacao FOREIGN KEY (cotacao_id) REFERENCES tb_cotacao (id),
    CONSTRAINT ck_proposta_status CHECK (status IN ('ENVIADA', 'EM_ANALISE', 'ACEITA', 'RECUSADA'))
);

CREATE INDEX idx_proposta_cotacao ON tb_proposta (cotacao_id);
CREATE INDEX idx_proposta_fornecedor ON tb_proposta (fornecedor_id);

CREATE TABLE tb_negociacao (
    id               UUID           NOT NULL,
    valor_final      NUMERIC(15, 2),
    status           VARCHAR(255)   NOT NULL,
    data_hora_inicio TIMESTAMP(6)   NOT NULL,
    datafinalizacao  DATE,
    proposta_id      UUID           NOT NULL,
    empresa_id       UUID           NOT NULL,
    fornecedor_id    UUID           NOT NULL,
    CONSTRAINT pk_negociacao PRIMARY KEY (id),
    CONSTRAINT uk_negociacao_proposta UNIQUE (proposta_id),
    CONSTRAINT fk_negociacao_proposta FOREIGN KEY (proposta_id) REFERENCES tb_proposta (id),
    CONSTRAINT fk_negociacao_empresa FOREIGN KEY (empresa_id) REFERENCES tb_empresa (id),
    CONSTRAINT fk_negociacao_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES tb_fornecedor (id),
    CONSTRAINT ck_negociacao_status CHECK (status IN ('EM_ANDAMENTO', 'FINALIZADA', 'CANCELADA'))
);

CREATE INDEX idx_negociacao_empresa ON tb_negociacao (empresa_id);
CREATE INDEX idx_negociacao_fornecedor ON tb_negociacao (fornecedor_id);

CREATE TABLE tb_mensagem_negociacao (
    id              UUID           NOT NULL,
    mensagem        VARCHAR(1000)  NOT NULL,
    valor_ofertado  NUMERIC(15, 2),
    tipo_remetente  VARCHAR(255)   NOT NULL,
    remetente_id    UUID           NOT NULL,
    data_hora_envio TIMESTAMP(6)   NOT NULL,
    negociacao_id   UUID           NOT NULL,
    CONSTRAINT pk_mensagem_negociacao PRIMARY KEY (id),
    CONSTRAINT fk_mensagem_negociacao FOREIGN KEY (negociacao_id) REFERENCES tb_negociacao (id),
    CONSTRAINT ck_mensagem_tipo_remetente CHECK (tipo_remetente IN ('EMPRESA', 'FORNECEDOR'))
);

CREATE INDEX idx_mensagem_negociacao ON tb_mensagem_negociacao (negociacao_id, data_hora_envio);

CREATE TABLE tb_refresh_token (
    id           UUID                        NOT NULL,
    token_hash   VARCHAR(64)                 NOT NULL,
    usuario_id   UUID                        NOT NULL,
    tipo_usuario VARCHAR(20)                 NOT NULL,
    criado_em    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expira_em    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revogado_em  TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_token_tipo CHECK (tipo_usuario IN ('EMPRESA', 'FORNECEDOR'))
);

CREATE INDEX idx_refresh_token_usuario ON tb_refresh_token (usuario_id);
