package com.gestao.dtos.cotacao;

import com.gestao.enums.StatusCotacao;
import java.time.LocalDateTime;
import java.util.UUID;

public record CotacaoResponse(
        UUID id,
        String nomeServico,
        String requisitos,
        LocalDateTime dataCriacao,
        LocalDateTime dataLimite,
        StatusCotacao status,
        UUID empresaId,
        String empresaNome,
        long quantidadePropostas
) {}