package com.gestao.dtos.dashboard;

import java.math.BigDecimal;

public record DashboardFornecedorResponse(
        long oportunidadesAbertas,
        long propostasPendentes,
        long negociacoesAtivas,
        long cotacoesGanhas,
        BigDecimal valorTotalGanho
) {}
