package com.gestao.compras.aplicacao.dto;

import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.StatusCotacao;

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
