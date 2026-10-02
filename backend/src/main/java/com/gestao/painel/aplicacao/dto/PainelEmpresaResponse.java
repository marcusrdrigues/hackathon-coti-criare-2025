package com.gestao.painel.aplicacao.dto;

import java.util.List;

public record PainelEmpresaResponse(
        long cotacoesAbertas,
        long cotacoesEmNegociacao,
        long cotacoesFechadas,
        long propostasRecebidas,
        List<CategoriaResumoResponse> categorias,
        List<FornecedorResumoResponse> topFornecedores
) {}
