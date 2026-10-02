package com.gestao.identidade.aplicacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Aceitar o convite: a pessoa confirma o nome e define a senha. O e-mail é o do convite. */
public record AceiteConviteRequest(
        @NotBlank(message = "Convite sem token")
        @Size(max = 100, message = "Token inválido")
        String token,

        @NotBlank(message = "Seu nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Senha deve ter letras e números")
        String senha
) {}
