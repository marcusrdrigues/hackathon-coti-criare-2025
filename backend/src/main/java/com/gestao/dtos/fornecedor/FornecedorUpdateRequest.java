package com.gestao.dtos.fornecedor;

import jakarta.validation.constraints.Size;

public record FornecedorUpdateRequest(
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nomeCompleto
) {

}