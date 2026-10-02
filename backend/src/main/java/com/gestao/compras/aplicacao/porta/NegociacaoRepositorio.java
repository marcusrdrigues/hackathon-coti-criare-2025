package com.gestao.compras.aplicacao.porta;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compras.aplicacao.SituacaoNegociacao;
import com.gestao.compras.dominio.Negociacao;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das negociações. As listas são paginadas (ver docs/adr/0020). */
public interface NegociacaoRepositorio {

    Negociacao salvar(Negociacao negociacao);

    Optional<Negociacao> buscarPorId(UUID id);

    boolean existeParaProposta(UUID propostaId);

    long contar();

    /**
     * Uma página das negociações de que a organização participa: as em andamento primeiro,
     * depois as que começaram mais recentemente.
     *
     * @param comoEmpresa se a organização participa como a empresa (senão, como o fornecedor)
     */
    Pagina<Negociacao> buscarDaOrganizacao(UUID organizacaoId, boolean comoEmpresa, SituacaoNegociacao situacao,
                                           PedidoDePagina pedido);
}
