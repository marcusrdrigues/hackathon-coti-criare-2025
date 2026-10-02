package com.gestao.identidade.infraestrutura.seguranca;

import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.TipoUsuario;
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
        throw new NaoAutenticadoException("Usuário não autenticado.");
    }

    /** Identidade a partir de um token já validado (também usado na conexão WebSocket). */
    public static UsuarioAutenticado de(Jwt jwt) {
        return new UsuarioAutenticado(
                UUID.fromString(jwt.getSubject()),
                TipoUsuario.valueOf(jwt.getClaimAsString(EmissorDeTokenJwt.CLAIM_TIPO)));
    }
}
