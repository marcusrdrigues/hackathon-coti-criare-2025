package com.gestao.identidade.dominio;

/** Lado da organização no negócio. Vai no token JWT como a claim "tipo". */
public enum TipoOrganizacao {
    /** Compra: publica cotações e fecha negócios */
    EMPRESA,
    /** Vende: envia propostas */
    FORNECEDOR
}
