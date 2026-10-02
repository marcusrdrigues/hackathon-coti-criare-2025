package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.RedefinicaoDeSenhaService;
import com.gestao.identidade.aplicacao.dto.NovaSenhaRequest;
import com.gestao.identidade.aplicacao.dto.PedidoDeRedefinicaoRequest;
import com.gestao.identidade.aplicacao.dto.TokenDeRedefinicaoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas públicas do "Esqueci minha senha" (spec 004). O token vai sempre no corpo. */
@RestController
@RequestMapping("/api/v1/auth/redefinicao")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class RedefinicaoDeSenhaController {

    private final RedefinicaoDeSenhaService redefinicaoDeSenhaService;

    @Operation(summary = "Pedir redefinição de senha",
            description = "Rota pública. Responde sempre igual, com conta ou sem, e envia o link por e-mail se a conta existir")
    @ApiResponse(responseCode = "202", description = "Pedido recebido")
    @PostMapping
    public ResponseEntity<Void> pedir(@Valid @RequestBody PedidoDeRedefinicaoRequest request) {
        redefinicaoDeSenhaService.pedir(request.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Consultar link de redefinição", description = "Rota pública. Confere se o link ainda vale")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "O link vale"),
            @ApiResponse(responseCode = "404", description = "Link vencido, usado, substituído ou inexistente")
    })
    @PostMapping("/consulta")
    public ResponseEntity<Void> consultar(@Valid @RequestBody TokenDeRedefinicaoRequest request) {
        redefinicaoDeSenhaService.consultar(request.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Criar a senha nova",
            description = "Rota pública. Troca a senha, gasta o link e encerra todas as sessões da pessoa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Senha trocada"),
            @ApiResponse(responseCode = "400", description = "Senha fora das regras; o link continua valendo"),
            @ApiResponse(responseCode = "404", description = "Link vencido, usado, substituído ou inexistente")
    })
    @PostMapping("/confirmacao")
    public ResponseEntity<Void> confirmar(@Valid @RequestBody NovaSenhaRequest request) {
        redefinicaoDeSenhaService.confirmar(request.token(), request.senha());
        return ResponseEntity.noContent().build();
    }
}
