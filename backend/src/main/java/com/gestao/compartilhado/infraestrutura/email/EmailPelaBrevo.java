package com.gestao.compartilhado.infraestrutura.email;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envio pela API HTTP da Brevo. A hospedagem gratuita da API bloqueia as portas de SMTP,
 * então o envio é por HTTPS (spec 004).
 */
class EmailPelaBrevo implements EnvioDeEmail {

    static final String ENDERECO = "https://api.brevo.com/v3/smtp/email";

    private final RestClient cliente;
    private final String remetente;
    private final String nomeDoRemetente;

    EmailPelaBrevo(RestClient.Builder construtor, String chave, String remetente, String nomeDoRemetente) {
        this.cliente = construtor.defaultHeader("api-key", chave).build();
        this.remetente = remetente;
        this.nomeDoRemetente = nomeDoRemetente;
    }

    @Override
    public void enviar(Email email) {
        try {
            cliente.post()
                    .uri(ENDERECO)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo(email))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new FalhaNoEnvioDeEmail("A Brevo não aceitou o e-mail \"" + email.assunto() + "\".", e);
        }
    }

    private Map<String, Object> corpo(Email email) {
        Map<String, Object> destinatario = new LinkedHashMap<>();
        destinatario.put("email", email.para());
        if (email.nomeDeQuemRecebe() != null && !email.nomeDeQuemRecebe().isBlank()) {
            destinatario.put("name", email.nomeDeQuemRecebe());
        }
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sender", Map.of("email", remetente, "name", nomeDoRemetente));
        corpo.put("to", List.of(destinatario));
        corpo.put("subject", email.assunto());
        corpo.put("textContent", email.texto());
        corpo.put("htmlContent", email.html());
        return corpo;
    }
}
