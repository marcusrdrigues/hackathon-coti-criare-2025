package com.gestao.compartilhado.infraestrutura.email;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/** O adaptador da Brevo contra um servidor simulado: nenhum e-mail sai de verdade. */
class EmailPelaBrevoTest {

    private static final EnvioDeEmail.Email EMAIL = new EnvioDeEmail.Email("ana@empresa.com", "Ana Ribeiro",
            "Assunto", "Texto do e-mail", "<p>HTML do e-mail</p>");

    private MockRestServiceServer servidor;
    private EmailPelaBrevo brevo;

    @BeforeEach
    void setUp() {
        RestClient.Builder construtor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(construtor).build();
        brevo = new EmailPelaBrevo(construtor, "chave-de-teste", "portal@exemplo.com", "Portal Criare");
    }

    @Test
    void enviaPelaApiComAChaveNoCabecalho() {
        servidor.expect(requestTo(EmailPelaBrevo.ENDERECO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "chave-de-teste"))
                .andExpect(jsonPath("$.sender.email").value("portal@exemplo.com"))
                .andExpect(jsonPath("$.sender.name").value("Portal Criare"))
                .andExpect(jsonPath("$.to[0].email").value("ana@empresa.com"))
                .andExpect(jsonPath("$.to[0].name").value("Ana Ribeiro"))
                .andExpect(jsonPath("$.subject").value("Assunto"))
                .andExpect(jsonPath("$.textContent").value("Texto do e-mail"))
                .andExpect(jsonPath("$.htmlContent").value("<p>HTML do e-mail</p>"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"<1@brevo>\"}"));

        brevo.enviar(EMAIL);

        servidor.verify();
    }

    @Test
    void recusaOuQuedaDoProvedorViraFalhaNoEnvio() {
        servidor.expect(requestTo(EmailPelaBrevo.ENDERECO)).andRespond(withUnauthorizedRequest());
        assertThatThrownBy(() -> brevo.enviar(EMAIL)).isInstanceOf(EnvioDeEmail.FalhaNoEnvioDeEmail.class);

        servidor.reset();
        servidor.expect(requestTo(EmailPelaBrevo.ENDERECO)).andRespond(withServerError());
        assertThatThrownBy(() -> brevo.enviar(EMAIL)).isInstanceOf(EnvioDeEmail.FalhaNoEnvioDeEmail.class);
    }

    @Test
    void oConteudoNuncaApareceNoToString() {
        assertThat(EMAIL.toString()).doesNotContain("Texto").doesNotContain("ana@empresa.com");
    }
}
