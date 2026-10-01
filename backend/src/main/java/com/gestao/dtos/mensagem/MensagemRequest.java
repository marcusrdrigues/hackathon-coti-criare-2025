package com.gestao.dtos.mensagem;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mensagem dentro de uma negociação. Pode ser só texto, só uma nova oferta de
 * valor (contraproposta) ou os dois — mas pelo menos um deles é obrigatório.
 * O remetente é sempre o usuário do token.
 */
public record MensagemRequest(
        @NotNull(message = "ID da negociação é obrigatório")
        UUID negociacaoId,

        @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
        String mensagem,

        @DecimalMin(value = "0.01", message = "Valor ofertado deve ser maior que zero")
        BigDecimal valorOfertado
) {}
