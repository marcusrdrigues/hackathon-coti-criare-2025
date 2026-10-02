package com.gestao.identidade.aplicacao.dto;

import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;

import java.util.UUID;

/**
 * A pessoa autenticada e a organização em nome de quem ela age. O {@code tipo} define a
 * área do front-end; o {@code papel}, o que ela pode fazer dentro da organização.
 */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        TipoOrganizacao tipo,
        Papel papel,
        OrganizacaoResumo organizacao
) {

    public record OrganizacaoResumo(UUID id, String razaoSocial, String cnpj) {}
}
