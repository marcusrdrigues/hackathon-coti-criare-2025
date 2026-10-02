package com.gestao.compartilhado.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentosTest {

    @Test
    void aceitaCnpjValidoComOuSemMascara() {
        assertTrue(Documentos.cnpjValido("11.222.333/0001-81"));
        assertTrue(Documentos.cnpjValido("11222333000181"));
    }

    @Test
    void recusaCnpjComDigitoErradoOuRepetido() {
        assertFalse(Documentos.cnpjValido("11.222.333/0001-82"));
        assertFalse(Documentos.cnpjValido("11111111111111"));
        assertFalse(Documentos.cnpjValido("123"));
        assertFalse(Documentos.cnpjValido(null));
    }

    @Test
    void normalizaEmailEDigitos() {
        assertEquals("11222333000181", Documentos.somenteDigitos("11.222.333/0001-81"));
        assertEquals("contato@empresa.com", Documentos.normalizarEmail("  Contato@Empresa.COM "));
    }
}
