package com.gestao.painel.aplicacao.dto;

import java.math.BigDecimal;

public record PainelFornecedorResponse(
        long oportunidadesAbertas,
        long propostasPendentes,
        long negociacoesAtivas,
        long cotacoesGanhas,
        BigDecimal valorTotalGanho
) {}
