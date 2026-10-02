package com.gestao.identidade.aplicacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O token do link de convite vai no corpo, e nunca no caminho da URL: assim ele não
 * aparece nos logs de acesso do servidor nem dos proxies.
 */
public record TokenDoConviteRequest(
        @NotBlank(message = "Convite sem token")
        @Size(max = 100, message = "Token inválido")
        String token
) {}
