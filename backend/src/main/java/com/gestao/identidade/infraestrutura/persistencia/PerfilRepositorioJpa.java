package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.PerfilRepositorio;
import com.gestao.identidade.dominio.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta dos perfis com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class PerfilRepositorioJpa implements PerfilRepositorio {

    private final PerfilJpa jpa;

    @Override
    public Perfil salvar(Perfil perfil) {
        return jpa.save(perfil);
    }

    @Override
    public Optional<Perfil> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Perfil> buscarPorNome(String nome) {
        return jpa.findByNome(nome);
    }

    @Override
    public List<Perfil> listarTodos() {
        return jpa.findAll();
    }

    @Override
    public boolean existeComNome(String nome) {
        return jpa.existsByNome(nome);
    }
}
