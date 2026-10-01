package com.gestao.dtos.empresa;

import java.util.UUID;

public record EmpresaResponse(
        UUID id,
        String razaoSocial,
        String cnpj,
        String email,
        String perfilNome
) {}