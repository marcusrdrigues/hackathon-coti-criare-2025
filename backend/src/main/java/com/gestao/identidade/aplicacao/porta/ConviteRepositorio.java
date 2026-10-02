package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Convite;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos convites (guardados só com o hash do token). */
public interface ConviteRepositorio {

    Convite salvar(Convite convite);

    Optional<Convite> buscarPorId(UUID id);

    Optional<Convite> buscarPorHash(String tokenHash);

    /** Ainda não aceitos, não cancelados e dentro da validade, dos mais novos para os mais antigos. */
    List<Convite> listarPendentes(UUID organizacaoId, Instant agora);
}
