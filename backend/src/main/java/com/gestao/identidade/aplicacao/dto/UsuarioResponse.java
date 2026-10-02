package com.gestao.identidade.aplicacao.dto;

import com.gestao.identidade.dominio.TipoUsuario;

import java.util.UUID;

/** Dados do usuário autenticado. O {@code tipo} define a área do front-end que ele acessa. */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String cnpj,
        TipoUsuario tipo
) {}
