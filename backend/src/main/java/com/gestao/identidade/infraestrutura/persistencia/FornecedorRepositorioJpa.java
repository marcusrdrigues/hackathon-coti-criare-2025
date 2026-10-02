package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.FornecedorRepositorio;
import com.gestao.identidade.dominio.Fornecedor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta da aplicação com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class FornecedorRepositorioJpa implements FornecedorRepositorio {

    private final FornecedorJpa jpa;

    @Override
    public Fornecedor salvar(Fornecedor fornecedor) {
        return jpa.save(fornecedor);
    }

    @Override
    public Optional<Fornecedor> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Fornecedor> buscarPorEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public boolean existeComEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public boolean existeComCnpj(String cnpj) {
        return jpa.existsByCnpj(cnpj);
    }
}
