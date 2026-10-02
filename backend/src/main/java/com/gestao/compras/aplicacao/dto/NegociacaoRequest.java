package com.gestao.compras.aplicacao.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NegociacaoRequest(
        @NotNull(message = "ID da proposta é obrigatório")
        UUID propostaId
) {}