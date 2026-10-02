package com.gestao.compartilhado.infraestrutura.persistencia;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Converte a paginação da aplicação de e para o Spring Data, nos adaptadores de persistência
 * (a aplicação não conhece o Spring Data; ver docs/adr/0020).
 */
public final class PaginasJpa {

    /** Caractere de escape dos padrões de LIKE gerados por {@link #contendo(String)}. */
    public static final char ESCAPE = '\\';

    private PaginasJpa() {
    }

    /**
     * A página pedida, ordenada pela propriedade da entidade que corresponde ao campo da API,
     * com o id como desempate (a mesma ordem em todas as páginas).
     *
     * @param propriedades campo da API -> propriedade da entidade; só o que está aqui ordena
     */
    public static PageRequest ordenada(PedidoDePagina pedido, Map<String, String> propriedades) {
        String propriedade = Objects.requireNonNull(propriedades.get(pedido.ordem().campo()), "ordem não permitida");
        Sort.Direction direcao = pedido.ordem().crescente() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pedido.numero(), pedido.tamanho(), Sort.by(direcao, propriedade).and(Sort.by("id")));
    }

    /** A página pedida, para consultas que já trazem a própria ordem. */
    public static PageRequest naOrdemDaConsulta(PedidoDePagina pedido) {
        return PageRequest.of(pedido.numero(), pedido.tamanho());
    }

    public static <T> Pagina<T> pagina(Page<T> pagina, PedidoDePagina pedido) {
        return Pagina.de(pagina.getContent(), pedido, pagina.getTotalElements());
    }

    /**
     * Padrão de LIKE para "contém o texto", em minúsculas e com {@code %} e {@code _} do
     * texto tratados como caracteres comuns (use com {@link #ESCAPE}).
     */
    public static String contendo(String texto) {
        String escapado = texto.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
