package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.FornecedorMapper;
import com.gestao.identidade.aplicacao.FornecedorService;
import com.gestao.identidade.aplicacao.dto.FornecedorCadastroRequest;
import com.gestao.identidade.aplicacao.dto.FornecedorResponse;
import com.gestao.identidade.aplicacao.dto.FornecedorUpdateRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro e perfil de fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;
    private final FornecedorMapper fornecedorMapper;
    private final UsuarioAtual usuarioAtual;

    @Operation(summary = "Cadastrar fornecedor", description = "Rota pública. CNPJ validado e senha gravada com BCrypt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Fornecedor cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Email ou CNPJ já cadastrado")
    })
    @PostMapping
    public ResponseEntity<FornecedorResponse> cadastrarFornecedor(@Valid @RequestBody FornecedorCadastroRequest request) {
        var fornecedor = fornecedorService.cadastrarFornecedor(fornecedorMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(fornecedorMapper.toResponse(fornecedor));
    }

    @Operation(summary = "Buscar fornecedor por ID")
    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponse> buscarPorId(@Parameter(description = "ID do fornecedor") @PathVariable UUID id) {
        return ResponseEntity.ok(fornecedorMapper.toResponse(fornecedorService.buscarPorId(id)));
    }

    @Operation(summary = "Atualizar fornecedor", description = "Só o próprio fornecedor pode alterar o cadastro")
    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponse> atualizarFornecedor(
            @Parameter(description = "ID do fornecedor") @PathVariable UUID id,
            @Valid @RequestBody FornecedorUpdateRequest request) {
        usuarioAtual.exigirMesmoUsuario(id);
        return ResponseEntity.ok(fornecedorMapper.toResponse(
                fornecedorService.atualizarFornecedor(id, request.nomeCompleto())));
    }

    @Operation(summary = "Excluir fornecedor", description = "Só o próprio fornecedor pode excluir a conta")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarFornecedor(@Parameter(description = "ID do fornecedor") @PathVariable UUID id) {
        usuarioAtual.exigirMesmoUsuario(id);
        fornecedorService.deletarFornecedor(id);
        return ResponseEntity.noContent().build();
    }
}
