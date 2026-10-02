package com.gestao.compras.aplicacao.dto;

import com.gestao.compras.dominio.TipoRemetente;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MensagemResponse(
        UUID id,
        String mensagem,
        BigDecimal valorOfertado,
        TipoRemetente tipoRemetente,
        UUID remetenteId,
        String remetenteNome,
        LocalDateTime dataEnvio,
        UUID negociacaoId
) {}
