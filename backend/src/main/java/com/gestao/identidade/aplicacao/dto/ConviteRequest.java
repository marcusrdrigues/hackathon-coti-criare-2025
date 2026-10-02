package com.gestao.identidade.aplicacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Quem o proprietário quer na equipe. */
public record ConviteRequest(
        @NotBlank(message = "Informe o nome da pessoa")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "Informe o e-mail da pessoa")
        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email
) {}
