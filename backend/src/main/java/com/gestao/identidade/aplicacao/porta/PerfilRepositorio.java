package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Perfil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos perfis de acesso. */
public interface PerfilRepositorio {

    Perfil salvar(Perfil perfil);

    Optional<Perfil> buscarPorId(UUID id);

    Optional<Perfil> buscarPorNome(String nome);

    List<Perfil> listarTodos();

    boolean existeComNome(String nome);
}
