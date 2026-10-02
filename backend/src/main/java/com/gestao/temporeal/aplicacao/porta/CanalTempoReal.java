package com.gestao.temporeal.aplicacao.porta;

import com.gestao.temporeal.aplicacao.AvisoTempoReal;
import com.gestao.temporeal.aplicacao.EventoNegociacao;

import java.util.UUID;

/** Porta: entrega eventos a quem está conectado. A implementação (STOMP) fica na infraestrutura. */
public interface CanalTempoReal {

    /** Para as duas partes que acompanham a negociação. */
    void publicarNaNegociacao(UUID negociacaoId, EventoNegociacao evento);

    /** Para um usuário só, na fila pessoal de avisos. */
    void avisar(UUID usuarioId, AvisoTempoReal aviso);
}
