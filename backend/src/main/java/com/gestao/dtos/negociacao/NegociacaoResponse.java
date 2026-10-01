package com.gestao.dtos.negociacao;

import com.gestao.enums.StatusNegociacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record NegociacaoResponse(
        UUID id,
        BigDecimal valorFinal,
        StatusNegociacao status,
        LocalDateTime dataInicio,
        LocalDate dataFinalizacao,
        UUID empresaId,
        String empresaNome,
        UUID fornecedorId,
        String fornecedorNome,
        UUID propostaId
) {}