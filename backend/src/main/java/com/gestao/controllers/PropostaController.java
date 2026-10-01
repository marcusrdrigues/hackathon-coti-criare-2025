package com.gestao.controllers;

import com.gestao.dtos.proposta.PropostaRequest;
import com.gestao.dtos.proposta.PropostaResponse;
import com.gestao.entities.Proposta;
import com.gestao.enums.StatusProposta;
import com.gestao.mappers.PropostaMapper;
import com.gestao.services.PropostaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/propostas")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;
    private final PropostaMapper propostaMapper;

    // POST /api/propostas - Criar proposta
    @PostMapping
    public ResponseEntity<PropostaResponse> criarProposta(@Valid @RequestBody PropostaRequest request) {
        Proposta proposta = propostaMapper.toEntity(request);
        Proposta novaProposta = propostaService.criarProposta(
                proposta,
                request.fornecedorId(),
                request.cotacaoId()
        );
        PropostaResponse response = propostaMapper.toResponse(novaProposta);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/propostas/{id} - Buscar proposta por ID
    @GetMapping("/{id}")
    public ResponseEntity<PropostaResponse> buscarPorId(@PathVariable UUID id) {
        Proposta proposta = propostaService.buscarPorId(id);
        PropostaResponse response = propostaMapper.toResponse(proposta);
        return ResponseEntity.ok(response);
    }

    // GET /api/propostas/cotacao/{cotacaoId} - Listar propostas de uma cotação
    @GetMapping("/cotacao/{cotacaoId}")
    public ResponseEntity<List<PropostaResponse>> listarPorCotacao(@PathVariable UUID cotacaoId) {
        List<Proposta> propostas = propostaService.listarPorCotacao(cotacaoId);
        List<PropostaResponse> responses = propostas.stream()
                .map(propostaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/propostas/fornecedor/{fornecedorId} - Listar propostas de um fornecedor
    @GetMapping("/fornecedor/{fornecedorId}")
    public ResponseEntity<List<PropostaResponse>> listarPorFornecedor(@PathVariable UUID fornecedorId) {
        List<Proposta> propostas = propostaService.listarPorFornecedor(fornecedorId);
        List<PropostaResponse> responses = propostas.stream()
                .map(propostaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/propostas/status/{status} - Listar propostas por status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PropostaResponse>> listarPorStatus(@PathVariable StatusProposta status) {
        List<Proposta> propostas = propostaService.listarPorStatus(status);
        List<PropostaResponse> responses = propostas.stream()
                .map(propostaMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/propostas/cotacao/{cotacaoId}/count - Contar propostas de uma cotação
    @GetMapping("/cotacao/{cotacaoId}/count")
    public ResponseEntity<Long> contarPropostasPorCotacao(@PathVariable UUID cotacaoId) {
        long count = propostaService.contarPropostasPorCotacao(cotacaoId);
        return ResponseEntity.ok(count);
    }

    // PUT /api/propostas/{id} - Atualizar proposta
    @PutMapping("/{id}")
    public ResponseEntity<PropostaResponse> atualizarProposta(
            @PathVariable UUID id,
            @Valid @RequestBody PropostaRequest request) {
        Proposta proposta = propostaMapper.toEntity(request);
        Proposta propostaAtualizada = propostaService.atualizarProposta(id, request.fornecedorId(), proposta);
        PropostaResponse response = propostaMapper.toResponse(propostaAtualizada);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/propostas/{id}/aceitar - Aceitar proposta
    @PatchMapping("/{id}/aceitar")
    public ResponseEntity<PropostaResponse> aceitarProposta(@PathVariable UUID id) {
        Proposta proposta = propostaService.aceitarProposta(id);
        PropostaResponse response = propostaMapper.toResponse(proposta);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/propostas/{id}/recusar - Recusar proposta
    @PatchMapping("/{id}/recusar")
    public ResponseEntity<PropostaResponse> recusarProposta(@PathVariable UUID id) {
        Proposta proposta = propostaService.recusarProposta(id);
        PropostaResponse response = propostaMapper.toResponse(proposta);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/propostas/{id}/status - Atualizar status da proposta
    @PatchMapping("/{id}/status")
    public ResponseEntity<PropostaResponse> atualizarStatus(
            @PathVariable UUID id,
            @RequestBody StatusRequest statusRequest) {
        Proposta proposta = propostaService.atualizarStatus(id, statusRequest.status());
        PropostaResponse response = propostaMapper.toResponse(proposta);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/propostas/{id} - Deletar proposta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarProposta(@PathVariable UUID id) {
        propostaService.deletarProposta(id);
        return ResponseEntity.noContent().build();
    }

    // Record auxiliar
    public record StatusRequest(StatusProposta status) {}
}