package com.gestao.dtos.dashboard;

import java.util.UUID;

public record FornecedorResumoResponse(
        UUID id,
        String nome,
        String email,
        long totalPropostas
) {}
