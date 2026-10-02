package com.gestao.compartilhado.aplicacao;

import java.util.List;
import java.util.function.Function;

/**
 * Um pedaço de uma lista e o total de itens dela.
 *
 * @param itens   os itens desta página
 * @param numero  a página, a partir de 0
 * @param tamanho o tamanho pedido (a última página pode vir com menos itens)
 * @param total   quantos itens a lista inteira tem
 */
public record Pagina<T>(List<T> itens, int numero, int tamanho, long total) {

    public Pagina {
        itens = List.copyOf(itens);
    }

    public static <T> Pagina<T> de(List<T> itens, PedidoDePagina pedido, long total) {
        return new Pagina<>(itens, pedido.numero(), pedido.tamanho(), total);
    }

    public <R> Pagina<R> map(Function<? super T, ? extends R> conversao) {
        return new Pagina<>(itens.stream().<R>map(conversao).toList(), numero, tamanho, total);
    }

    public int totalDePaginas() {
        return tamanho == 0 ? 0 : (int) Math.ceil((double) total / tamanho);
    }
}
