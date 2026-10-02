package com.gestao.painel.infraestrutura.web;

import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import com.gestao.painel.aplicacao.PainelService;
import com.gestao.painel.aplicacao.dto.PainelEmpresaResponse;
import com.gestao.painel.aplicacao.dto.PainelFornecedorResponse;
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
public class PainelController {

    private final PainelService dashboardService;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Painel da empresa",
            description = "Perfil EMPRESA. Cotações por status, propostas recebidas, demandas por categoria e top fornecedores")
    @PreAuthorize("hasRole('EMPRESA')")
    @GetMapping("/empresa")
    public ResponseEntity<PainelEmpresaResponse> empresa() {
        return ResponseEntity.ok(dashboardService.resumoEmpresa(usuarioAtual.obter().organizacaoId()));
    }

    @Operation(summary = "Painel do fornecedor",
            description = "Perfil FORNECEDOR. Oportunidades abertas, propostas pendentes, negociações e cotações ganhas")
    @PreAuthorize("hasRole('FORNECEDOR')")
    @GetMapping("/fornecedor")
    public ResponseEntity<PainelFornecedorResponse> fornecedor() {
        return ResponseEntity.ok(dashboardService.resumoFornecedor(usuarioAtual.obter().organizacaoId()));
    }
}
