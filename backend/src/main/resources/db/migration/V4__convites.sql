-- =====================================================================
-- V4: convites para a equipe de uma organização (spec 001, R2; docs/adr/0017)
--
-- O proprietário convida alguém pelo nome e e-mail e recebe um link para
-- repassar. O banco guarda só o hash do token do link, como nas sessões:
-- quem tiver acesso ao banco não consegue usar os convites gravados.
-- Um convite vale 72 horas e uma vez só.
-- =====================================================================

CREATE TABLE tb_convite (
    id             UUID                        NOT NULL,
    organizacao_id UUID                        NOT NULL,
    nome           VARCHAR(150)                NOT NULL,
    email          VARCHAR(100)                NOT NULL,
    token_hash     VARCHAR(64)                 NOT NULL,
    convidado_por  UUID                        NOT NULL,
    criado_em      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expira_em      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    aceito_em      TIMESTAMP(6) WITH TIME ZONE,
    cancelado_em   TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_convite PRIMARY KEY (id),
    CONSTRAINT uk_convite_token UNIQUE (token_hash),
    CONSTRAINT fk_convite_organizacao FOREIGN KEY (organizacao_id) REFERENCES tb_organizacao (id),
    CONSTRAINT fk_convite_convidado_por FOREIGN KEY (convidado_por) REFERENCES tb_usuario (id)
);

CREATE INDEX idx_convite_organizacao ON tb_convite (organizacao_id);
