package com.gestao.dtos.dashboard;

public record CategoriaResumoResponse(
        String categoria,
        String descricao,
        long total,
        int percentual
) {}
