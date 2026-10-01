package com.gestao.temporeal;

import java.util.UUID;

/** Endereços STOMP usados pela API e pelo front-end. */
public final class Destinos {

    /** Endpoint do handshake WebSocket */
    public static final String ENDPOINT = "/ws";

    /** Eventos de uma negociação, para as duas partes: /topic/negociacoes/{id} */
    public static final String PREFIXO_NEGOCIACAO = "/topic/negociacoes/";

    /** Avisos pessoais (mensagem nova, proposta recebida...): o cliente assina /user/queue/avisos */
    public static final String FILA_AVISOS = "/queue/avisos";

    /** Prefixo das assinaturas pessoais */
    public static final String PREFIXO_USUARIO = "/user/";

    private Destinos() {
    }

    public static String negociacao(UUID id) {
        return PREFIXO_NEGOCIACAO + id;
    }
}
