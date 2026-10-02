package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.EquipeService;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
import com.gestao.identidade.aplicacao.dto.ConviteRequest;
import com.gestao.identidade.aplicacao.dto.ConviteResponse;
import com.gestao.identidade.aplicacao.dto.MembroResponse;
import com.gestao.identidade.infraestrutura.seguranca.UsuarioAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/equipe")
@RequiredArgsConstructor
@Tag(name = "Equipe", description = "Pessoas da organização e convites (só o proprietário convida e remove)")
public class EquipeController {

    private final EquipeService equipeService;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Equipe", description = "Pessoas ativas da organização do token, proprietários primeiro")
    @GetMapping("/membros")
    public ResponseEntity<List<MembroResponse>> membros() {
        UsuarioAutenticado usuario = usuarioAtual.obter();
        return ResponseEntity.ok(equipeService.listarMembros(usuario).stream()
                .map(membro -> MembroResponse.de(membro, usuario.usuarioId()))
                .toList());
    }

    @Operation(summary = "Remover da equipe",
            description = "Proprietário. As sessões da pessoa deixam de valer na hora; o histórico continua com o nome dela")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Removida"),
            @ApiResponse(responseCode = "400", description = "Remover a si mesmo ou o último proprietário"),
            @ApiResponse(responseCode = "404", description = "Não é da equipe desta organização")
    })
    @PreAuthorize("hasRole('PROPRIETARIO')")
    @DeleteMapping("/membros/{id}")
    public ResponseEntity<Void> remover(@Parameter(description = "ID do vínculo de membro") @PathVariable UUID id) {
        equipeService.removerMembro(usuarioAtual.obter(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Convites pendentes", description = "Proprietário. Os links não aparecem aqui: só na criação")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    @GetMapping("/convites")
    public ResponseEntity<List<ConviteResponse>> convites() {
        return ResponseEntity.ok(equipeService.listarConvites(usuarioAtual.obter()).stream()
                .map(ConviteResponse::de)
                .toList());
    }

    @Operation(summary = "Convidar",
            description = "Proprietário. Devolve o token do link de uso único, válido por 72 horas, para repassar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Convite criado; o token só aparece nesta resposta"),
            @ApiResponse(responseCode = "409", description = "Já existe uma conta com este e-mail")
    })
    @PreAuthorize("hasRole('PROPRIETARIO')")
    @PostMapping("/convites")
    public ResponseEntity<ConviteResponse> convidar(@Valid @RequestBody ConviteRequest request) {
        EquipeService.ConviteCriado criado = equipeService.convidar(usuarioAtual.obter(), request.nome(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ConviteResponse.comToken(criado.convite(), criado.token()));
    }

    @Operation(summary = "Cancelar convite", description = "Proprietário. O link deixa de valer")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    @DeleteMapping("/convites/{id}")
    public ResponseEntity<Void> cancelar(@Parameter(description = "ID do convite") @PathVariable UUID id) {
        equipeService.cancelarConvite(usuarioAtual.obter(), id);
        return ResponseEntity.noContent().build();
    }
}
