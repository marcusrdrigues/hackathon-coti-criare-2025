package com.gestao.dtos.cotacao;

import com.gestao.enums.CategoriaCotacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A empresa dona da cotação é sempre o usuário do token. */
public record CotacaoRequest(
        @NotBlank(message = "Nome do serviço é obrigatório")
        @Size(max = 200, message = "Nome do serviço deve ter no máximo 200 caracteres")
        String nomeServico,

        @NotBlank(message = "Requisitos são obrigatórios")
        @Size(max = 1000, message = "Requisitos devem ter no máximo 1000 caracteres")
        String requisitos,

        CategoriaCotacao categoria,

        @DecimalMin(value = "0.01", message = "Orçamento estimado deve ser maior que zero")
        BigDecimal orcamentoEstimado,

        LocalDateTime dataLimite
) {}
