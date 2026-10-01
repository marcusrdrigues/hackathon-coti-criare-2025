package com.gestao.dtos.auth;

import com.gestao.enums.TipoUsuario;

/** Conta de demonstração oferecida na tela de login (sem expor a senha). */
public record ContaDemoResponse(
        TipoUsuario perfil,
        String nome,
        String descricao
) {}
