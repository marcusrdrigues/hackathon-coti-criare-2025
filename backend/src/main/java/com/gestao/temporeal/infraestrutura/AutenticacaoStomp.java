package com.gestao.temporeal.infraestrutura;

import com.gestao.compras.aplicacao.AcessoCompras;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.infraestrutura.seguranca.ClaimsDoToken;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

/**
 * Segurança do canal STOMP, que não passa pelos filtros HTTP do Spring Security:
 *
 * <ul>
 *   <li>CONNECT: exige {@code Authorization: Bearer <token>} válido; a identidade fica na sessão</li>
 *   <li>SUBSCRIBE: só participantes assinam {@code /topic/negociacoes/{id}}; avisos só pela fila pessoal</li>
 *   <li>SEND: só para negociações de que o usuário participa</li>
 * </ul>
 *
 * Qualquer recusa vira um frame ERROR e a sessão é encerrada.
 */
@Component
@RequiredArgsConstructor
public class AutenticacaoStomp implements ChannelInterceptor {

    private static final String PREFIXO_BEARER = "Bearer ";
    private static final String PREFIXO_ENVIO_NEGOCIACAO = "/app/negociacoes/";

    private final JwtDecoder jwtDecoder;
    private final AcessoCompras acessoCompras;
    private final SessoesWebSocket sessoes;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acesso = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acesso == null || acesso.getCommand() == null) {
            return message;
        }
        StompCommand comando = acesso.getCommand();
        if (comando == StompCommand.CONNECT) {
            JwtAuthenticationToken usuario = autenticar(acesso.getFirstNativeHeader("Authorization"));
            acesso.setUser(usuario);
            sessoes.associar(acesso.getSessionId(), UUID.fromString(usuario.getToken().getSubject()));
        } else if (comando == StompCommand.SUBSCRIBE) {
            autorizarAssinatura(acesso.getDestination(), usuarioDa(acesso.getUser()));
        } else if (comando == StompCommand.SEND) {
            autorizarEnvio(acesso.getDestination(), usuarioDa(acesso.getUser()));
        }
        return message;
    }

    private JwtAuthenticationToken autenticar(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            throw new MessageDeliveryException("Conexão sem token de acesso.");
        }
        try {
            Jwt jwt = jwtDecoder.decode(cabecalho.substring(PREFIXO_BEARER.length()).trim());
            String organizacao = jwt.getClaimAsString(ClaimsDoToken.ORGANIZACAO);
            if (organizacao == null) {
                throw new MessageDeliveryException("Sessão antiga. Faça login novamente.");
            }
            // O nome na sessão é a organização: os avisos de /user/{id}/queue/avisos chegam a toda a equipe
            return new JwtAuthenticationToken(jwt, ClaimsDoToken.autoridades(jwt), organizacao);
        } catch (JwtException e) {
            throw new MessageDeliveryException("Token de acesso inválido ou expirado.");
        }
    }

    private void autorizarAssinatura(String destino, UsuarioAutenticado usuario) {
        if (destino == null) {
            throw new MessageDeliveryException("Assinatura sem destino.");
        }
        if (destino.equals(Destinos.PREFIXO_USUARIO + Destinos.FILA_AVISOS.substring(1))) {
            return;
        }
        if (destino.startsWith(Destinos.PREFIXO_NEGOCIACAO)) {
            exigirParticipante(destino.substring(Destinos.PREFIXO_NEGOCIACAO.length()), usuario);
            return;
        }
        throw new MessageDeliveryException("Destino não permitido: " + destino);
    }

    private void autorizarEnvio(String destino, UsuarioAutenticado usuario) {
        if (destino == null || !destino.startsWith(PREFIXO_ENVIO_NEGOCIACAO)) {
            throw new MessageDeliveryException("Destino não permitido.");
        }
        String resto = destino.substring(PREFIXO_ENVIO_NEGOCIACAO.length());
        int barra = resto.indexOf('/');
        exigirParticipante(barra < 0 ? resto : resto.substring(0, barra), usuario);
    }

    /**
     * A mesma política de acesso da API REST (ADR 0016). Id inválido, inexistente ou de
     * outra organização recebem a mesma recusa, para não revelar quais ids existem.
     */
    private void exigirParticipante(String id, UsuarioAutenticado usuario) {
        try {
            acessoCompras.negociacaoVisivel(UUID.fromString(id), usuario);
        } catch (RuntimeException e) {
            throw new MessageDeliveryException("Negociação não encontrada.");
        }
    }

    private static UsuarioAutenticado usuarioDa(Principal principal) {
        if (principal instanceof JwtAuthenticationToken token) {
            return UsuarioAtual.de(token.getToken());
        }
        throw new MessageDeliveryException("Sessão não autenticada.");
    }
}
