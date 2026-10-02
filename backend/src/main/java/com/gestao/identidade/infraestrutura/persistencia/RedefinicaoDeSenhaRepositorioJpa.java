package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.RedefinicaoDeSenhaRepositorio;
import com.gestao.identidade.dominio.RedefinicaoDeSenha;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta dos links de redefinição com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class RedefinicaoDeSenhaRepositorioJpa implements RedefinicaoDeSenhaRepositorio {

    private final RedefinicaoDeSenhaJpa jpa;

    @Override
    public RedefinicaoDeSenha salvar(RedefinicaoDeSenha redefinicao) {
        return jpa.save(redefinicao);
    }

    @Override
    public Optional<RedefinicaoDeSenha> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash);
    }

    @Override
    public int substituirAbertos(UUID usuarioId, Instant agora) {
        return jpa.substituirAbertos(usuarioId, agora);
    }

    @Override
    public boolean usar(UUID id, Instant agora) {
        return jpa.usar(id, agora) == 1;
    }

    @Override
    public int apagarVencidosAntesDe(Instant limite) {
        return jpa.apagarVencidosAntesDe(limite);
    }
}
