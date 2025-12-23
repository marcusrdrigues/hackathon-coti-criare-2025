package com.gestao.dtos.cotacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CotacaoRequest(
        @NotBlank(message = "Nome do serviço é obrigatório")
        @Size(max = 200, message = "Nome do serviço deve ter no máximo 200 caracteres")
        String nomeServico,

        @NotBlank(message = "Requisitos são obrigatórios")
        @Size(max = 1000, message = "Requisitos devem ter no máximo 1000 caracteres")
        String requisitos,

        LocalDateTime dataLimite,

        @NotNull(message = "ID da empresa é obrigatório")
        UUID empresaId
) {}