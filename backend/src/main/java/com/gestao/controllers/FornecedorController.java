package com.gestao.controllers;


import com.gestao.dtos.fornecedor.FornecedorCadastroRequest;
import com.gestao.dtos.fornecedor.FornecedorResponse;
import com.gestao.dtos.fornecedor.FornecedorUpdateRequest;
import com.gestao.entities.Fornecedor;
import com.gestao.mappers.FornecedorMapper;
import com.gestao.services.FornecedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fornecedores")
@RequiredArgsConstructor
public class FornecedorController {

    private final FornecedorService fornecedorService;
    private final FornecedorMapper fornecedorMapper;

    // POST /api/fornecedores - Cadastrar fornecedor
    @PostMapping
    public ResponseEntity<FornecedorResponse> cadastrarFornecedor(@Valid @RequestBody FornecedorCadastroRequest request) {
        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        Fornecedor novoFornecedor = fornecedorService.cadastrarFornecedor(fornecedor);
        FornecedorResponse response = fornecedorMapper.toResponse(novoFornecedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/fornecedores - Listar todos os fornecedores
    @GetMapping
    public ResponseEntity<List<FornecedorResponse>> listarTodos() {
        List<Fornecedor> fornecedores = fornecedorService.listarTodos();
        List<FornecedorResponse> responses = fornecedores.stream()
                .map(fornecedorMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/fornecedores/{id} - Buscar fornecedor por ID
    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponse> buscarPorId(@PathVariable UUID id) {
        Fornecedor fornecedor = fornecedorService.buscarPorId(id);
        FornecedorResponse response = fornecedorMapper.toResponse(fornecedor);
        return ResponseEntity.ok(response);
    }

    // GET /api/fornecedores/email/{email} - Buscar fornecedor por email
    @GetMapping("/email/{email}")
    public ResponseEntity<FornecedorResponse> buscarPorEmail(@PathVariable String email) {
        Fornecedor fornecedor = fornecedorService.buscarPorEmail(email);
        FornecedorResponse response = fornecedorMapper.toResponse(fornecedor);
        return ResponseEntity.ok(response);
    }

    // PUT /api/fornecedores/{id} - Atualizar fornecedor
    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponse> atualizarFornecedor(
            @PathVariable UUID id,
            @Valid @RequestBody FornecedorUpdateRequest request) {
        Fornecedor fornecedorAtualizado = fornecedorService.atualizarFornecedor(id, request.nomeCompleto());
        FornecedorResponse response = fornecedorMapper.toResponse(fornecedorAtualizado);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/fornecedores/{id} - Deletar fornecedor
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarFornecedor(@PathVariable UUID id) {
        fornecedorService.deletarFornecedor(id);
        return ResponseEntity.noContent().build();
    }
}