package com.gestao.auditoria.infraestrutura.eventos;

import com.gestao.auditoria.aplicacao.RegistroDeEventosDeSeguranca;
import com.gestao.auditoria.infraestrutura.seguranca.PessoaDaRequisicao;
import com.gestao.compartilhado.dominio.EventoDeSeguranca;
import com.gestao.compartilhado.infraestrutura.web.Rastreio;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Ouve os eventos de segurança de toda a aplicação e manda gravar, na hora em que acontecem
 * (spec 003, R2). Completa o evento com a pessoa autenticada e o rastreio da requisição.
 *
 * <p>Se a gravação falhar, a ação de quem publicou continua: derrubar um login porque a
 * auditoria caiu tiraria o portal do ar por um problema secundário. A falha vai para o log,
 * com o rastreio, para ser investigada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OuvinteDeEventosDeSeguranca {

    private final RegistroDeEventosDeSeguranca registro;

    @EventListener
    void aoAcontecer(EventoDeSeguranca evento) {
        try {
            UUID usuarioId = evento.usuarioId();
            UUID organizacaoId = evento.organizacaoId();
            if (usuarioId == null) {
                Optional<UsuarioAutenticado> pessoa = PessoaDaRequisicao.atual();
                usuarioId = pessoa.map(UsuarioAutenticado::usuarioId).orElse(null);
                organizacaoId = pessoa.map(UsuarioAutenticado::organizacaoId).orElse(organizacaoId);
            }
            registro.registrar(evento, usuarioId, organizacaoId, Rastreio.idAtual());
        } catch (RuntimeException e) {
            log.error("Evento de segurança {} não foi gravado.", evento.tipo(), e);
        }
    }
}
