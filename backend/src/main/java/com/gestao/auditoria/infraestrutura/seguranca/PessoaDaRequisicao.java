package com.gestao.auditoria.infraestrutura.seguranca;

import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Quem está autenticado na requisição em andamento, para a auditoria. Lê o contexto de
 * segurança da thread, porque o Envers e os eventos chegam aqui sem passar por um controller.
 */
public final class PessoaDaRequisicao {

    private PessoaDaRequisicao() {
    }

    /** A pessoa do token desta requisição, ou vazio sem login (rota pública, rotina do sistema). */
    public static Optional<UsuarioAutenticado> atual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            try {
                return Optional.of(UsuarioAtual.de(token.getToken()));
            } catch (RuntimeException e) {
                return Optional.empty(); // token sem as informações de agora: tratado como sem pessoa
            }
        }
        return Optional.empty();
    }
}
