package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.ConviteRepositorio;
import com.gestao.identidade.dominio.Convite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta dos convites com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class ConviteRepositorioJpa implements ConviteRepositorio {

    private final ConviteJpa jpa;

    @Override
    public Convite salvar(Convite convite) {
        return jpa.save(convite);
    }

    @Override
    public Optional<Convite> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Convite> buscarPorHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash);
    }

    @Override
    public List<Convite> listarPendentes(UUID organizacaoId, Instant agora) {
        return jpa.pendentes(organizacaoId, agora);
    }
}
