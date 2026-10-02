package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.FornecedorMapper;
import com.gestao.identidade.aplicacao.FornecedorService;
import com.gestao.identidade.aplicacao.dto.FornecedorCadastroRequest;
import com.gestao.identidade.aplicacao.dto.FornecedorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;
    private final FornecedorMapper fornecedorMapper;

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
}
