package com.gestao.controllers;

import com.gestao.dtos.empresa.EmpresaCadastroRequest;
import com.gestao.dtos.empresa.EmpresaResponse;
import com.gestao.dtos.empresa.EmpresaUpdateRequest;
import com.gestao.mappers.EmpresaMapper;
import com.gestao.security.UsuarioAtual;
import com.gestao.services.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Cadastro e perfil de empresas compradoras")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final EmpresaMapper empresaMapper;
    private final UsuarioAtual usuarioAtual;

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

    @Operation(summary = "Buscar empresa por ID")
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> buscarPorId(@Parameter(description = "ID da empresa") @PathVariable UUID id) {
        return ResponseEntity.ok(empresaMapper.toResponse(empresaService.buscarPorId(id)));
    }

    @Operation(summary = "Atualizar empresa", description = "Só a própria empresa pode alterar o cadastro")
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponse> atualizarEmpresa(
            @Parameter(description = "ID da empresa") @PathVariable UUID id,
            @Valid @RequestBody EmpresaUpdateRequest request) {
        usuarioAtual.exigirMesmoUsuario(id);
        return ResponseEntity.ok(empresaMapper.toResponse(empresaService.atualizarEmpresa(id, request.razaoSocial())));
    }

    @Operation(summary = "Excluir empresa", description = "Só a própria empresa pode excluir a conta")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEmpresa(@Parameter(description = "ID da empresa") @PathVariable UUID id) {
        usuarioAtual.exigirMesmoUsuario(id);
        empresaService.deletarEmpresa(id);
        return ResponseEntity.noContent().build();
    }
}
