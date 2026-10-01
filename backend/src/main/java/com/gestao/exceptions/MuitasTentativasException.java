package com.gestao.exceptions;

import lombok.Getter;

/** Muitas tentativas de login seguidas com senha errada (HTTP 429). */
@Getter
public class MuitasTentativasException extends RuntimeException {

    private final long segundosParaLiberar;

    public MuitasTentativasException(String message, long segundosParaLiberar) {
        super(message);
        this.segundosParaLiberar = segundosParaLiberar;
    }
}
