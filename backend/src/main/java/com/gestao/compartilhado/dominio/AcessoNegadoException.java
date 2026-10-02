package com.gestao.compartilhado.dominio;

/** O usuário está autenticado, mas o recurso pertence a outra pessoa (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
