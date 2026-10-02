package com.gestao.compras.aplicacao.dto;

import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PropostaResponse(
        UUID id,
        BigDecimal valor,
        String descricao,
        StatusProposta status,
        LocalDateTime dataEnvio,
        UUID fornecedorId,
        String fornecedorNome,
        String fornecedorCnpj,
        UUID cotacaoId,
        String cotacaoNome,
        StatusCotacao cotacaoStatus,
        UUID empresaId,
        String empresaNome,
        UUID negociacaoId,
        StatusNegociacao negociacaoStatus,
        BigDecimal valorFinal
) {}
