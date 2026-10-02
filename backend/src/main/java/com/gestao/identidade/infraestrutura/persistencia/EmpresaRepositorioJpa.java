package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.EmpresaRepositorio;
import com.gestao.identidade.dominio.Empresa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador: implementa a porta da aplicação com o Spring Data JPA. */
@Repository
@RequiredArgsConstructor
class EmpresaRepositorioJpa implements EmpresaRepositorio {

    private final EmpresaJpa jpa;

    @Override
    public Empresa salvar(Empresa empresa) {
        return jpa.save(empresa);
    }

    @Override
    public Optional<Empresa> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Empresa> buscarPorEmail(String email) {
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

    @Override
    public long contar() {
        return jpa.count();
    }
}
