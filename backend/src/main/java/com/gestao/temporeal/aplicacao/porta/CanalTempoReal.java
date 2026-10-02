package com.gestao.temporeal.aplicacao.porta;

import com.gestao.temporeal.aplicacao.AvisoTempoReal;
import com.gestao.temporeal.aplicacao.EventoNegociacao;

import java.util.UUID;

/** Porta: entrega eventos a quem está conectado. A implementação (STOMP) fica na infraestrutura. */
public interface CanalTempoReal {

    /** Para as duas partes que acompanham a negociação. */
    void publicarNaNegociacao(UUID negociacaoId, EventoNegociacao evento);

    /** Para toda a equipe de uma organização, na fila de avisos (cada pessoa conectada recebe). */
    void avisar(UUID organizacaoId, AvisoTempoReal aviso);

    /** Fecha as conexões abertas pela pessoa (por exemplo, quando ela sai da organização). */
    void encerrarConexoesDe(UUID usuarioId);
}
