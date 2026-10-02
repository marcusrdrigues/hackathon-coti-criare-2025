package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.AuthService;
import com.gestao.identidade.aplicacao.CadastroService.NovaOrganizacao;
import com.gestao.identidade.aplicacao.CadastroService;
import com.gestao.identidade.aplicacao.dto.CadastroRequest;
import com.gestao.identidade.aplicacao.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cadastro")
@RequiredArgsConstructor
@Tag(name = "Cadastro", description = "Entrada de empresas e fornecedores na plataforma")
public class CadastroController {

    private final CadastroService cadastroService;
    private final AuthService authService;

    @Operation(summary = "Cadastrar organização",
            description = "Rota pública. Cria a organização (empresa ou fornecedor) e a pessoa proprietária juntas. "
                    + "CNPJ validado, e-mail único e senha gravada com BCrypt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Organização e proprietário criados"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail ou CNPJ já cadastrado")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
        var proprietario = cadastroService.cadastrar(new NovaOrganizacao(request.tipo(), request.razaoSocial(),
                request.cnpj(), request.nome(), request.email(), request.senha()));
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.usuario(proprietario));
    }
}
