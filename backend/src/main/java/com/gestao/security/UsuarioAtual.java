package com.gestao.security;

import com.gestao.enums.TipoUsuario;
import com.gestao.exceptions.AcessoNegadoException;
import com.gestao.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dá aos controllers acesso ao usuário do token. A identidade sempre vem
 * daqui, nunca de um ID enviado no corpo da requisição.
 */
@Component
public class UsuarioAtual {

    public UsuarioAutenticado obter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return de(token.getToken());
        }
        throw new UnauthorizedException("Usuário não autenticado.");
    }

    /** Identidade a partir de um token já validado (também usado na conexão WebSocket). */
    public static UsuarioAutenticado de(Jwt jwt) {
        return new UsuarioAutenticado(
                UUID.fromString(jwt.getSubject()),
                TipoUsuario.valueOf(jwt.getClaimAsString(TokenService.CLAIM_TIPO)));
    }

    /** Garante que o usuário só altere o próprio cadastro. */
    public UsuarioAutenticado exigirMesmoUsuario(UUID id) {
        UsuarioAutenticado atual = obter();
        if (!atual.id().equals(id)) {
            throw new AcessoNegadoException("Você só pode alterar o seu próprio cadastro.");
        }
        return atual;
    }
}
