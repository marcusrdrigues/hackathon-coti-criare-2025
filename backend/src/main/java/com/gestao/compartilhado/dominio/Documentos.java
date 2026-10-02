package com.gestao.compartilhado.dominio;

/**
 * Utilitários para tratar CNPJ e e-mail antes de gravar ou comparar.
 */
public final class Documentos {

    private Documentos() {
    }

    /** Remove pontuação: "11.222.333/0001-81" vira "11222333000181". */
    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    /** Valida tamanho e dígitos verificadores de um CNPJ (com ou sem máscara). */
    public static boolean cnpjValido(String cnpj) {
        String digitos = somenteDigitos(cnpj);
        if (digitos == null || digitos.length() != 14 || digitos.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = calcularDigito(digitos.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = calcularDigito(digitos.substring(0, 12) + dv1, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return digitos.charAt(12) - '0' == dv1 && digitos.charAt(13) - '0' == dv2;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static int calcularDigito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
