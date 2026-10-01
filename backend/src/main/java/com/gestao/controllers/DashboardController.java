package com.gestao.controllers;

import com.gestao.dtos.dashboard.DashboardEmpresaResponse;
import com.gestao.dtos.dashboard.DashboardFornecedorResponse;
import com.gestao.security.UsuarioAtual;
import com.gestao.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores dos painéis de empresa e fornecedor")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Painel da empresa",
            description = "Perfil EMPRESA. Cotações por status, propostas recebidas, demandas por categoria e top fornecedores")
    @PreAuthorize("hasRole('EMPRESA')")
    @GetMapping("/empresa")
    public ResponseEntity<DashboardEmpresaResponse> empresa() {
        return ResponseEntity.ok(dashboardService.resumoEmpresa(usuarioAtual.obter().id()));
    }

    @Operation(summary = "Painel do fornecedor",
            description = "Perfil FORNECEDOR. Oportunidades abertas, propostas pendentes, negociações e cotações ganhas")
    @PreAuthorize("hasRole('FORNECEDOR')")
    @GetMapping("/fornecedor")
    public ResponseEntity<DashboardFornecedorResponse> fornecedor() {
        return ResponseEntity.ok(dashboardService.resumoFornecedor(usuarioAtual.obter().id()));
    }
}
