package com.gestao.identidade.aplicacao.dto;

import com.gestao.identidade.dominio.TipoOrganizacao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro público: a organização (empresa ou fornecedor) e a pessoa que vai ser a proprietária. */
public record CadastroRequest(
        @NotNull(message = "Informe se é empresa ou fornecedor")
        TipoOrganizacao tipo,

        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
        String razaoSocial,

        @NotBlank(message = "CNPJ é obrigatório")
        @Size(min = 14, max = 18, message = "CNPJ deve ter 14 dígitos")
        String cnpj,

        @NotBlank(message = "Seu nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Senha deve ter letras e números")
        String senha
) {}
