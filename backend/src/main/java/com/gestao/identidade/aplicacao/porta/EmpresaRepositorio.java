package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Empresa;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das empresas. A aplicação depende desta interface; a implementação fica na infraestrutura. */
public interface EmpresaRepositorio {

    Empresa salvar(Empresa empresa);

    Optional<Empresa> buscarPorId(UUID id);

    Optional<Empresa> buscarPorEmail(String email);

    boolean existeComEmail(String email);

    boolean existeComCnpj(String cnpj);

    long contar();
}
