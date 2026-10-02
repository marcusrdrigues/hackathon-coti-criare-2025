package com.gestao.compartilhado.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MascarasTest {

    @Test
    void emailMantemSoAPrimeiraLetraEODominio() {
        assertThat(Mascaras.email("maria.souza@empresa.com")).isEqualTo("m***@empresa.com");
    }

    @Test
    void emailVazioOuSemArrobaNaoVaza() {
        assertThat(Mascaras.email(null)).isEmpty();
        assertThat(Mascaras.email(" ")).isEmpty();
        assertThat(Mascaras.email("semarroba")).isEqualTo("***");
        assertThat(Mascaras.email("@dominio.com")).isEqualTo("***");
    }
}
