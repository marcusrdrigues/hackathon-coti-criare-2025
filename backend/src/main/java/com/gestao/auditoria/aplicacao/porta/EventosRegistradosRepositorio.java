package com.gestao.auditoria.aplicacao.porta;

import com.gestao.auditoria.dominio.EventoRegistrado;

/** Porta de gravação dos eventos de segurança. Só acrescenta: não há como alterar nem apagar por aqui. */
public interface EventosRegistradosRepositorio {

    EventoRegistrado acrescentar(EventoRegistrado evento);
}
