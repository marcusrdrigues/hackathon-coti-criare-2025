package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.Fornecedor;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência dos fornecedores. */
public interface FornecedorRepositorio {

    Fornecedor salvar(Fornecedor fornecedor);

    Optional<Fornecedor> buscarPorId(UUID id);

    Optional<Fornecedor> buscarPorEmail(String email);

    boolean existeComEmail(String email);

    boolean existeComCnpj(String cnpj);
}
