package com.gestao.compartilhado.aplicacao;

/** Texto de busca vindo do cliente, pronto para filtrar uma lista. */
public final class Busca {

    /** Mais que isso não ajuda a achar nada e só pesa na consulta. */
    public static final int TAMANHO_MAXIMO = 100;

    private Busca() {
    }

    /** Sem espaços nas pontas e com no máximo {@value #TAMANHO_MAXIMO} caracteres; vazio vira {@code null} (sem filtro). */
    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.strip();
        return limpo.length() > TAMANHO_MAXIMO ? limpo.substring(0, TAMANHO_MAXIMO) : limpo;
    }
}
