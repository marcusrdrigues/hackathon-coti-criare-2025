package com.gestao.compartilhado.aplicacao;

import com.gestao.compartilhado.dominio.RegraDeNegocioException;

import java.util.Objects;

/**
 * Que pedaço de uma lista pedir: a página (a partir de 0), o tamanho e a ordem.
 * Tipo próprio porque a aplicação não conhece o Spring Data (regra do ArquiteturaTest);
 * a web e a persistência convertem de e para o Spring.
 *
 * @param numero  página pedida, a partir de 0
 * @param tamanho itens por página, de 1 a {@value #TAMANHO_MAXIMO} (acima disso, vale o máximo)
 * @param ordem   campo e direção, já conferidos contra a lista de campos aceitos do endpoint
 */
public record PedidoDePagina(int numero, int tamanho, Ordem ordem) {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 50;

    /** Campo de ordenação (o nome exposto na API) e a direção. */
    public record Ordem(String campo, boolean crescente) {

        public Ordem {
            Objects.requireNonNull(campo, "campo");
        }
    }

    public PedidoDePagina {
        if (numero < 0) {
            throw new RegraDeNegocioException("A primeira página é a 0.");
        }
        if (tamanho < 1) {
            throw new RegraDeNegocioException("O tamanho da página precisa ser de pelo menos 1.");
        }
        tamanho = Math.min(tamanho, TAMANHO_MAXIMO);
        Objects.requireNonNull(ordem, "ordem");
    }

    /** Quantos itens pular até o começo desta página. */
    public long deslocamento() {
        return (long) numero * tamanho;
    }
}
