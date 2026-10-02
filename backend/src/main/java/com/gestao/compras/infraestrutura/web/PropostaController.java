package com.gestao.compras.infraestrutura.web;

import com.gestao.compras.aplicacao.PropostaMapper;
import com.gestao.compras.aplicacao.PropostaService;
import com.gestao.compras.aplicacao.dto.PropostaRequest;
import com.gestao.compras.aplicacao.dto.PropostaResponse;
import com.gestao.compras.dominio.Proposta;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/propostas")
@RequiredArgsConstructor
@Tag(name = "Propostas", description = "Lances dos fornecedores para as cotações")
public class PropostaController {

    private final PropostaService propostaService;
    private final PropostaMapper propostaMapper;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Enviar proposta", description = "Perfil FORNECEDOR. Uma proposta por cotação, dentro do prazo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Proposta enviada"),
            @ApiResponse(responseCode = "400", description = "Cotação fechada, prazo vencido ou proposta repetida"),
            @ApiResponse(responseCode = "403", description = "Usuário não é um fornecedor")
    })
    @PreAuthorize("hasRole('FORNECEDOR')")
    @PostMapping
    public ResponseEntity<PropostaResponse> criarProposta(@Valid @RequestBody PropostaRequest request) {
        Proposta proposta = propostaService.criarProposta(
                propostaMapper.toEntity(request), usuarioAtual.obter().id(), request.cotacaoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(propostaMapper.toResponse(proposta));
    }

    @Operation(summary = "Minhas propostas", description = "Perfil FORNECEDOR. Propostas enviadas pelo fornecedor do token")
    @PreAuthorize("hasRole('FORNECEDOR')")
    @GetMapping("/minhas")
    public ResponseEntity<List<PropostaResponse>> minhas() {
        return ResponseEntity.ok(paraResposta(propostaService.listarPorFornecedor(usuarioAtual.obter().id())));
    }

    @Operation(summary = "Buscar proposta", description = "Só o fornecedor autor e a empresa dona da cotação")
    @GetMapping("/{id}")
    public ResponseEntity<PropostaResponse> buscarPorId(@Parameter(description = "ID da proposta") @PathVariable UUID id) {
        return ResponseEntity.ok(propostaMapper.toResponse(propostaService.buscarParaUsuario(id, usuarioAtual.obter())));
    }

    @Operation(summary = "Propostas de uma cotação",
            description = "Perfil EMPRESA, dona da cotação. Fornecedores não veem os lances dos concorrentes")
    @PreAuthorize("hasRole('EMPRESA')")
    @GetMapping("/cotacao/{cotacaoId}")
    public ResponseEntity<List<PropostaResponse>> listarPorCotacao(
            @Parameter(description = "ID da cotação") @PathVariable UUID cotacaoId) {
        return ResponseEntity.ok(paraResposta(propostaService.listarPorCotacao(cotacaoId, usuarioAtual.obter().id())));
    }

    @Operation(summary = "Recusar proposta", description = "Perfil EMPRESA, dona da cotação")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/recusar")
    public ResponseEntity<PropostaResponse> recusarProposta(@Parameter(description = "ID da proposta") @PathVariable UUID id) {
        return ResponseEntity.ok(propostaMapper.toResponse(propostaService.recusarProposta(id, usuarioAtual.obter().id())));
    }

    @Operation(summary = "Retirar proposta", description = "Perfil FORNECEDOR, autor. Só enquanto não foi aceita")
    @PreAuthorize("hasRole('FORNECEDOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarProposta(@Parameter(description = "ID da proposta") @PathVariable UUID id) {
        propostaService.deletarProposta(id, usuarioAtual.obter().id());
        return ResponseEntity.noContent().build();
    }

    private List<PropostaResponse> paraResposta(List<Proposta> propostas) {
        return propostas.stream().map(propostaMapper::toResponse).toList();
    }
}
