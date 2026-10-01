package com.gestao.controllers;

import com.gestao.dtos.mensagem.MensagemRequest;
import com.gestao.dtos.mensagem.MensagemResponse;
import com.gestao.entities.MensagemNegociacao;
import com.gestao.mappers.MensagemMapper;
import com.gestao.security.UsuarioAtual;
import com.gestao.services.MensagemNegociacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mensagens")
@RequiredArgsConstructor
@Tag(name = "Mensagens", description = "Histórico e contrapropostas dentro de uma negociação")
public class MensagemNegociacaoController {

    private final MensagemNegociacaoService mensagemService;
    private final MensagemMapper mensagemMapper;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Enviar mensagem ou contraproposta",
            description = "O remetente é o usuário do token, que precisa participar da negociação")
    @PostMapping
    public ResponseEntity<MensagemResponse> enviarMensagem(@Valid @RequestBody MensagemRequest request) {
        MensagemNegociacao mensagem = mensagemService.enviarMensagem(
                request.negociacaoId(), request.mensagem(), request.valorOfertado(), usuarioAtual.obter());
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemMapper.toResponse(mensagem));
    }

    @Operation(summary = "Histórico da negociação", description = "Em ordem cronológica; só para os participantes")
    @GetMapping("/negociacao/{negociacaoId}")
    public ResponseEntity<List<MensagemResponse>> listarMensagens(@PathVariable UUID negociacaoId) {
        return ResponseEntity.ok(mensagemService.listarMensagens(negociacaoId, usuarioAtual.obter()).stream()
                .map(mensagemMapper::toResponse)
                .toList());
    }

    @Operation(summary = "Buscar mensagem", description = "Só para os participantes da negociação")
    @GetMapping("/{id}")
    public ResponseEntity<MensagemResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(mensagemMapper.toResponse(mensagemService.buscarPorId(id, usuarioAtual.obter())));
    }
}
