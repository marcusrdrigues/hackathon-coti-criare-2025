package com.gestao.auditoria.infraestrutura.persistencia;

import com.gestao.auditoria.dominio.OrigemDaRevisao;
import com.gestao.compartilhado.infraestrutura.web.Rastreio;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Preenche cada revisão com quem está agindo. O Envers cria esta classe sozinho (não é um bean
 * do Spring), então ela lê o usuário do contexto de segurança da thread, como o resto da API.
 */
public class AutorDaRevisao implements RevisionListener {

    @Override
    public void newRevision(Object entidade) {
        Revisao revisao = (Revisao) entidade;
        String rastreio = Rastreio.idAtual();
        UsuarioAutenticado usuario = usuarioDaRequisicao();
        if (usuario != null) {
            revisao.preencher(usuario.usuarioId(), usuario.organizacaoId(), rastreio, OrigemDaRevisao.PESSOA);
        } else if (RequestContextHolder.getRequestAttributes() != null) {
            // Rota pública (cadastro, aceite de convite): quem age é a pessoa que está entrando
            revisao.preencher(null, null, rastreio, OrigemDaRevisao.PUBLICO);
        } else {
            revisao.preencher(null, null, rastreio, OrigemDaRevisao.SISTEMA);
        }
    }

    private static UsuarioAutenticado usuarioDaRequisicao() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            try {
                return UsuarioAtual.de(token.getToken());
            } catch (RuntimeException e) {
                return null; // token sem as informações de agora: tratado como sem pessoa
            }
        }
        return null;
    }
}
