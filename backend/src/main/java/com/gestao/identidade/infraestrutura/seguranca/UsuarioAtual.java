package com.gestao.identidade.infraestrutura.seguranca;

import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;
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

    /**
     * Identidade a partir de um token já validado (também usado na conexão WebSocket).
     * Um token de antes da separação entre pessoa e organização não tem as claims novas:
     * vale como não autenticado, e o front-end renova a sessão ou pede login.
     */
    public static UsuarioAutenticado de(Jwt jwt) {
        String organizacao = jwt.getClaimAsString(ClaimsDoToken.ORGANIZACAO);
        String tipo = jwt.getClaimAsString(ClaimsDoToken.TIPO);
        String papel = jwt.getClaimAsString(ClaimsDoToken.PAPEL);
        if (Papel.SUPERADMIN.name().equals(papel)) {
            return UsuarioAutenticado.superadmin(UUID.fromString(jwt.getSubject()));
        }
        if (organizacao == null || tipo == null || papel == null) {
            throw new NaoAutenticadoException("Sessão antiga. Faça login novamente.");
        }
        return new UsuarioAutenticado(UUID.fromString(jwt.getSubject()), UUID.fromString(organizacao),
                TipoOrganizacao.valueOf(tipo), Papel.valueOf(papel));
    }
}
