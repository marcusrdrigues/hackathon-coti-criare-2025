package com.gestao.dtos.negociacao;

import com.gestao.enums.StatusCotacao;
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
        UUID propostaId,
        BigDecimal valorProposta,
        BigDecimal ultimaOferta,
        UUID cotacaoId,
        String cotacaoNome,
        String cotacaoRequisitos,
        StatusCotacao cotacaoStatus,
        /* Mensagens da outra parte que quem pediu ainda não viu (null quando não se aplica) */
        Integer naoLidas
) {}
