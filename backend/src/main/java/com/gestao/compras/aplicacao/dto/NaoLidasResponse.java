package com.gestao.compras.aplicacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mensagens da outra parte que a organização ainda não viu, para o contador da navegação. */
public record NaoLidasResponse(
        @Schema(description = "Soma de todas as negociações", example = "3") long total,
        @Schema(description = "Id da negociação -> não lidas (só as que têm alguma)") Map<UUID, Integer> porNegociacao) {

    public static NaoLidasResponse de(Map<UUID, Integer> porNegociacao) {
        Map<UUID, Integer> comAlguma = new HashMap<>();
        porNegociacao.forEach((id, total) -> {
            if (total != null && total > 0) {
                comAlguma.put(id, total);
            }
        });
        long soma = comAlguma.values().stream().mapToLong(Integer::longValue).sum();
        return new NaoLidasResponse(soma, Map.copyOf(comAlguma));
    }
}
