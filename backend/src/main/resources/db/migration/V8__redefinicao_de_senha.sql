-- =====================================================================
-- V8: links de redefinição de senha (spec 004)
--
-- Guarda só o hash SHA-256 do token: quem lê o banco não consegue usar
-- um link. Cada link vale 30 minutos e uma vez só; um pedido novo marca
-- os anteriores como substituídos. Os links somem junto com a pessoa.
-- =====================================================================

CREATE TABLE tb_redefinicao_senha (
    id             UUID                        NOT NULL,
    usuario_id     UUID                        NOT NULL,
    token_hash     VARCHAR(64)                 NOT NULL,
    criada_em      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expira_em      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    usada_em       TIMESTAMP(6) WITH TIME ZONE,
    substituida_em TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_redefinicao_senha PRIMARY KEY (id),
    CONSTRAINT uk_redefinicao_senha_hash UNIQUE (token_hash),
    CONSTRAINT fk_redefinicao_senha_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id) ON DELETE CASCADE
);

CREATE INDEX idx_redefinicao_senha_usuario ON tb_redefinicao_senha (usuario_id);
