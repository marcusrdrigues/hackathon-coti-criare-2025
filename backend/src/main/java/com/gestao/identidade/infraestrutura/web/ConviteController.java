package com.gestao.identidade.infraestrutura.web;

import com.gestao.identidade.aplicacao.AuthService;
import com.gestao.identidade.aplicacao.ConviteService;
import com.gestao.identidade.aplicacao.dto.AceiteConviteRequest;
import com.gestao.identidade.aplicacao.dto.ConviteAbertoResponse;
import com.gestao.identidade.aplicacao.dto.TokenDoConviteRequest;
import com.gestao.identidade.aplicacao.dto.TokenResponse;
import com.gestao.identidade.infraestrutura.seguranca.CookieDeSessao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas públicas de quem recebeu um link de convite. O token vai sempre no corpo
 * (nunca na URL), para não ficar nos logs de acesso.
 */
@RestController
@RequestMapping("/api/v1/convites")
@RequiredArgsConstructor
@Tag(name = "Convites", description = "Aceitar o convite para a equipe de uma organização")
public class ConviteController {

    private final ConviteService conviteService;
    private final CookieDeSessao cookieDeSessao;

    @Operation(summary = "Consultar convite", description = "Rota pública. De qual organização é o convite e para quem")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Convite válido"),
            @ApiResponse(responseCode = "404", description = "Convite vencido, já usado, cancelado ou inexistente")
    })
    @PostMapping("/consulta")
    public ResponseEntity<ConviteAbertoResponse> consultar(@Valid @RequestBody TokenDoConviteRequest request) {
        return ResponseEntity.ok(ConviteAbertoResponse.de(conviteService.consultar(request.token())));
    }

    @Operation(summary = "Aceitar convite",
            description = "Rota pública. Cria a conta da pessoa como membro da organização e já abre a sessão")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Conta criada e sessão aberta"),
            @ApiResponse(responseCode = "404", description = "Convite vencido, já usado, cancelado ou inexistente"),
            @ApiResponse(responseCode = "409", description = "O e-mail do convite ganhou uma conta nesse meio-tempo")
    })
    @PostMapping("/aceite")
    public ResponseEntity<TokenResponse> aceitar(@Valid @RequestBody AceiteConviteRequest request) {
        AuthService.Sessao sessao = conviteService.aceitar(request.token(), request.nome(), request.senha());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieDeSessao.criar(sessao.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(sessao.resposta());
    }
}
