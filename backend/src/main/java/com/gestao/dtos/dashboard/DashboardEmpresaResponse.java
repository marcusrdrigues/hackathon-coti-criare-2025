package com.gestao.dtos.dashboard;

import java.util.List;

public record DashboardEmpresaResponse(
        long cotacoesAbertas,
        long cotacoesEmNegociacao,
        long cotacoesFechadas,
        long propostasRecebidas,
        List<CategoriaResumoResponse> categorias,
        List<FornecedorResumoResponse> topFornecedores
) {}
