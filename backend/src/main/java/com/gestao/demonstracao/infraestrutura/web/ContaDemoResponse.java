package com.gestao.demonstracao.infraestrutura.web;

import com.gestao.identidade.dominio.TipoUsuario;

/** Conta de demonstração oferecida na tela de login (sem expor a senha). */
public record ContaDemoResponse(
        TipoUsuario perfil,
        String nome,
        String descricao
) {}
