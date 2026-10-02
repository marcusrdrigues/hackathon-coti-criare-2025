package com.gestao.compras.dominio;

import java.util.UUID;

/** Uma mensagem (ou oferta) foi gravada numa negociação. */
public record MensagemEnviadaEvento(UUID mensagemId) {
}
