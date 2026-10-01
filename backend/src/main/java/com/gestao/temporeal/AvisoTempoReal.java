package com.gestao.temporeal;

import java.util.UUID;

/**
 * Aviso pessoal, enviado para /user/queue/avisos de quem precisa saber.
 * O front-end mostra como notificação e atualiza contadores e listas.
 */
public record AvisoTempoReal(Tipo tipo, UUID negociacaoId, UUID cotacaoId, String titulo, String texto) {

    public enum Tipo {
        MENSAGEM,
        PROPOSTA_RECEBIDA,
        NEGOCIACAO_INICIADA,
        NEGOCIACAO_FINALIZADA,
        NEGOCIACAO_CANCELADA
    }
}
