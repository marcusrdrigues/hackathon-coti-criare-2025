package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.RedefinicaoDeSenha;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos links de redefinição de senha (guardados só como hash). */
public interface RedefinicaoDeSenhaRepositorio {

    RedefinicaoDeSenha salvar(RedefinicaoDeSenha redefinicao);

    Optional<RedefinicaoDeSenha> buscarPorHash(String tokenHash);

    /** Marca os links ainda abertos da pessoa como substituídos: só o mais novo vale. */
    int substituirAbertos(UUID usuarioId, Instant agora);

    /**
     * Marca o link como usado, só se ninguém o usou antes.
     *
     * @return {@code true} se foi este pedido que usou o link
     */
    boolean usar(UUID id, Instant agora);

    /** Apaga os links vencidos antes do limite; devolve quantos foram apagados. */
    int apagarVencidosAntesDe(Instant limite);
}
