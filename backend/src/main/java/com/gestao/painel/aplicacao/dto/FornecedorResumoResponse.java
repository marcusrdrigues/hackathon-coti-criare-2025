package com.gestao.painel.aplicacao.dto;

import java.util.UUID;

public record FornecedorResumoResponse(
        UUID id,
        String nome,
        String email,
        long totalPropostas
) {}
