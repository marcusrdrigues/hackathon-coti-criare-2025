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
        /* A pessoa que escreveu */
        UUID remetenteId,
        /* A organização dela (empresa ou fornecedor) */
        String remetenteNome,
        /* O nome da pessoa */
        String remetentePessoa,
        LocalDateTime dataEnvio,
        UUID negociacaoId
) {}
