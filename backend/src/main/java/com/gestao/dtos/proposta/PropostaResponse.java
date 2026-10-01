package com.gestao.dtos.proposta;

import com.gestao.enums.StatusProposta;

import java.math.BigDecimal;
import java.util.UUID;

public record PropostaResponse(
        UUID id,
        BigDecimal valor,
        String descricao,
        StatusProposta status,
        UUID fornecedorId,
        String fornecedorNome,
        UUID cotacaoId,
        String cotacaoNome
) {}