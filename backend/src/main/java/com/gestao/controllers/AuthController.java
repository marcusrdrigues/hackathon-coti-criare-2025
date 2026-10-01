package com.gestao.controllers;

import com.gestao.dtos.auth.LoginRequest;
import com.gestao.dtos.auth.TokenResponse;
import com.gestao.dtos.auth.UsuarioResponse;
import com.gestao.security.JwtProperties;
import com.gestao.security.UsuarioAtual;
import com.gestao.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação de sessão e logout (JWT + refresh token em cookie HttpOnly)")
public class AuthController {

    /** O cookie só é enviado pelo navegador para as rotas de autenticação. */
    static final String COOKIE_REFRESH = "refresh_token";
    private static final String CAMINHO_COOKIE = "/api/v1/auth";

    private final AuthService authService;
    private final UsuarioAtual usuarioAtual;
    private final JwtProperties propriedades;

    @Operation(summary = "Login",
            description = "Devolve um access token JWT de curta duração e grava o refresh token num cookie HttpOnly")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado"),
            @ApiResponse(responseCode = "401", description = "Email ou senha inválidos"),
            @ApiResponse(responseCode = "429", description = "Muitas tentativas seguidas; tente depois")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return comCookie(authService.login(request.email(), request.senha()));
    }

    @Operation(summary = "Renovar sessão",
            description = "Usa o refresh token do cookie para gerar um access token novo. O refresh token é trocado a cada uso")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> renovar(@CookieValue(name = COOKIE_REFRESH, required = false) String refresh) {
        return comCookie(authService.renovar(refresh));
    }

    @Operation(summary = "Logout", description = "Revoga o refresh token e apaga o cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE_REFRESH, required = false) String refresh) {
        authService.logout(refresh);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @Operation(summary = "Usuário atual", description = "Dados de quem está autenticado pelo access token")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.usuario(usuarioAtual.obter()));
    }

    private ResponseEntity<TokenResponse> comCookie(AuthService.Sessao sessao) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(sessao.refreshToken(), propriedades.expiracaoRefresh()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(sessao.resposta());
    }

    private ResponseCookie cookie(String valor, Duration validade) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true)                       // JavaScript não lê o cookie (protege contra XSS)
                .secure(propriedades.cookieSeguro())  // true em produção (HTTPS)
                .sameSite("Strict")                   // não vai em requisições de outros sites (protege contra CSRF)
                .path(CAMINHO_COOKIE)
                .maxAge(validade)
                .build();
    }
}
