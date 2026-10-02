package com.gestao.auditoria.infraestrutura.persistencia;

import com.gestao.auditoria.dominio.OrigemDaRevisao;
import com.gestao.auditoria.infraestrutura.seguranca.PessoaDaRequisicao;
import com.gestao.compartilhado.infraestrutura.web.Rastreio;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import org.hibernate.envers.RevisionListener;
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
        UsuarioAutenticado usuario = PessoaDaRequisicao.atual().orElse(null);
        if (usuario != null) {
            revisao.preencher(usuario.usuarioId(), usuario.organizacaoId(), rastreio, OrigemDaRevisao.PESSOA);
        } else if (RequestContextHolder.getRequestAttributes() != null) {
            // Rota pública (cadastro, aceite de convite): quem age é a pessoa que está entrando
            revisao.preencher(null, null, rastreio, OrigemDaRevisao.PUBLICO);
        } else {
            revisao.preencher(null, null, rastreio, OrigemDaRevisao.SISTEMA);
        }
    }
}
