package com.gestao.auditoria.infraestrutura.persistencia;

import com.gestao.auditoria.aplicacao.porta.EventosRegistradosRepositorio;
import com.gestao.auditoria.dominio.EventoRegistrado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Adaptador: implementa a porta dos eventos de segurança com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class EventosRegistradosRepositorioJpa implements EventosRegistradosRepositorio {

    private final EventoRegistradoJpa jpa;

    @Override
    public EventoRegistrado acrescentar(EventoRegistrado evento) {
        return jpa.save(evento);
    }
}
