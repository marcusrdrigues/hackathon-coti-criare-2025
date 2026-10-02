package com.gestao.identidade.aplicacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens opacos (sessões e convites): 256 bits aleatórios, que só a pessoa recebe.
 * O banco guarda o hash SHA-256; como o token já é aleatório e longo, não precisa de salt.
 */
final class TokenAleatorio {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenAleatorio() {
    }

    static String gerar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
