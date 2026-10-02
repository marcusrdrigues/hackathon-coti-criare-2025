package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.EmpresaMapper;
import com.gestao.identidade.aplicacao.EmpresaService;
import com.gestao.identidade.aplicacao.dto.EmpresaCadastroRequest;
import com.gestao.identidade.aplicacao.dto.EmpresaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Cadastro de empresas compradoras")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final EmpresaMapper empresaMapper;

    @Operation(summary = "Cadastrar empresa", description = "Rota pública. CNPJ validado e senha gravada com BCrypt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empresa cadastrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Email ou CNPJ já cadastrado")
    })
    @PostMapping
    public ResponseEntity<EmpresaResponse> cadastrarEmpresa(@Valid @RequestBody EmpresaCadastroRequest request) {
        var empresa = empresaService.cadastrarEmpresa(empresaMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaMapper.toResponse(empresa));
    }
}
