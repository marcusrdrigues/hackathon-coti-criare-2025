package com.gestao.compras.infraestrutura.web;

import com.gestao.compras.aplicacao.CotacaoMapper;
import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.dto.CategoriaResponse;
import com.gestao.compras.aplicacao.dto.CotacaoRequest;
import com.gestao.compras.aplicacao.dto.CotacaoResponse;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cotacoes")
@RequiredArgsConstructor
@Tag(name = "Cotações", description = "Pedidos de compra publicados pelas empresas")
public class CotacaoController {

    private final CotacaoService cotacaoService;
    private final CotacaoMapper cotacaoMapper;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Criar cotação", description = "Perfil EMPRESA. A cotação fica em nome da empresa do token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cotação criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou data limite no passado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é uma empresa")
    })
    @PreAuthorize("hasRole('EMPRESA')")
    @PostMapping
    public ResponseEntity<CotacaoResponse> criarCotacao(@Valid @RequestBody CotacaoRequest request) {
        Cotacao cotacao = cotacaoService.criarCotacao(cotacaoMapper.toEntity(request), usuarioAtual.obter().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(cotacaoMapper.toResponse(cotacao));
    }

    @Operation(summary = "Minhas cotações", description = "Perfil EMPRESA. Cotações da empresa do token, mais recentes primeiro")
    @PreAuthorize("hasRole('EMPRESA')")
    @GetMapping("/minhas")
    public ResponseEntity<List<CotacaoResponse>> minhas() {
        return ResponseEntity.ok(paraResposta(cotacaoService.listarPorEmpresa(usuarioAtual.obter().id())));
    }

    @Operation(summary = "Mural", description = "Cotações abertas e dentro do prazo")
    @GetMapping("/abertas")
    public ResponseEntity<List<CotacaoResponse>> listarCotacoesAbertas() {
        return ResponseEntity.ok(paraResposta(cotacaoService.listarCotacoesAbertas()));
    }

    @Operation(summary = "Listar categorias", description = "Rota pública. Categorias disponíveis para classificar uma cotação")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(Arrays.stream(CategoriaCotacao.values())
                .map(c -> new CategoriaResponse(c.name(), c.getDescricao()))
                .toList());
    }

    @Operation(summary = "Buscar cotação", description = "Fornecedores veem qualquer cotação; empresas, só as próprias")
    @GetMapping("/{id}")
    public ResponseEntity<CotacaoResponse> buscarPorId(@Parameter(description = "ID da cotação") @PathVariable UUID id) {
        return ResponseEntity.ok(cotacaoMapper.toResponse(cotacaoService.buscarParaUsuario(id, usuarioAtual.obter())));
    }

    @Operation(summary = "Editar cotação", description = "Perfil EMPRESA, dona da cotação. Só cotações abertas")
    @PreAuthorize("hasRole('EMPRESA')")
    @PutMapping("/{id}")
    public ResponseEntity<CotacaoResponse> atualizarCotacao(
            @Parameter(description = "ID da cotação") @PathVariable UUID id,
            @Valid @RequestBody CotacaoRequest request) {
        Cotacao cotacao = cotacaoService.atualizarCotacao(id, usuarioAtual.obter().id(), cotacaoMapper.toEntity(request));
        return ResponseEntity.ok(cotacaoMapper.toResponse(cotacao));
    }

    @Operation(summary = "Cancelar cotação", description = "Perfil EMPRESA, dona da cotação. Recusa as propostas pendentes")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CotacaoResponse> cancelarCotacao(@Parameter(description = "ID da cotação") @PathVariable UUID id) {
        return ResponseEntity.ok(cotacaoMapper.toResponse(cotacaoService.cancelarCotacao(id, usuarioAtual.obter().id())));
    }

    @Operation(summary = "Excluir cotação", description = "Perfil EMPRESA, dona da cotação. Só cotações sem propostas")
    @PreAuthorize("hasRole('EMPRESA')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCotacao(@Parameter(description = "ID da cotação") @PathVariable UUID id) {
        cotacaoService.deletarCotacao(id, usuarioAtual.obter().id());
        return ResponseEntity.noContent().build();
    }

    private List<CotacaoResponse> paraResposta(List<Cotacao> cotacoes) {
        return cotacoes.stream().map(cotacaoMapper::toResponse).toList();
    }
}
