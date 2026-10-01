package com.gestao.dtos.fornecedor;

import java.util.UUID;

public record FornecedorResponse(
        UUID id,
        String nomeCompleto,
        String cnpj,
        String email,
        String perfilNome
) {}