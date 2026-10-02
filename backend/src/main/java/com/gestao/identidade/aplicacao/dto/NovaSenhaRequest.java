package com.gestao.identidade.aplicacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A senha nova, com as mesmas regras do cadastro, e o token do link. */
public record NovaSenhaRequest(
        @NotBlank(message = "Link sem token")
        @Size(max = 100, message = "Token inválido")
        String token,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Senha deve ter letras e números")
        String senha
) {}
