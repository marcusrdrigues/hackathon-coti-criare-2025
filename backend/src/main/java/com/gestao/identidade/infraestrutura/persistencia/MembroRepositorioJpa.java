package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.MembroRepositorio;
import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Papel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta dos vínculos com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class MembroRepositorioJpa implements MembroRepositorio {

    private final MembroJpa jpa;

    @Override
    public Membro salvar(Membro membro) {
        return jpa.save(membro);
    }

    @Override
    public Optional<Membro> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Membro> buscarAtivoDoUsuario(UUID usuarioId) {
        return jpa.ativoDoUsuario(usuarioId);
    }

    @Override
    public List<Membro> listarAtivosDaOrganizacao(UUID organizacaoId) {
        return jpa.ativosDaOrganizacao(organizacaoId, Papel.PROPRIETARIO);
    }
}
