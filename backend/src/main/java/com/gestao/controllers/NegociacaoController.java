package com.gestao.controllers;

import com.gestao.dtos.negociacao.FinalizarNegociacaoRequest;
import com.gestao.dtos.negociacao.NegociacaoRequest;
import com.gestao.dtos.negociacao.NegociacaoResponse;
import com.gestao.entities.Negociacao;
import com.gestao.enums.StatusNegociacao;
import com.gestao.mappers.NegociacaoMapper;
import com.gestao.services.NegociacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/negociacoes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NegociacaoController {

    private final NegociacaoService negociacaoService;
    private final NegociacaoMapper negociacaoMapper;

    // POST /api/negociacoes - Criar negociação
    @PostMapping
    public ResponseEntity<NegociacaoResponse> criarNegociacao(@Valid @RequestBody NegociacaoRequest request) {
        Negociacao novaNegociacao = negociacaoService.criarNegociacao(request.propostaId());
        NegociacaoResponse response = negociacaoMapper.toResponse(novaNegociacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/negociacoes - Listar todas as negociações
    @GetMapping
    public ResponseEntity<List<NegociacaoResponse>> listarTodas() {
        List<Negociacao> negociacoes = negociacaoService.listarTodas();
        List<NegociacaoResponse> responses = negociacoes.stream()
                .map(negociacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/negociacoes/{id} - Buscar negociação por ID
    @GetMapping("/{id}")
    public ResponseEntity<NegociacaoResponse> buscarPorId(@PathVariable UUID id) {
        Negociacao negociacao = negociacaoService.buscarPorId(id);
        NegociacaoResponse response = negociacaoMapper.toResponse(negociacao);
        return ResponseEntity.ok(response);
    }

    // GET /api/negociacoes/proposta/{propostaId} - Buscar negociação por proposta
    @GetMapping("/proposta/{propostaId}")
    public ResponseEntity<NegociacaoResponse> buscarPorProposta(@PathVariable UUID propostaId) {
        Negociacao negociacao = negociacaoService.buscarPorProposta(propostaId);
        NegociacaoResponse response = negociacaoMapper.toResponse(negociacao);
        return ResponseEntity.ok(response);
    }

    // GET /api/negociacoes/empresa/{empresaId} - Listar negociações de uma empresa
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<NegociacaoResponse>> listarPorEmpresa(@PathVariable UUID empresaId) {
        List<Negociacao> negociacoes = negociacaoService.listarPorEmpresa(empresaId);
        List<NegociacaoResponse> responses = negociacoes.stream()
                .map(negociacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/negociacoes/fornecedor/{fornecedorId} - Listar negociações de um fornecedor
    @GetMapping("/fornecedor/{fornecedorId}")
    public ResponseEntity<List<NegociacaoResponse>> listarPorFornecedor(@PathVariable UUID fornecedorId) {
        List<Negociacao> negociacoes = negociacaoService.listarPorFornecedor(fornecedorId);
        List<NegociacaoResponse> responses = negociacoes.stream()
                .map(negociacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/negociacoes/status/{status} - Listar negociações por status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NegociacaoResponse>> listarPorStatus(@PathVariable StatusNegociacao status) {
        List<Negociacao> negociacoes = negociacaoService.listarPorStatus(status);
        List<NegociacaoResponse> responses = negociacoes.stream()
                .map(negociacaoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // PATCH /api/negociacoes/{id}/finalizar - Finalizar negociação
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<NegociacaoResponse> finalizarNegociacao(
            @PathVariable UUID id,
            @Valid @RequestBody FinalizarNegociacaoRequest request) {
        Negociacao negociacao = negociacaoService.finalizarNegociacao(id, request.valorFinal());
        NegociacaoResponse response = negociacaoMapper.toResponse(negociacao);
        return ResponseEntity.ok(response);
    }

    // PATCH /api/negociacoes/{id}/cancelar - Cancelar negociação
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<NegociacaoResponse> cancelarNegociacao(@PathVariable UUID id) {
        Negociacao negociacao = negociacaoService.cancelarNegociacao(id);
        NegociacaoResponse response = negociacaoMapper.toResponse(negociacao);
        return ResponseEntity.ok(response);
    }
}