package com.gestao.compras.aplicacao.porta;

import com.gestao.compras.dominio.Proposta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das propostas. */
public interface PropostaRepositorio {

    Proposta salvar(Proposta proposta);

    Optional<Proposta> buscarPorId(UUID id);

    /** Propostas de uma cotação, da mais barata para a mais cara. */
    List<Proposta> listarDaCotacao(UUID cotacaoId);

    /** Propostas de um fornecedor, da mais cara para a mais barata. */
    List<Proposta> listarDoFornecedor(UUID fornecedorId);

    boolean existeDoFornecedorNaCotacao(UUID fornecedorId, UUID cotacaoId);

    void excluir(Proposta proposta);
}
