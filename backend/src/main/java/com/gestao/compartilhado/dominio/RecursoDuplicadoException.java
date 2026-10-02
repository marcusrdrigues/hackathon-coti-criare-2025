package com.gestao.compartilhado.dominio;

public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String message) {
        super(message);
    }

    public RecursoDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}