package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** O modelo do e-mail de redefinição (spec 004, R5). */
class EmailDeRedefinicaoTest {

    private final EnvioDoLinkDeRedefinicao envio = new EnvioDoLinkDeRedefinicao(email -> { }, "https://portal.exemplo.com/");

    @Test
    void trazOLinkComOTokenNoFragmentoEOPrazo() {
        EnvioDeEmail.Email email = envio.email(new LinkDeRedefinicaoCriado("ana@empresa.com", "Ana Ribeiro", "abc_123-XYZ",
                Duration.ofMinutes(30)));

        assertThat(email.para()).isEqualTo("ana@empresa.com");
        assertThat(email.texto())
                .contains("Olá, Ana,")
                .contains("https://portal.exemplo.com/redefinir-senha#token=abc_123-XYZ")
                .contains("30 minutos")
                .contains("a sua senha continua a mesma");
        assertThat(email.html()).contains("href=\"https://portal.exemplo.com/redefinir-senha#token=abc_123-XYZ\"");
    }

    @Test
    void nomeDigitadoViraTextoNoHtml() {
        EnvioDeEmail.Email email = envio.email(new LinkDeRedefinicaoCriado("x@empresa.com", "<script>alert(1)</script> Silva",
                "t", Duration.ofMinutes(30)));

        assertThat(email.html()).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void semNomeASaudacaoFicaNeutra() {
        EnvioDeEmail.Email email = envio.email(new LinkDeRedefinicaoCriado("x@empresa.com", " ", "t",
                Duration.ofMinutes(30)));

        assertThat(email.texto()).startsWith("Olá,\n");
    }

    @Test
    void oEventoNuncaMostraOToken() {
        assertThat(new LinkDeRedefinicaoCriado("ana@empresa.com", "Ana", "segredo", Duration.ofMinutes(30)).toString())
                .doesNotContain("segredo").doesNotContain("ana@empresa.com");
    }
}
