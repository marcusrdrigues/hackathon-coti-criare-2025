package com.gestao.identidade.aplicacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Dados do perfil")
public record PerfilResponse(
        @Schema(description = "ID do perfil", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Nome do perfil", example = "EMPRESA")
        String nome
) {

}