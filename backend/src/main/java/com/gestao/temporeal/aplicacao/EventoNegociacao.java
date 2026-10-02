package com.gestao.temporeal.aplicacao;

import com.gestao.compras.aplicacao.dto.MensagemResponse;
import com.gestao.compras.aplicacao.dto.NegociacaoResponse;
import com.gestao.compras.dominio.TipoRemetente;

/**
 * O que trafega em /topic/negociacoes/{id}.
 *
 * MENSAGEM:  mensagem nova + negociação atualizada (última oferta)
 * STATUS:    a negociação foi fechada ou encerrada
 * DIGITANDO: uma das partes está escrevendo (remetente diz qual)
 */
public record EventoNegociacao(Tipo tipo, MensagemResponse mensagem, NegociacaoResponse negociacao, TipoRemetente remetente) {

    public enum Tipo {
        MENSAGEM,
        STATUS,
        DIGITANDO
    }

    public static EventoNegociacao mensagem(MensagemResponse mensagem, NegociacaoResponse negociacao) {
        return new EventoNegociacao(Tipo.MENSAGEM, mensagem, negociacao, mensagem.tipoRemetente());
    }

    public static EventoNegociacao status(NegociacaoResponse negociacao) {
        return new EventoNegociacao(Tipo.STATUS, null, negociacao, null);
    }

    public static EventoNegociacao digitando(TipoRemetente remetente) {
        return new EventoNegociacao(Tipo.DIGITANDO, null, null, remetente);
    }
}
