package com.gestao.identidade.aplicacao.dto;

import com.gestao.identidade.dominio.Convite;

import java.time.Instant;
import java.util.UUID;

/**
 * Um convite pendente. O {@code token} só vem na resposta de quem acabou de criar o convite:
 * é a única vez em que ele existe fora do link, porque o banco guarda só o hash.
 */
public record ConviteResponse(UUID id, String nome, String email, String convidadoPor, Instant expiraEm,
                              String token) {

    public static ConviteResponse de(Convite convite) {
        return comToken(convite, null);
    }

    public static ConviteResponse comToken(Convite convite, String token) {
        return new ConviteResponse(convite.getId(), convite.getNome(), convite.getEmail(),
                convite.getConvidadoPor().getNome(), convite.getExpiraEm(), token);
    }
}
