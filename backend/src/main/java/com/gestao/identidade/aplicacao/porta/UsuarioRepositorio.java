package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Usuario;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das pessoas que entram no sistema. */
public interface UsuarioRepositorio {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existeComEmail(String email);
}
