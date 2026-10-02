package com.gestao.compras.infraestrutura.web;

import com.gestao.compartilhado.aplicacao.Pagina;
import com.gestao.compartilhado.aplicacao.PedidoDePagina;
import com.gestao.compartilhado.infraestrutura.web.PaginaResponse;
import com.gestao.compartilhado.infraestrutura.web.Paginacao;
import com.gestao.compras.aplicacao.CotacaoMapper;
import com.gestao.compras.aplicacao.CotacaoService;
import com.gestao.compras.aplicacao.dto.CategoriaResponse;
import com.gestao.compras.aplicacao.dto.CotacaoRequest;
import com.gestao.compras.aplicacao.dto.CotacaoResponse;
import com.gestao.compras.dominio.CategoriaCotacao;
import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.identidade.aplicacao.UsuarioAutenticado;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cotacoes")
@RequiredArgsConstructor
@Tag(name = "Cotações", description = "Pedidos de compra publicados pelas empresas")
public class CotacaoController {

    private static final PedidoDePagina.Ordem MAIS_RECENTES = new PedidoDePagina.Ordem("dataCriacao", false);

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
        Cotacao cotacao = cotacaoService.criarCotacao(cotacaoMapper.toEntity(request), usuarioAtual.obter());
        return ResponseEntity.status(HttpStatus.CREATED).body(cotacaoMapper.toResponse(cotacao));
    }

    @Operation(summary = "Minhas cotações", description = "Perfil EMPRESA. Cotações da empresa do token, paginadas "
            + "(page, size até 50, sort por dataCriacao, dataLimite ou nomeServico; padrão: mais recentes), "
            + "com filtro opcional por situação e por texto no título ou nos requisitos")
    @PreAuthorize("hasRole('EMPRESA')")
    @GetMapping("/minhas")
    public ResponseEntity<PaginaResponse<CotacaoResponse>> minhas(
            @Parameter(description = "Situação") @RequestParam(required = false) StatusCotacao status,
            @Parameter(description = "Texto no título ou nos requisitos") @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordem, ex.: dataLimite,asc") @RequestParam(required = false) String sort) {
        PedidoDePagina pedido = Paginacao.pedido(page, size, sort, CotacaoService.ORDENS_DA_EMPRESA, MAIS_RECENTES);
        Pagina<Cotacao> pagina = cotacaoService.buscarDaEmpresa(usuarioAtual.obter(), status, busca, pedido);
        return ResponseEntity.ok(PaginaResponse.de(pagina.map(cotacaoMapper::toResponse)));
    }

    @Operation(summary = "Minhas cotações por situação",
            description = "Perfil EMPRESA. Quantas cotações a empresa tem em cada situação, inclusive as em zero")
    @PreAuthorize("hasRole('EMPRESA')")
    @GetMapping("/minhas/contagem")
    public ResponseEntity<Map<StatusCotacao, Long>> contagem() {
        return ResponseEntity.ok(cotacaoService.contarDaEmpresaPorSituacao(usuarioAtual.obter()));
    }

    @Operation(summary = "Mural", description = "Cotações abertas e dentro do prazo, paginadas (sort por dataCriacao "
            + "ou dataLimite; padrão: mais recentes), com filtro opcional por categoria e por texto no título, nos "
            + "requisitos ou no nome da empresa. Para o fornecedor, cada item traz a proposta que ele já enviou")
    @GetMapping("/abertas")
    public ResponseEntity<PaginaResponse<CotacaoResponse>> listarCotacoesAbertas(
            @Parameter(description = "Categoria") @RequestParam(required = false) CategoriaCotacao categoria,
            @Parameter(description = "Texto no título, nos requisitos ou na empresa")
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordem, ex.: dataLimite,asc") @RequestParam(required = false) String sort) {
        UsuarioAutenticado usuario = usuarioAtual.obter();
        PedidoDePagina pedido = Paginacao.pedido(page, size, sort, CotacaoService.ORDENS_DO_MURAL, MAIS_RECENTES);
        Pagina<Cotacao> pagina = cotacaoService.buscarNoMural(categoria, busca, pedido);
        return ResponseEntity.ok(PaginaResponse.de(pagina.map(c -> cotacaoMapper.paraQuemPede(c, usuario))));
    }

    @Operation(summary = "Listar categorias", description = "Rota pública. Categorias disponíveis para classificar uma cotação")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(Arrays.stream(CategoriaCotacao.values())
                .map(c -> new CategoriaResponse(c.name(), c.getDescricao()))
                .toList());
    }

    @Operation(summary = "Buscar cotação", description = "Empresas veem as próprias; fornecedores, as abertas e aquelas para as quais enviaram proposta. As demais respondem 404")
    @GetMapping("/{id}")
    public ResponseEntity<CotacaoResponse> buscarPorId(@Parameter(description = "ID da cotação") @PathVariable UUID id) {
        UsuarioAutenticado usuario = usuarioAtual.obter();
        return ResponseEntity.ok(cotacaoMapper.paraQuemPede(cotacaoService.buscarParaUsuario(id, usuario), usuario));
    }

    @Operation(summary = "Editar cotação", description = "Perfil EMPRESA, dona da cotação. Só cotações abertas")
    @PreAuthorize("hasRole('EMPRESA')")
    @PutMapping("/{id}")
    public ResponseEntity<CotacaoResponse> atualizarCotacao(
            @Parameter(description = "ID da cotação") @PathVariable UUID id,
            @Valid @RequestBody CotacaoRequest request) {
        Cotacao cotacao = cotacaoService.atualizarCotacao(id, usuarioAtual.obter(), cotacaoMapper.toEntity(request));
        return ResponseEntity.ok(cotacaoMapper.toResponse(cotacao));
    }

    @Operation(summary = "Cancelar cotação", description = "Perfil EMPRESA, dona da cotação. Recusa as propostas pendentes")
    @PreAuthorize("hasRole('EMPRESA')")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CotacaoResponse> cancelarCotacao(@Parameter(description = "ID da cotação") @PathVariable UUID id) {
        return ResponseEntity.ok(cotacaoMapper.toResponse(cotacaoService.cancelarCotacao(id, usuarioAtual.obter())));
    }

}
