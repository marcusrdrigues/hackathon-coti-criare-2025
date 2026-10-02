package com.gestao.compartilhado.infraestrutura.web;

import com.gestao.compartilhado.aplicacao.PedidoDePagina.Ordem;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;

import java.util.Locale;
import java.util.Set;

/**
 * Lê os parâmetros de paginação de sempre do Spring ({@code page}, {@code size} e
 * {@code sort=campo,asc|desc}) e confere a ordenação contra os campos que o endpoint aceita.
 * Ordenar por um campo fora da lista responde 400: ninguém ordena por coluna interna.
 */
public final class Paginacao {

    private Paginacao() {
    }

    /**
     * @param campos campos aceitos para ordenação, com os nomes expostos na API
     * @param padrao a ordem quando o cliente não informa {@code sort}
     */
    public static PedidoDePagina pedido(int page, int size, String sort, Set<String> campos, Ordem padrao) {
        return new PedidoDePagina(page, size, ordem(sort, campos, padrao));
    }

    private static Ordem ordem(String sort, Set<String> campos, Ordem padrao) {
        if (sort == null || sort.isBlank()) {
            return padrao;
        }
        String[] partes = sort.split(",", -1);
        String campo = partes[0].trim();
        if (!campos.contains(campo)) {
            throw new RegraDeNegocioException("Não é possível ordenar por esse campo. Use: "
                    + String.join(", ", campos.stream().sorted().toList()) + ".");
        }
        if (partes.length == 1) {
            return new Ordem(campo, true);
        }
        String direcao = partes[1].trim().toLowerCase(Locale.ROOT);
        if (partes.length > 2 || !(direcao.equals("asc") || direcao.equals("desc"))) {
            throw new RegraDeNegocioException("A direção da ordenação é 'asc' ou 'desc'.");
        }
        return new Ordem(campo, direcao.equals("asc"));
    }
}
