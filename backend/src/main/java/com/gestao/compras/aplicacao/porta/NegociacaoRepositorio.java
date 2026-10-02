package com.gestao.compras.aplicacao.porta;

import com.gestao.compras.dominio.Negociacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das negociações. Listas das mais recentes para as mais antigas. */
public interface NegociacaoRepositorio {

    Negociacao salvar(Negociacao negociacao);

    Optional<Negociacao> buscarPorId(UUID id);

    Optional<Negociacao> buscarPorProposta(UUID propostaId);

    boolean existeParaProposta(UUID propostaId);

    List<Negociacao> listarDaEmpresa(UUID empresaId);

    List<Negociacao> listarDoFornecedor(UUID fornecedorId);

    long contar();
}
