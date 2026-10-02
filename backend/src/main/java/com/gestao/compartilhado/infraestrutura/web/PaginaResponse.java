package com.gestao.compartilhado.infraestrutura.web;

import com.gestao.compartilhado.aplicacao.Pagina;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Formato das listas paginadas da API: o mesmo do {@code PagedModel} do Spring Data,
 * {@code { content, page: { size, number, totalElements, totalPages } }} (ver docs/adr/0020).
 */
@Schema(description = "Uma página de uma lista")
public record PaginaResponse<T>(
        @Schema(description = "Os itens desta página") List<T> content,
        @Schema(description = "Onde esta página está na lista inteira") Info page) {

    public record Info(
            @Schema(description = "Itens por página", example = "20") int size,
            @Schema(description = "Página atual, a partir de 0", example = "0") int number,
            @Schema(description = "Itens na lista inteira", example = "42") long totalElements,
            @Schema(description = "Total de páginas", example = "3") int totalPages) {
    }

    public static <T> PaginaResponse<T> de(Pagina<T> pagina) {
        return new PaginaResponse<>(pagina.itens(),
                new Info(pagina.tamanho(), pagina.numero(), pagina.total(), pagina.totalDePaginas()));
    }
}
