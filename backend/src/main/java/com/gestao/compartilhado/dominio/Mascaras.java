package com.gestao.compartilhado.dominio;

/**
 * Esconde dados pessoais antes de irem para logs (ver docs/adr/0014). Mantém só o
 * suficiente para alguém reconhecer o dado durante uma investigação.
 */
public final class Mascaras {

    private Mascaras() {
    }

    /** {@code maria.souza@empresa.com} vira {@code m***@empresa.com}. */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }
}
