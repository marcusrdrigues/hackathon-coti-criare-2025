package com.gestao.administracao.infraestrutura.web;

import com.gestao.administracao.aplicacao.AdministracaoService;
import com.gestao.administracao.aplicacao.dto.OrganizacaoResumoResponse;
import com.gestao.administracao.aplicacao.porta.ConsultasDaAdministracao;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compartilhado.infraestrutura.web.PaginaResponse;
import com.gestao.compartilhado.infraestrutura.web.Paginacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Área administrativa: só o superadmin, e só leitura nesta fase. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
@Tag(name = "Administração", description = "Visão do superadmin sobre a plataforma (somente leitura)")
public class AdministracaoController {

    private static final PedidoDePagina.Ordem MAIS_RECENTES = new PedidoDePagina.Ordem("criadaEm", false);

    private final AdministracaoService administracaoService;

    @Operation(summary = "Organizações",
            description = "Superadmin. Organizações com o tipo, a data de entrada e os totais de pessoas ativas, "
                    + "cotações e propostas. Paginada; ordena por razaoSocial ou criadaEm (padrão: mais recentes).")
    @GetMapping("/organizacoes")
    public ResponseEntity<PaginaResponse<OrganizacaoResumoResponse>> organizacoes(
            @Parameter(description = "Página, a partir de 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (máximo 50)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordem, ex.: razaoSocial,asc") @RequestParam(required = false) String sort) {
        PedidoDePagina pedido = Paginacao.pedido(page, size, sort,
                ConsultasDaAdministracao.ORDENS_DAS_ORGANIZACOES, MAIS_RECENTES);
        return ResponseEntity.ok(PaginaResponse.de(administracaoService.organizacoes(pedido)));
    }
}
