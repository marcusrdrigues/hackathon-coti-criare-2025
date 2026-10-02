package com.gestao.temporeal.infraestrutura;

import com.gestao.temporeal.aplicacao.AvisoTempoReal;
import com.gestao.temporeal.aplicacao.EventoNegociacao;
import com.gestao.temporeal.aplicacao.porta.CanalTempoReal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Adaptador: entrega os eventos pelo broker STOMP do Spring. */
@Component
@RequiredArgsConstructor
class CanalStomp implements CanalTempoReal {

    private final SimpMessagingTemplate mensageiro;

    @Override
    public void publicarNaNegociacao(UUID negociacaoId, EventoNegociacao evento) {
        mensageiro.convertAndSend(Destinos.negociacao(negociacaoId), evento);
    }

    @Override
    public void avisar(UUID organizacaoId, AvisoTempoReal aviso) {
        mensageiro.convertAndSendToUser(organizacaoId.toString(), Destinos.FILA_AVISOS, aviso);
    }
}
