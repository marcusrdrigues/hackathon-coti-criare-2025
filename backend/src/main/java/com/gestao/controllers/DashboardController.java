package com.gestao.controllers;

import com.gestao.dtos.dashboard.DashboardEmpresaResponse;
import com.gestao.dtos.dashboard.DashboardFornecedorResponse;
import com.gestao.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores dos painéis de empresa e fornecedor")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Painel da empresa",
            description = "Totais de cotações por status, propostas recebidas, demandas por categoria e top fornecedores")
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<DashboardEmpresaResponse> empresa(
            @Parameter(description = "ID da empresa") @PathVariable UUID empresaId) {
        return ResponseEntity.ok(dashboardService.resumoEmpresa(empresaId));
    }

    @Operation(summary = "Painel do fornecedor",
            description = "Oportunidades abertas, propostas pendentes, negociações ativas e cotações ganhas")
    @GetMapping("/fornecedor/{fornecedorId}")
    public ResponseEntity<DashboardFornecedorResponse> fornecedor(
            @Parameter(description = "ID do fornecedor") @PathVariable UUID fornecedorId) {
        return ResponseEntity.ok(dashboardService.resumoFornecedor(fornecedorId));
    }
}
