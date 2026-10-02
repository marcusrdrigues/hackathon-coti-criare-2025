package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.TipoOrganizacao;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das organizações. */
public interface OrganizacaoRepositorio {

    Organizacao salvar(Organizacao organizacao);

    Optional<Organizacao> buscarPorId(UUID id);

    boolean existeComCnpj(String cnpj, TipoOrganizacao tipo);

    long contar();
}
