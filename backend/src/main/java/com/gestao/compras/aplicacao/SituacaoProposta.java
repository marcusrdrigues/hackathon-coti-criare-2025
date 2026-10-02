package com.gestao.compras.aplicacao;

/** Como o fornecedor separa as próprias propostas na lista. */
public enum SituacaoProposta {
    /** Aguardando análise, em análise ou com a negociação em andamento */
    ANDAMENTO,
    /** Encerradas: negócio fechado ou proposta recusada (inclusive por cancelamento ou outra escolha) */
    HISTORICO
}
