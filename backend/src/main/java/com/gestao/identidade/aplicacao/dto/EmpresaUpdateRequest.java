package com.gestao.identidade.aplicacao.dto;

import jakarta.validation.constraints.Size;

public record EmpresaUpdateRequest(
        @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
        String razaoSocial
) {}