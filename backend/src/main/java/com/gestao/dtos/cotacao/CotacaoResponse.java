package com.gestao.dtos.cotacao;

import com.gestao.enums.CategoriaCotacao;
import com.gestao.enums.StatusCotacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
        BigDecimal melhorOferta
) {}
