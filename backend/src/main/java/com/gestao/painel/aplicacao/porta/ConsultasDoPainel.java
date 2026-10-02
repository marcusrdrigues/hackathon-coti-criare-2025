package com.gestao.painel.aplicacao.porta;

import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Consultas só de leitura que alimentam os painéis. É o lado de leitura (CQRS no
 * mesmo banco): números agregados direto do banco, sem passar pelas regras de escrita.
 */
public interface ConsultasDoPainel {

    long contarCotacoesDaEmpresa(UUID empresaId, StatusCotacao status);

    long contarPropostasRecebidas(UUID empresaId);

    List<FornecedorResumoProjection> fornecedoresQueMaisPropuseram(UUID empresaId, int limite);

    List<CategoriaResumoProjection> cotacoesPorCategoria(UUID empresaId);

    long contarCotacoesAbertasEmVigor(LocalDateTime agora);

    long contarPropostasDoFornecedor(UUID fornecedorId, Collection<StatusProposta> status);

    long contarNegociacoesDoFornecedor(UUID fornecedorId, StatusNegociacao status);

    /** Soma dos valores finais; zero quando não há negociação nesse status. */
    BigDecimal somarValorFinalDoFornecedor(UUID fornecedorId, StatusNegociacao status);
}
