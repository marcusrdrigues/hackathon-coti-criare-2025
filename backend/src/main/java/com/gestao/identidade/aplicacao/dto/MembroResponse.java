package com.gestao.identidade.aplicacao.dto;

import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Papel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma pessoa da equipe. O {@code id} é o do vínculo (é ele que se remove);
 * {@code voce} marca a própria pessoa que pediu a lista.
 */
public record MembroResponse(UUID id, String nome, String email, Papel papel, LocalDateTime desde, boolean voce) {

    public static MembroResponse de(Membro membro, UUID usuarioAtual) {
        return new MembroResponse(membro.getId(), membro.getUsuario().getNome(), membro.getUsuario().getEmail(),
                membro.getPapel(), membro.getCriadoEm(), membro.getUsuario().getId().equals(usuarioAtual));
    }
}
