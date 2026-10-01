package com.gestao.controllers;

import com.gestao.dtos.negociacao.FinalizarNegociacaoRequest;
import com.gestao.dtos.negociacao.NegociacaoRequest;
import com.gestao.dtos.negociacao.NegociacaoResponse;
import com.gestao.entities.Negociacao;
import com.gestao.mappers.NegociacaoMapper;
import com.gestao.security.UsuarioAtual;
import com.gestao.services.NegociacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/negociacoes")
@RequiredArgsConstructor
@Tag(name = "Negociações", description = "Negociação entre a empresa e o fornecedor escolhido")
public class NegociacaoController {

    private final NegociacaoService negociacaoService;
    private final NegociacaoMapper negociacaoMapper;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Iniciar negociação",
            description = "Perfil EMPRESA, dona da cotação. Aceita a proposta e coloca a cotação em EM_NEGOCIACAO")
    @PreAuthorize("hasRole('EMPRESA')")
    @PostMapping
    public ResponseEntity<NegociacaoResponse> criarNegociacao(@Valid @RequestBody NegociacaoRequest request) {
        Negociacao negociacao = negociacaoService.criarNegociacao(request.propostaId(), usuarioAtual.obter().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(negociacaoMapper.toResponse(negociacao));
    }

    @Operation(summary = "Minhas negociações", description = "Negociações em que o usuário do token participa")
    @GetMapping("/minhas")
    public ResponseEntity<List<NegociacaoResponse>> minhas() {
        return ResponseEntity.ok(negociacaoService.listarDoUsuario(usuarioAtual.obter()).stream()
                .map(negociacaoMapper::toResponse)
                .toList());
    }

    @Operation(summary = "Buscar negociação", description = "Só a empresa e o fornecedor participantes")
    @GetMapping("/{id}")
    public ResponseEntity<NegociacaoResponse> buscarPorId(@Parameter(description = "ID da negociação") @PathVariable UUID id) {
        return ResponseEntity.ok(negociacaoMapper.toResponse(
                negociacaoService.buscarParaParticipante(id, usuarioAtual.obter())));
    }

    @Operation(summary = "Negociação de uma proposta", description = "Só a empresa e o fornecedor participantes")
    @GetMapping("/proposta/{propostaId}")
    public ResponseEntity<NegociacaoResponse> buscarPorProposta(@PathVariable UUID propostaId) {
        return ResponseEntity.ok(negociacaoMapper.toResponse(
                negociacaoService.buscarPorPropostaParaParticipante(propostaId, usuarioAtual.obter())));
    }

    @Operation(summary = "Fechar negócio",
            description = "Perfil EMPRESA participante. Fecha a cotação e recusa as demais propostas")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<NegociacaoResponse> finalizarNegociacao(
            @PathVariable UUID id,
            @Valid @RequestBody FinalizarNegociacaoRequest request) {
        return ResponseEntity.ok(negociacaoMapper.toResponse(
                negociacaoService.finalizarNegociacao(id, request.valorFinal(), usuarioAtual.obter().id())));
    }

    @Operation(summary = "Encerrar sem acordo",
            description = "Perfil EMPRESA participante. Recusa a proposta e reabre a cotação")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<NegociacaoResponse> cancelarNegociacao(@PathVariable UUID id) {
        return ResponseEntity.ok(negociacaoMapper.toResponse(
                negociacaoService.cancelarNegociacao(id, usuarioAtual.obter().id())));
    }
}
