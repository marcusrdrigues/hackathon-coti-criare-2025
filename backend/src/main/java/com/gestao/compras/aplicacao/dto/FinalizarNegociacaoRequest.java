package com.gestao.compras.aplicacao.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinalizarNegociacaoRequest(
        @NotNull(message = "Valor final é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valorFinal
) {}