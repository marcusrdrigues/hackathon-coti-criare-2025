package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Membro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos vínculos entre pessoas e organizações. */
public interface MembroRepositorio {

    Membro salvar(Membro membro);

    Optional<Membro> buscarPorId(UUID id);

    /** O vínculo em vigor da pessoa (nesta fase, cada pessoa está em uma organização). */
    Optional<Membro> buscarAtivoDoUsuario(UUID usuarioId);

    /** A equipe atual: proprietários primeiro, depois por nome. */
    List<Membro> listarAtivosDaOrganizacao(UUID organizacaoId);
}
