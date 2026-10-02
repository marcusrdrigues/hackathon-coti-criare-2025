package com.gestao.identidade.aplicacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O token do link de redefinição vai no corpo, nunca na URL, para não ficar em log de acesso. */
public record TokenDeRedefinicaoRequest(
        @NotBlank(message = "Link sem token")
        @Size(max = 100, message = "Token inválido")
        String token
) {}
