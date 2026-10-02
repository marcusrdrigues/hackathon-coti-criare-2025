-- =====================================================================
-- V3: pessoas e organizações separadas (spec 001, R1; docs/adr/0015)
--
-- Antes, a conta de acesso morava dentro de tb_empresa e tb_fornecedor:
-- uma empresa era um login. Agora:
--   tb_organizacao  a empresa compradora ou o fornecedor (razão social, CNPJ)
--   tb_usuario      a pessoa que entra no sistema (nome, e-mail, senha)
--   tb_membro       a pessoa numa organização, com um papel
--
-- Os dados existentes são preservados. Cada empresa e cada fornecedor vira
-- uma organização com o MESMO id, e ganha um usuário proprietário também com
-- esse id. Assim, tudo o que guardava o id da conta (remetente das mensagens,
-- dono das sessões) continua apontando para o lugar certo sem conversão.
--
-- tb_empresa, tb_fornecedor e tb_perfil deixam de ser usadas e só saem numa
-- migração futura, depois de a versão nova estar validada em produção.
--
-- As chaves estrangeiras antigas (para tb_empresa e tb_fornecedor) já foram
-- removidas pela V2.1, que as encontra pelo que ligam, e não pelo nome.
-- =====================================================================

CREATE TABLE tb_organizacao (
    id           UUID         NOT NULL,
    tipo         VARCHAR(20)  NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    cnpj         VARCHAR(14)  NOT NULL,
    criada_em    TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_organizacao PRIMARY KEY (id),
    -- A mesma empresa pode comprar e também fornecer: o CNPJ é único por tipo
    CONSTRAINT uk_organizacao_cnpj_tipo UNIQUE (cnpj, tipo),
    CONSTRAINT ck_organizacao_tipo CHECK (tipo IN ('EMPRESA', 'FORNECEDOR'))
);

CREATE TABLE tb_usuario (
    id            UUID         NOT NULL,
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(100) NOT NULL,
    senha_hash    VARCHAR(100) NOT NULL,
    superadmin    BOOLEAN      NOT NULL,
    criado_em     TIMESTAMP(6) NOT NULL,
    desativado_em TIMESTAMP(6),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE tb_membro (
    id             UUID         NOT NULL,
    usuario_id     UUID         NOT NULL,
    organizacao_id UUID         NOT NULL,
    papel          VARCHAR(20)  NOT NULL,
    criado_em      TIMESTAMP(6) NOT NULL,
    removido_em    TIMESTAMP(6),
    CONSTRAINT pk_membro PRIMARY KEY (id),
    CONSTRAINT uk_membro_usuario_organizacao UNIQUE (usuario_id, organizacao_id),
    CONSTRAINT fk_membro_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id),
    CONSTRAINT fk_membro_organizacao FOREIGN KEY (organizacao_id) REFERENCES tb_organizacao (id),
    CONSTRAINT ck_membro_papel CHECK (papel IN ('PROPRIETARIO', 'MEMBRO'))
);

CREATE INDEX idx_membro_organizacao ON tb_membro (organizacao_id);

-- ---------------------------------------------------------------------
-- Dados: empresas e fornecedores viram organizações com o proprietário
-- ---------------------------------------------------------------------

-- Tamanhos e máscara tratados também aqui: o banco de produção foi criado pelo
-- Hibernate, com colunas mais largas que as da V1
INSERT INTO tb_organizacao (id, tipo, razao_social, cnpj, criada_em)
SELECT id, 'EMPRESA', SUBSTRING(nome, 1, 200), REPLACE(REPLACE(REPLACE(cnpj, '.', ''), '/', ''), '-', ''),
       CURRENT_TIMESTAMP
FROM tb_empresa;

INSERT INTO tb_organizacao (id, tipo, razao_social, cnpj, criada_em)
SELECT id, 'FORNECEDOR', SUBSTRING(nome, 1, 200), REPLACE(REPLACE(REPLACE(cnpj, '.', ''), '/', ''), '-', ''),
       CURRENT_TIMESTAMP
FROM tb_fornecedor;

-- O nome da pessoa começa igual ao da organização; ela pode corrigir depois.
-- O e-mail já era único entre empresas e fornecedores (regra do cadastro) e
-- gravado em minúsculas; LOWER e TRIM só garantem o formato da tabela nova.
INSERT INTO tb_usuario (id, nome, email, senha_hash, superadmin, criado_em)
SELECT id, SUBSTRING(nome, 1, 150), LOWER(TRIM(email)), senha, FALSE, CURRENT_TIMESTAMP
FROM tb_empresa;

INSERT INTO tb_usuario (id, nome, email, senha_hash, superadmin, criado_em)
SELECT id, SUBSTRING(nome, 1, 150), LOWER(TRIM(email)), senha, FALSE, CURRENT_TIMESTAMP
FROM tb_fornecedor;

INSERT INTO tb_membro (id, usuario_id, organizacao_id, papel, criado_em)
SELECT id, id, id, 'PROPRIETARIO', CURRENT_TIMESTAMP
FROM tb_organizacao;

-- ---------------------------------------------------------------------
-- As tabelas do negócio passam a apontar para as organizações
-- (as colunas empresa_id e fornecedor_id mantêm o nome)
-- ---------------------------------------------------------------------

ALTER TABLE tb_cotacao ADD CONSTRAINT fk_cotacao_empresa FOREIGN KEY (empresa_id) REFERENCES tb_organizacao (id);
ALTER TABLE tb_proposta ADD CONSTRAINT fk_proposta_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES tb_organizacao (id);
ALTER TABLE tb_negociacao ADD CONSTRAINT fk_negociacao_empresa FOREIGN KEY (empresa_id) REFERENCES tb_organizacao (id);
ALTER TABLE tb_negociacao ADD CONSTRAINT fk_negociacao_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES tb_organizacao (id);

-- ---------------------------------------------------------------------
-- Quem fez cada ação: a pessoa, além da organização
-- ---------------------------------------------------------------------

ALTER TABLE tb_cotacao ADD COLUMN criada_por UUID;
UPDATE tb_cotacao SET criada_por = empresa_id;
ALTER TABLE tb_cotacao ALTER COLUMN criada_por SET NOT NULL;
ALTER TABLE tb_cotacao ADD CONSTRAINT fk_cotacao_criada_por FOREIGN KEY (criada_por) REFERENCES tb_usuario (id);

ALTER TABLE tb_proposta ADD COLUMN enviada_por UUID;
UPDATE tb_proposta SET enviada_por = fornecedor_id;
ALTER TABLE tb_proposta ALTER COLUMN enviada_por SET NOT NULL;
ALTER TABLE tb_proposta ADD CONSTRAINT fk_proposta_enviada_por FOREIGN KEY (enviada_por) REFERENCES tb_usuario (id);

-- O remetente já guardava o id da conta, que agora é o id do usuário
ALTER TABLE tb_mensagem_negociacao ADD CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES tb_usuario (id);

-- ---------------------------------------------------------------------
-- Sessões: o token novo carrega organização e papel, então todos entram de novo
-- ---------------------------------------------------------------------

-- O nome da restrição só é este nos bancos criados pela V1; no PostgreSQL, uma
-- restrição de outro nome sobre a coluna sai junto com ela.
DELETE FROM tb_refresh_token;
ALTER TABLE tb_refresh_token DROP CONSTRAINT IF EXISTS ck_refresh_token_tipo;
ALTER TABLE tb_refresh_token DROP COLUMN tipo_usuario;
ALTER TABLE tb_refresh_token ADD CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id);
