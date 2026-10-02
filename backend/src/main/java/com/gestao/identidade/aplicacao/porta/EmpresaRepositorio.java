package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Empresa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das empresas. A aplicação depende desta interface; a implementação fica na infraestrutura. */
public interface EmpresaRepositorio {

    Empresa salvar(Empresa empresa);

    Optional<Empresa> buscarPorId(UUID id);

    Optional<Empresa> buscarPorEmail(String email);

    List<Empresa> listarTodos();

    boolean existeComEmail(String email);

    boolean existeComCnpj(String cnpj);

    void excluir(Empresa empresa);

    long contar();
}
