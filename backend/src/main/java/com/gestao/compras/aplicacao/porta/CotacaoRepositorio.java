package com.gestao.compras.aplicacao.porta;

import com.gestao.compras.dominio.Cotacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de persistência das cotações. Listas sempre das mais recentes para as mais antigas. */
public interface CotacaoRepositorio {

    Cotacao salvar(Cotacao cotacao);

    Optional<Cotacao> buscarPorId(UUID id);

    List<Cotacao> listarDaEmpresa(UUID empresaId);

    /** Abertas e ainda dentro do prazo no momento informado: o mural dos fornecedores. */
    List<Cotacao> listarAbertasVigentes(LocalDateTime agora);

    long contar();
}
