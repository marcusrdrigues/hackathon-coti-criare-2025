package com.gestao.controllers;

import com.gestao.dtos.mensagem.MensagemRequest;
import com.gestao.dtos.mensagem.MensagemResponse;
import com.gestao.entities.MensagemNegociacao;
import com.gestao.enums.TipoRemetente;
import com.gestao.mappers.MensagemMapper;
import com.gestao.services.MensagemNegociacaoService;
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
@CrossOrigin(origins = "*")
public class MensagemNegociacaoController {

    private final MensagemNegociacaoService mensagemService;
    private final MensagemMapper mensagemMapper;

    // POST /api/mensagens - Enviar mensagem
    @PostMapping
    public ResponseEntity<MensagemResponse> enviarMensagem(@Valid @RequestBody MensagemRequest request) {
        MensagemNegociacao mensagem = mensagemService.enviarMensagem(
                request.negociacaoId(),
                request.mensagem(),
                request.tipoRemetente(),
                request.remetenteId()
        );
        MensagemResponse response = mensagemMapper.toResponse(mensagem);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/mensagens/negociacao/{negociacaoId} - Listar mensagens de uma negociação
    @GetMapping("/negociacao/{negociacaoId}")
    public ResponseEntity<List<MensagemResponse>> listarMensagens(@PathVariable UUID negociacaoId) {
        List<MensagemNegociacao> mensagens = mensagemService.listarMensagens(negociacaoId);
        List<MensagemResponse> responses = mensagens.stream()
                .map(mensagemMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // GET /api/mensagens/{id} - Buscar mensagem por ID
    @GetMapping("/{id}")
    public ResponseEntity<MensagemResponse> buscarPorId(@PathVariable UUID id) {
        MensagemNegociacao mensagem = mensagemService.buscarPorId(id);
        MensagemResponse response = mensagemMapper.toResponse(mensagem);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/mensagens/{id} - Deletar mensagem
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarMensagem(@PathVariable UUID id) {
        mensagemService.deletarMensagem(id);
        return ResponseEntity.noContent().build();
    }
}