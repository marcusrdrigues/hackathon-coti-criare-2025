package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.OrganizacaoRepositorio;
import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.TipoOrganizacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta das organizações com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class OrganizacaoRepositorioJpa implements OrganizacaoRepositorio {

    private final OrganizacaoJpa jpa;

    @Override
    public Organizacao salvar(Organizacao organizacao) {
        return jpa.save(organizacao);
    }

    @Override
    public Optional<Organizacao> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existeComCnpj(String cnpj, TipoOrganizacao tipo) {
        return jpa.existsByCnpjAndTipo(cnpj, tipo);
    }

    @Override
    public long contar() {
        return jpa.count();
    }
}
