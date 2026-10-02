package com.gestao.identidade.aplicacao.dto;

/**
 * Resposta de login e de renovação de sessão. O refresh token NÃO vem aqui:
 * ele vai num cookie HttpOnly, fora do alcance de JavaScript.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario
) {}
