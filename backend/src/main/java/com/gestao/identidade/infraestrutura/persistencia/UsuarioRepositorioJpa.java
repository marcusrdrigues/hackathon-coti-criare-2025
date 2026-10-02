package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das pessoas com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class UsuarioRepositorioJpa implements UsuarioRepositorio {

    private final UsuarioJpa jpa;

    @Override
    public Usuario salvar(Usuario usuario) {
        return jpa.save(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public boolean existeComEmail(String email) {
        return jpa.existsByEmail(email);
    }
}
