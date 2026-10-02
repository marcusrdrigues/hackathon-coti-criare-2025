package com.gestao.compras.dominio;

import com.gestao.identidade.dominio.TipoOrganizacao;

/** Quem escreveu uma mensagem da negociação: uma das duas partes. */
public enum TipoRemetente {
    EMPRESA,
    FORNECEDOR;

    /** Cada tipo de usuário fala na negociação como a parte correspondente. */
    public static TipoRemetente de(TipoOrganizacao tipo) {
        return valueOf(tipo.name());
    }
}
