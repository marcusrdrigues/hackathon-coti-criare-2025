package com.gestao.compras.aplicacao.porta;

import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das cotações. Listas sempre das mais recentes para as mais antigas. */
public interface CotacaoRepositorio {

    Cotacao salvar(Cotacao cotacao);

    Optional<Cotacao> buscarPorId(UUID id);

    List<Cotacao> listarDaEmpresa(UUID empresaId);

    List<Cotacao> listarDaEmpresaPorStatus(UUID empresaId, StatusCotacao status);

    List<Cotacao> listarPorStatus(StatusCotacao status);

    /** Abertas e ainda dentro do prazo no momento informado: o mural dos fornecedores. */
    List<Cotacao> listarAbertasVigentes(LocalDateTime agora);

    void excluir(Cotacao cotacao);

    long contar();
}
