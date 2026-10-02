package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das sessões (refresh tokens, guardados só como hash). */
public interface RefreshTokenRepositorio {

    RefreshToken salvar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(String tokenHash);

    /** Apaga todas as sessões do usuário, inclusive as já trocadas; devolve quantas foram apagadas. */
    int apagarTodasDoUsuario(UUID usuarioId);

    /** Apaga as sessões vencidas antes do limite; devolve quantas foram apagadas. */
    int apagarExpiradosAntesDe(Instant limite);

    long contarAtivos(UUID usuarioId);
}
