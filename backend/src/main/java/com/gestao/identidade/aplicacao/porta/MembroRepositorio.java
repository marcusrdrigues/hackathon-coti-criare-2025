package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Membro;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos vínculos entre pessoas e organizações. */
public interface MembroRepositorio {

    Membro salvar(Membro membro);

    /** O vínculo em vigor da pessoa (nesta fase, cada pessoa está em uma organização). */
    Optional<Membro> buscarAtivoDoUsuario(UUID usuarioId);
}
