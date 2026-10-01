package com.gestao.dtos.mensagem;

import com.gestao.enums.TipoRemetente;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mensagem dentro de uma negociação. Pode ser só texto, só uma nova oferta de
 * valor (contraproposta) ou os dois — mas pelo menos um deles é obrigatório.
 */
public record MensagemRequest(
        @NotNull(message = "ID da negociação é obrigatório")
        UUID negociacaoId,

        @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
        String mensagem,

        @DecimalMin(value = "0.01", message = "Valor ofertado deve ser maior que zero")
        BigDecimal valorOfertado,

        @NotNull(message = "Tipo de remetente é obrigatório")
        TipoRemetente tipoRemetente,

        @NotNull(message = "ID do remetente é obrigatório")
        UUID remetenteId
) {}
