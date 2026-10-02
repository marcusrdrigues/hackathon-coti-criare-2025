-- =====================================================================
-- V2: quando cada parte leu a negociação pela última vez.
-- Mensagens da outra parte enviadas depois desse momento contam como
-- não lidas (contador na barra lateral e na lista de negociações).
-- A contagem usa o índice idx_mensagem_negociacao (negociacao_id, data_hora_envio) da V1.
-- =====================================================================

ALTER TABLE tb_negociacao ADD COLUMN lida_empresa_em TIMESTAMP(6);
ALTER TABLE tb_negociacao ADD COLUMN lida_fornecedor_em TIMESTAMP(6);

-- Conversas que já existiam começam lidas pelas duas partes: ninguém
-- deve abrir o sistema com um contador inflado pelo histórico.
UPDATE tb_negociacao SET lida_empresa_em = CURRENT_TIMESTAMP, lida_fornecedor_em = CURRENT_TIMESTAMP;
