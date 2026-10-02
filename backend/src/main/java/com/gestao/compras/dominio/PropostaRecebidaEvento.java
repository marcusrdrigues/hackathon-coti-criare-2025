package com.gestao.compras.dominio;

import java.util.UUID;

/** Um fornecedor enviou uma proposta para uma cotação. */
public record PropostaRecebidaEvento(UUID propostaId) {
}
