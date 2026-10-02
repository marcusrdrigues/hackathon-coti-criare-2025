package com.gestao.identidade.aplicacao.dto;

import com.gestao.identidade.dominio.Convite;
import com.gestao.identidade.dominio.TipoOrganizacao;

import java.time.Instant;

/** O que a tela de aceitar convite mostra antes de a pessoa definir a senha. */
public record ConviteAbertoResponse(String organizacao, TipoOrganizacao tipo, String convidadoPor, String nome,
                                    String email, Instant expiraEm) {

    public static ConviteAbertoResponse de(Convite convite) {
        return new ConviteAbertoResponse(convite.getOrganizacao().getRazaoSocial(), convite.getOrganizacao().getTipo(),
                convite.getConvidadoPor().getNome(), convite.getNome(), convite.getEmail(), convite.getExpiraEm());
    }
}
