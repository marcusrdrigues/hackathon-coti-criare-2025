package com.gestao.auditoria.aplicacao;

import com.gestao.auditoria.aplicacao.porta.EventosRegistradosRepositorio;
import com.gestao.auditoria.dominio.EventoRegistrado;
import com.gestao.compartilhado.dominio.EventoDeSeguranca;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Grava os eventos de segurança (spec 003, R2) numa transação própria: a senha errada fica
 * registrada mesmo com o login desfeito, e o convite criado, mesmo que algo falhe depois.
 */
@Service
@RequiredArgsConstructor
public class RegistroDeEventosDeSeguranca {

    private final EventosRegistradosRepositorio repositorio;

    /**
     * @param usuarioId     quem agiu, já resolvido por quem chama (do evento ou da requisição)
     * @param organizacaoId a organização dessa pessoa
     * @param traceId       o identificador de rastreio da requisição, se houver
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventoRegistrado registrar(EventoDeSeguranca evento, UUID usuarioId, UUID organizacaoId, String traceId) {
        return repositorio.acrescentar(new EventoRegistrado(evento, usuarioId, organizacaoId, traceId, Instant.now()));
    }
}
