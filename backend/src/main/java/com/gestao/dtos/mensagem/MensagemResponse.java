package com.gestao.dtos.mensagem;

import com.gestao.enums.TipoRemetente;  // ← MUDOU
import java.time.LocalDate;
import java.util.UUID;

public record MensagemResponse(
        UUID id,
        String mensagem,
        TipoRemetente tipoRemetente,
        UUID remetenteId,
        String remetenteNome,
        LocalDate dataEnvio,
        UUID negociacaoId
) {}