package com.gestao.dtos.empresa;

import jakarta.validation.constraints.Size;

public record EmpresaUpdateRequest(
        @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
        String razaoSocial
) {}