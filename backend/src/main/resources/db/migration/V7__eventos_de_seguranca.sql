-- =====================================================================
-- V7: eventos de segurança (spec 003, R2; docs/adr/0021)
--
-- Login, falha e bloqueio, sessão revogada por reuso, saída, convites,
-- remoção da equipe, mudanças no superadmin e acessos negados. Cada linha
-- é gravada numa transação própria, então fica mesmo quando a operação
-- que a gerou é desfeita.
--
-- O e-mail entra só mascarado (m***@empresa.com) e nenhuma credencial
-- entra aqui. Sem chave estrangeira: o evento continua válido mesmo
-- depois de a pessoa ou a organização deixarem de existir.
-- =====================================================================

CREATE TABLE tb_evento_seguranca (
    id              UUID                        NOT NULL,
    tipo            VARCHAR(40)                 NOT NULL,
    ocorrido_em     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    usuario_id      UUID,
    organizacao_id  UUID,
    email_mascarado VARCHAR(100),
    detalhe         VARCHAR(500),
    trace_id        VARCHAR(32),
    CONSTRAINT pk_evento_seguranca PRIMARY KEY (id)
);

-- O console filtra por período, por tipo e por organização
CREATE INDEX idx_evento_seguranca_momento ON tb_evento_seguranca (ocorrido_em);
CREATE INDEX idx_evento_seguranca_tipo ON tb_evento_seguranca (tipo, ocorrido_em);
CREATE INDEX idx_evento_seguranca_organizacao ON tb_evento_seguranca (organizacao_id, ocorrido_em);
CREATE INDEX idx_evento_seguranca_usuario ON tb_evento_seguranca (usuario_id);
