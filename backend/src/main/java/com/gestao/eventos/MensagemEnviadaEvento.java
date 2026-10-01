package com.gestao.eventos;

import java.util.UUID;

/** Uma mensagem (ou oferta) foi gravada numa negociação. */
public record MensagemEnviadaEvento(UUID mensagemId) {
}
