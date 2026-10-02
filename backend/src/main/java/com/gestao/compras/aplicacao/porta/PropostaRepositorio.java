package com.gestao.compras.aplicacao.porta;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compras.aplicacao.SituacaoProposta;
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

    boolean existeDoFornecedorNaCotacao(UUID fornecedorId, UUID cotacaoId);

    void excluir(Proposta proposta);

    /**
     * Uma página das propostas do fornecedor numa situação: as com negociação em andamento
     * primeiro, depois as enviadas mais recentemente.
     */
    Pagina<Proposta> buscarDoFornecedor(UUID fornecedorId, SituacaoProposta situacao, PedidoDePagina pedido);
}
