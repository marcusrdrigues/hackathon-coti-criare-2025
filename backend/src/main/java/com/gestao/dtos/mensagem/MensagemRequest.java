package com.gestao.dtos.mensagem;

import com.gestao.enums.TipoRemetente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MensagemRequest(
        @NotNull(message = "ID da negociação é obrigatório")
        UUID negociacaoId,

        @NotBlank(message = "Mensagem é obrigatória")
        @Size(max = 1000, message = "Mensagem deve ter no máximo 1000 caracteres")
        String mensagem,

        @NotNull(message = "Tipo de remetente é obrigatório")
        TipoRemetente tipoRemetente,

        @NotNull(message = "ID do remetente é obrigatório")
        UUID remetenteId
) {}