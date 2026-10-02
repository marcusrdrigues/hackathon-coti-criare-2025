package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Perfil;

import java.util.Optional;

/** Porta de persistência dos perfis de acesso. */
public interface PerfilRepositorio {

    Perfil salvar(Perfil perfil);

    Optional<Perfil> buscarPorNome(String nome);

    boolean existeComNome(String nome);
}
