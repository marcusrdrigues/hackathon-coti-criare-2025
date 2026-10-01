package com.gestao.dtos.auth;

import java.util.UUID;

/**
 * Dados do usuário autenticado. O campo {@code tipo} ("EMPRESA" ou "FORNECEDOR")
 * define qual área do front-end ele acessa.
 */
public record LoginResponse(
        UUID id,
        String nome,
        String email,
        String cnpj,
        String tipo
) {}
