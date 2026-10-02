package com.gestao.auditoria.infraestrutura.persistencia;

import com.gestao.auditoria.dominio.EventoRegistrado;
import org.springframework.data.repository.Repository;

import java.util.UUID;

/** Só o {@code save}: o repositório de eventos não expõe alteração nem exclusão. */
interface EventoRegistradoJpa extends Repository<EventoRegistrado, UUID> {

    EventoRegistrado save(EventoRegistrado evento);
}
