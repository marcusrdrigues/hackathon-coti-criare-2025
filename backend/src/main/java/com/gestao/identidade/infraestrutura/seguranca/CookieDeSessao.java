package com.gestao.identidade.infraestrutura.seguranca;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Monta o cookie do refresh token com as mesmas proteções em todo lugar que abre sessão. */
@Component
@RequiredArgsConstructor
public class CookieDeSessao {

    public static final String NOME = "refresh_token";
    /** O navegador só envia o cookie para as rotas de autenticação. */
    private static final String CAMINHO = "/api/v1/auth";

    private final JwtProperties propriedades;

    public ResponseCookie criar(String refreshToken) {
        return montar(refreshToken, propriedades.expiracaoRefresh());
    }

    public ResponseCookie apagar() {
        return montar("", Duration.ZERO);
    }

    private ResponseCookie montar(String valor, Duration validade) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)                       // JavaScript não lê o cookie (protege contra XSS)
                .secure(propriedades.cookieSeguro())  // true em produção (HTTPS)
                .sameSite("Strict")                   // não vai em requisições de outros sites (protege contra CSRF)
                .path(CAMINHO)
                .maxAge(validade)
                .build();
    }
}
