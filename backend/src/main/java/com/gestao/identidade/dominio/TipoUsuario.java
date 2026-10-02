package com.gestao.identidade.dominio;

/** Perfil de quem está autenticado. Vai no token JWT como a claim "tipo". */
public enum TipoUsuario {
    EMPRESA,
    FORNECEDOR
}
