package com.gestao.controllers;

import com.gestao.dtos.cotacao.CotacaoRequest;
import com.gestao.dtos.cotacao.CotacaoResponse;
import com.gestao.entities.Cotacao;
import com.gestao.dtos.cotacao.CategoriaResponse;
import com.gestao.enums.CategoriaCotacao;
import com.gestao.mappers.CotacaoMapper;
import com.gestao.services.CotacaoService;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cotacoes")
@RequiredArgsConstructor
@Tag(name = "Cotações", description = "Endpoints para gerenciamento de cotações")
public class CotacaoController {

    private final CotacaoService cotacaoService;
    private final CotacaoMapper cotacaoMapper;

    @Operation(summary = "Criar cotação", description = "Cria uma nova cotação de serviço")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cotação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @PostMapping
    public ResponseEntity<CotacaoResponse> criarCotacao(@Valid @RequestBody CotacaoRequest request) {
        Cotacao cotacao = cotacaoMapper.toEntity(request);
        Cotacao novaCotacao = cotacaoService.criarCotacao(cotacao, request.empresaId());
        CotacaoResponse response = cotacaoMapper.toResponse(novaCotacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar todas as cotações", description = "Retorna uma lista com todas as cotações")
    @ApiResponse(responseCode = "200", description = "Lista de cotações retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<CotacaoResponse>> listarTodas() {
        List<Cotacao> cotacoes = cotacaoService.listarTodas();
        List<CotacaoResponse> responses = cotacoes.stream()
                .map(cotacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Listar categorias", description = "Categorias disponíveis para classificar uma cotação")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        List<CategoriaResponse> categorias = Arrays.stream(CategoriaCotacao.values())
                .map(c -> new CategoriaResponse(c.name(), c.getDescricao()))
                .toList();
        return ResponseEntity.ok(categorias);
    }

    @Operation(summary = "Listar cotações abertas", description = "Retorna todas as cotações com status ABERTA")
    @ApiResponse(responseCode = "200", description = "Lista de cotações abertas retornada com sucesso")
    @GetMapping("/abertas")
    public ResponseEntity<List<CotacaoResponse>> listarCotacoesAbertas() {
        List<Cotacao> cotacoes = cotacaoService.listarCotacoesAbertas();
        List<CotacaoResponse> responses = cotacoes.stream()
                .map(cotacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Buscar cotação por ID", description = "Retorna os dados de uma cotação específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cotação encontrada"),
            @ApiResponse(responseCode = "404", description = "Cotação não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CotacaoResponse> buscarPorId(
            @Parameter(description = "ID da cotação") @PathVariable UUID id) {
        Cotacao cotacao = cotacaoService.buscarPorId(id);
        CotacaoResponse response = cotacaoMapper.toResponse(cotacao);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar cotações por empresa", description = "Retorna todas as cotações de uma empresa específica")
    @ApiResponse(responseCode = "200", description = "Lista de cotações retornada com sucesso")
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<CotacaoResponse>> listarPorEmpresa(
            @Parameter(description = "ID da empresa") @PathVariable UUID empresaId) {
        List<Cotacao> cotacoes = cotacaoService.listarPorEmpresa(empresaId);
        List<CotacaoResponse> responses = cotacoes.stream()
                .map(cotacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Atualizar cotação", description = "Atualiza os dados de uma cotação existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cotação atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cotação não encontrada"),
            @ApiResponse(responseCode = "400", description = "Cotação não pode ser editada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CotacaoResponse> atualizarCotacao(
            @Parameter(description = "ID da cotação") @PathVariable UUID id,
            @Valid @RequestBody CotacaoRequest request) {
        Cotacao cotacao = cotacaoMapper.toEntity(request);
        Cotacao cotacaoAtualizada = cotacaoService.atualizarCotacao(id, cotacao);
        CotacaoResponse response = cotacaoMapper.toResponse(cotacaoAtualizada);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar cotação", description = "Cancela uma cotação existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cotação cancelada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cotação não encontrada")
    })
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CotacaoResponse> cancelarCotacao(
            @Parameter(description = "ID da cotação") @PathVariable UUID id) {
        Cotacao cotacao = cotacaoService.cancelarCotacao(id);
        CotacaoResponse response = cotacaoMapper.toResponse(cotacao);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deletar cotação", description = "Remove uma cotação do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cotação deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cotação não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCotacao(
            @Parameter(description = "ID da cotação") @PathVariable UUID id) {
        cotacaoService.deletarCotacao(id);
        return ResponseEntity.noContent().build();
    }

}