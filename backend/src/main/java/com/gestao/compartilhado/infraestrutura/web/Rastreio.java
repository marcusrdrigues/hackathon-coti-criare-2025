package com.gestao.compartilhado.infraestrutura.web;

import org.slf4j.MDC;

/** Identificador de rastreio da requisição em andamento (o mesmo dos logs). */
public final class Rastreio {

    /** Chave usada no MDC pelo Micrometer Tracing e por {@link RastreioDeRequisicao}. */
    public static final String CHAVE = "traceId";

    /** Cabeçalho em que a API devolve o identificador. */
    public static final String CABECALHO = "X-Trace-Id";

    private Rastreio() {
    }

    /** O traceId da requisição atual, ou {@code null} fora de uma requisição. */
    public static String idAtual() {
        return MDC.get(CHAVE);
    }
}
