package com.gestao.temporeal.infraestrutura;

import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.infraestrutura.seguranca.EmissorDeTokenJwt;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
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
    private final NegociacaoService negociacaoService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acesso = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acesso == null || acesso.getCommand() == null) {
            return message;
        }
        StompCommand comando = acesso.getCommand();
        if (comando == StompCommand.CONNECT) {
            acesso.setUser(autenticar(acesso.getFirstNativeHeader("Authorization")));
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
            String tipo = jwt.getClaimAsString(EmissorDeTokenJwt.CLAIM_TIPO);
            List<GrantedAuthority> perfis = tipo == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + tipo));
            // O nome do usuário na sessão é o id: é ele que endereça /user/{id}/queue/avisos
            return new JwtAuthenticationToken(jwt, perfis, jwt.getSubject());
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

    private void exigirParticipante(String id, UsuarioAutenticado usuario) {
        try {
            // Lança AcessoNegadoException se o usuário não for uma das partes
            negociacaoService.buscarParaParticipante(UUID.fromString(id), usuario);
        } catch (IllegalArgumentException e) {
            throw new MessageDeliveryException("Negociação inválida.");
        } catch (RuntimeException e) {
            throw new MessageDeliveryException("Você não participa desta negociação.");
        }
    }

    private static UsuarioAutenticado usuarioDa(Principal principal) {
        if (principal instanceof JwtAuthenticationToken token) {
            return UsuarioAtual.de(token.getToken());
        }
        throw new MessageDeliveryException("Sessão não autenticada.");
    }
}
