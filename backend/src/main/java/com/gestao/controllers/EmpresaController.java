package com.gestao.controllers;

import com.gestao.entities.Empresa;
import com.gestao.dtos.empresa.EmpresaCadastroRequest;
import com.gestao.dtos.empresa.EmpresaResponse;
import com.gestao.dtos.empresa.EmpresaUpdateRequest;
import com.gestao.mappers.EmpresaMapper;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Empresas", description = "Endpoints para gerenciamento de empresas")
public class EmpresaController {

    private final EmpresaService empresaService;
    private final EmpresaMapper empresaMapper;

    @Operation(summary = "Cadastrar nova empresa", description = "Cria uma nova empresa no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empresa cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Email ou CNPJ já cadastrado")
    })
    @PostMapping
    public ResponseEntity<EmpresaResponse> cadastrarEmpresa(@Valid @RequestBody EmpresaCadastroRequest request) {
        Empresa empresa = empresaMapper.toEntity(request);
        Empresa novaEmpresa = empresaService.cadastrarEmpresa(empresa);
        EmpresaResponse response = empresaMapper.toResponse(novaEmpresa);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar todas as empresas", description = "Retorna uma lista com todas as empresas cadastradas")
    @ApiResponse(responseCode = "200", description = "Lista de empresas retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listarTodas() {
        List<Empresa> empresas = empresaService.listarTodas();
        List<EmpresaResponse> responses = empresas.stream()
                .map(empresaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Buscar empresa por ID", description = "Retorna os dados de uma empresa específica pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> buscarPorId(
            @Parameter(description = "ID da empresa") @PathVariable UUID id) {
        Empresa empresa = empresaService.buscarPorId(id);
        EmpresaResponse response = empresaMapper.toResponse(empresa);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar empresa por email", description = "Retorna os dados de uma empresa específica pelo email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<EmpresaResponse> buscarPorEmail(
            @Parameter(description = "Email da empresa") @PathVariable String email) {
        Empresa empresa = empresaService.buscarPorEmail(email);
        EmpresaResponse response = empresaMapper.toResponse(empresa);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Atualizar empresa", description = "Atualiza os dados de uma empresa existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponse> atualizarEmpresa(
            @Parameter(description = "ID da empresa") @PathVariable UUID id,
            @Valid @RequestBody EmpresaUpdateRequest request) {
        Empresa empresa = empresaService.buscarPorId(id);
        if (request.razaoSocial() != null) {
            empresa.setRazaoSocial(request.razaoSocial());
        }
        Empresa empresaAtualizada = empresaService.atualizarEmpresa(id, empresa);
        EmpresaResponse response = empresaMapper.toResponse(empresaAtualizada);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deletar empresa", description = "Remove uma empresa do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Empresa deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEmpresa(
            @Parameter(description = "ID da empresa") @PathVariable UUID id) {
        empresaService.deletarEmpresa(id);
        return ResponseEntity.noContent().build();
    }
}