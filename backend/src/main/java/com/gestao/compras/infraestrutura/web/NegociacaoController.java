package com.gestao.compras.infraestrutura.web;

import com.gestao.compras.aplicacao.NegociacaoMapper;
import com.gestao.compras.aplicacao.NegociacaoService;
import com.gestao.compras.aplicacao.dto.FinalizarNegociacaoRequest;
import com.gestao.compras.aplicacao.dto.NegociacaoRequest;
import com.gestao.compras.aplicacao.dto.NegociacaoResponse;
import com.gestao.compras.dominio.Negociacao;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
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
import java.util.Map;
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
        Negociacao negociacao = negociacaoService.criarNegociacao(request.propostaId(), usuarioAtual.obter().organizacaoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(negociacaoMapper.toResponse(negociacao));
    }

    @Operation(summary = "Minhas negociações", description = "Negociações em que o usuário do token participa")
    @GetMapping("/minhas")
    public ResponseEntity<List<NegociacaoResponse>> minhas() {
        UsuarioAutenticado usuario = usuarioAtual.obter();
        Map<UUID, Integer> naoLidas = negociacaoService.contarNaoLidas(usuario);
        return ResponseEntity.ok(negociacaoService.listarDoUsuario(usuario).stream()
                .map(n -> negociacaoMapper.toResponse(n, naoLidas.getOrDefault(n.getId(), 0)))
                .toList());
    }

    @Operation(summary = "Buscar negociação", description = "Só a empresa e o fornecedor participantes")
    @GetMapping("/{id}")
    public ResponseEntity<NegociacaoResponse> buscarPorId(@Parameter(description = "ID da negociação") @PathVariable UUID id) {
        UsuarioAutenticado usuario = usuarioAtual.obter();
        Negociacao negociacao = negociacaoService.buscarParaParticipante(id, usuario);
        return ResponseEntity.ok(negociacaoMapper.toResponse(negociacao, negociacaoService.contarNaoLidas(negociacao, usuario)));
    }

    @Operation(summary = "Marcar como lida",
            description = "Participante. As mensagens da outra parte enviadas até agora deixam de contar como não lidas")
    @PatchMapping("/{id}/leitura")
    public ResponseEntity<Void> marcarComoLida(@Parameter(description = "ID da negociação") @PathVariable UUID id) {
        negociacaoService.marcarComoLida(id, usuarioAtual.obter());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Fechar negócio",
            description = "Perfil EMPRESA participante. Fecha a cotação e recusa as demais propostas")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<NegociacaoResponse> finalizarNegociacao(
            @PathVariable UUID id,
            @Valid @RequestBody FinalizarNegociacaoRequest request) {
        return ResponseEntity.ok(negociacaoMapper.toResponse(
                negociacaoService.finalizarNegociacao(id, request.valorFinal(), usuarioAtual.obter().organizacaoId())));
    }

    @Operation(summary = "Encerrar sem acordo",
            description = "Perfil EMPRESA participante. Recusa a proposta e reabre a cotação")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<NegociacaoResponse> cancelarNegociacao(@PathVariable UUID id) {
        return ResponseEntity.ok(negociacaoMapper.toResponse(
                negociacaoService.cancelarNegociacao(id, usuarioAtual.obter().organizacaoId())));
    }
}
