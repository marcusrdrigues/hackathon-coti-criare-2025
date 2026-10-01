package com.gestao.controllers;

import com.gestao.dtos.perfil.PerfilResponse;
import com.gestao.entities.Perfil;
import com.gestao.mappers.PerfilMapper;
import com.gestao.services.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/perfis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Perfis", description = "Endpoints para gerenciamento de perfis")
public class PerfilController {

    private final PerfilService perfilService;
    private final PerfilMapper perfilMapper;

    @Operation(summary = "Listar todos os perfis", description = "Retorna uma lista com todos os perfis disponíveis (EMPRESA e FORNECEDOR)")
    @ApiResponse(responseCode = "200", description = "Lista de perfis retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<PerfilResponse>> listarTodos() {
        List<Perfil> perfis = perfilService.listarTodos();
        List<PerfilResponse> responses = perfis.stream()
                .map(perfilMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Buscar perfil por ID", description = "Retorna os dados de um perfil específico")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponse> buscarPorId(@PathVariable UUID id) {
        Perfil perfil = perfilService.buscarPorId(id);
        PerfilResponse response = perfilMapper.toResponse(perfil);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar perfil por nome", description = "Retorna um perfil específico pelo nome (EMPRESA ou FORNECEDOR)")
    @ApiResponse(responseCode = "200", description = "Perfil encontrado")
    @GetMapping("/nome/{nome}")
    public ResponseEntity<PerfilResponse> buscarPorNome(@PathVariable String nome) {
        Perfil perfil = perfilService.buscarPorNome(nome);
        PerfilResponse response = perfilMapper.toResponse(perfil);
        return ResponseEntity.ok(response);
    }
}