package com.gestao.compras.aplicacao.dto;

import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusProposta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma cotação. O que depende de quem pede: a {@code melhorOferta} só vai para a empresa dona
 * (o fornecedor não vê o lance do concorrente) e a {@code minhaProposta} só para o fornecedor.
 */
public record CotacaoResponse(
        UUID id,
        String nomeServico,
        String requisitos,
        CategoriaCotacao categoria,
        String categoriaDescricao,
        BigDecimal orcamentoEstimado,
        LocalDateTime dataCriacao,
        LocalDateTime dataLimite,
        StatusCotacao status,
        UUID empresaId,
        String empresaNome,
        long quantidadePropostas,
        @Schema(description = "Menor valor entre as propostas em jogo; só para a empresa dona")
        BigDecimal melhorOferta,
        @Schema(description = "A proposta que o fornecedor de quem pede enviou para esta cotação, se houver")
        MinhaProposta minhaProposta
) {

    public record MinhaProposta(UUID id, BigDecimal valor, StatusProposta status) {}
}
