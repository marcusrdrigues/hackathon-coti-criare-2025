package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.RefreshTokenRepositorio;
import com.gestao.identidade.dominio.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das sessões com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class RefreshTokenRepositorioJpa implements RefreshTokenRepositorio {

    private final RefreshTokenJpa jpa;

    @Override
    public RefreshToken salvar(RefreshToken token) {
        return jpa.save(token);
    }

    @Override
    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash);
    }

    @Override
    public int revogarTodosDoUsuario(UUID usuarioId, Instant agora) {
        return jpa.revogarTodosDoUsuario(usuarioId, agora);
    }

    @Override
    public int apagarExpiradosAntesDe(Instant limite) {
        return jpa.apagarExpiradosAntesDe(limite);
    }

    @Override
    public long contarAtivos(UUID usuarioId) {
        return jpa.contarAtivos(usuarioId);
    }
}
