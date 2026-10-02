package com.gestao.compartilhado.dominio;

public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String message) {
        super(message);
    }

    public NaoAutenticadoException(String message, Throwable cause) {
        super(message, cause);
    }
}