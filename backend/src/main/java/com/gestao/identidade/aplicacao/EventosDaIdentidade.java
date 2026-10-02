package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.EventoDeSeguranca;
import com.gestao.identidade.aplicacao.porta.MembroRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Publica os eventos de segurança da identidade (spec 003, R2) já com a organização e o
 * e-mail da conta, para o console do superadmin filtrar por organização.
 */
@Component
@RequiredArgsConstructor
class EventosDaIdentidade {

    private final UsuarioRepositorio usuarioRepositorio;
    private final MembroRepositorio membroRepositorio;
    private final ApplicationEventPublisher eventos;

    /** Evento de uma conta conhecida pelo id. */
    void publicar(EventoDeSeguranca.Tipo tipo, UUID usuarioId, String detalhe) {
        Optional<Usuario> conta = usuarioRepositorio.buscarPorId(usuarioId);
        eventos.publishEvent(EventoDeSeguranca.de(tipo, usuarioId, organizacaoDe(usuarioId),
                conta.map(Usuario::getEmail).orElse(null), detalhe));
    }

    /** Evento de uma conta que já está em mãos. */
    void publicar(EventoDeSeguranca.Tipo tipo, Usuario conta, String detalhe) {
        eventos.publishEvent(EventoDeSeguranca.de(tipo, conta.getId(), organizacaoDe(conta.getId()),
                conta.getEmail(), detalhe));
    }

    /** Evento de uma tentativa de entrar: a conta pode não existir, e o e-mail digitado vai mascarado. */
    void publicarTentativa(EventoDeSeguranca.Tipo tipo, Optional<Usuario> conta, String emailDigitado,
                           String detalhe) {
        UUID usuarioId = conta.map(Usuario::getId).orElse(null);
        eventos.publishEvent(EventoDeSeguranca.de(tipo, usuarioId,
                usuarioId == null ? null : organizacaoDe(usuarioId), emailDigitado, detalhe));
    }

    /** Ação de uma pessoa da equipe sobre outra pessoa ou convite (o e-mail é o de quem foi afetado). */
    void publicarDaEquipe(EventoDeSeguranca.Tipo tipo, UsuarioAutenticado quem, String emailAfetado,
                          String detalhe) {
        eventos.publishEvent(EventoDeSeguranca.de(tipo, quem.usuarioId(), quem.organizacaoId(), emailAfetado,
                detalhe));
    }

    private UUID organizacaoDe(UUID usuarioId) {
        return membroRepositorio.buscarAtivoDoUsuario(usuarioId)
                .map(membro -> membro.getOrganizacao().getId())
                .orElse(null);
    }
}
