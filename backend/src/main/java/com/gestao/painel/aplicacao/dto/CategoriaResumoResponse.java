package com.gestao.painel.aplicacao.dto;

public record CategoriaResumoResponse(
        String categoria,
        String descricao,
        long total,
        int percentual
) {}
