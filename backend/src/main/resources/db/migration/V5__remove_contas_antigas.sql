-- =====================================================================
-- V5: remove as tabelas de conta de antes da separação entre pessoa e
-- organização (spec 001, passo 10; docs/adr/0015)
--
-- Desde a V3, as contas vivem em tb_organizacao, tb_usuario e tb_membro,
-- com os mesmos ids. As tabelas antigas ficaram no banco, sem uso, até a
-- versão nova ser validada em produção. A V2.1 já soltou as chaves que
-- apontavam para elas, e nada no código as lê.
--
-- Sem CASCADE de propósito: se alguma chave ainda apontar para elas, a
-- migração falha, o PostgreSQL desfaz tudo e o deploy mantém a versão
-- anterior, em vez de apagar uma dependência sem aviso.
-- =====================================================================

-- Das que dependem para a referenciada (empresa e fornecedor apontam para o perfil)
DROP TABLE IF EXISTS tb_empresa;
DROP TABLE IF EXISTS tb_fornecedor;
DROP TABLE IF EXISTS tb_perfil;
