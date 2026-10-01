package com.gestao.dtos.fornecedor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorCadastroRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nomeCompleto,

        @NotBlank(message = "CNPJ é obrigatório")
        @Size(min = 14, max = 18, message = "CNPJ deve ter 14 dígitos")
        String cnpj,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres")
        String senha
) {}
