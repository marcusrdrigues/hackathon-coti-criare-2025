package com.gestao.compras.dominio;

import java.util.UUID;

/** A negociação começou, foi fechada ou foi encerrada sem acordo. */
public record NegociacaoAlteradaEvento(UUID negociacaoId, Tipo tipo) {

    public enum Tipo {
        INICIADA,
        FINALIZADA,
        CANCELADA
    }
}
