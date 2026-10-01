package com.gestao.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações de autenticação (prefixo {@code app.jwt} no application.properties).
 *
 * @param secret            chave HMAC em Base64 com pelo menos 256 bits; vazia gera uma chave aleatória a cada inicialização
 * @param issuer            emissor gravado e conferido em todo token
 * @param expiracaoAcesso   validade do access token (curta)
 * @param expiracaoRefresh  validade do refresh token (longa)
 * @param cookieSeguro      marca o cookie do refresh token como Secure (exige HTTPS)
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration expiracaoAcesso,
        Duration expiracaoRefresh,
        boolean cookieSeguro
) {}
