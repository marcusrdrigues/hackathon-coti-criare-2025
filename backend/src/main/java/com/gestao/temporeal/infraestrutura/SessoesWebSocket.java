package com.gestao.temporeal.infraestrutura;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * As conexões abertas e de quem são. Serve para encerrar na hora as conexões de quem saiu
 * da organização: sem isso, a pessoa continuaria recebendo os avisos da equipe.
 *
 * <p>O id da sessão STOMP é o mesmo da sessão WebSocket, e o dono é associado no CONNECT
 * ({@link AutenticacaoStomp}).
 */
@Slf4j
@Component
class SessoesWebSocket implements WebSocketHandlerDecoratorFactory {

    static final CloseStatus FORA_DA_ORGANIZACAO =
            CloseStatus.POLICY_VIOLATION.withReason("Você não faz mais parte desta organização.");

    private final Map<String, WebSocketSession> abertas = new ConcurrentHashMap<>();
    private final Map<String, UUID> donos = new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession sessao) throws Exception {
                abertas.put(sessao.getId(), sessao);
                super.afterConnectionEstablished(sessao);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession sessao, CloseStatus status) throws Exception {
                abertas.remove(sessao.getId());
                donos.remove(sessao.getId());
                super.afterConnectionClosed(sessao, status);
            }
        };
    }

    void associar(String sessaoId, UUID usuarioId) {
        if (sessaoId != null) {
            donos.put(sessaoId, usuarioId);
        }
    }

    /** Fecha todas as conexões da pessoa; devolve quantas foram fechadas. */
    int encerrarDe(UUID usuarioId) {
        int encerradas = 0;
        for (Map.Entry<String, UUID> dono : donos.entrySet()) {
            WebSocketSession sessao = abertas.get(dono.getKey());
            if (dono.getValue().equals(usuarioId) && sessao != null) {
                try {
                    sessao.close(FORA_DA_ORGANIZACAO);
                    encerradas++;
                } catch (IOException e) {
                    log.warn("Não foi possível encerrar a conexão {}: {}", dono.getKey(), e.getMessage());
                }
            }
        }
        return encerradas;
    }
}
