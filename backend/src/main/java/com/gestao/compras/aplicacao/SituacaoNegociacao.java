package com.gestao.compras.aplicacao;

/** Quais negociações listar. */
public enum SituacaoNegociacao {
    /** Só as em andamento: as que ainda pedem resposta */
    ANDAMENTO,
    /** Todas, com as em andamento primeiro */
    TODAS
}
