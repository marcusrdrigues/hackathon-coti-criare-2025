package com.gestao.demonstracao.infraestrutura.web;

import com.gestao.identidade.dominio.TipoOrganizacao;

/** Conta de demonstração oferecida na tela de login (sem expor a senha). */
public record ContaDemoResponse(
        TipoOrganizacao perfil,
        String nome,
        String descricao
) {}
