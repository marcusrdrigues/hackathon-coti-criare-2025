package com.gestao.compartilhado.infraestrutura.email;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import com.gestao.compartilhado.dominio.Mascaras;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Escolhe o envio de e-mail pela configuração do servidor: a Brevo quando a chave e o
 * remetente existem, ou o envio desligado (desenvolvimento, testes e demo sem e-mail).
 */
@Slf4j
@Configuration
class ConfiguracaoDoEmail {

    @Bean
    EnvioDeEmail envioDeEmail(@Value("${app.email.brevo.chave:}") String chave,
                              @Value("${app.email.remetente:}") String remetente,
                              @Value("${app.email.remetente-nome:Portal Criare}") String nomeDoRemetente) {
        if (chave.isBlank() || remetente.isBlank()) {
            log.warn("Envio de e-mail desligado: defina BREVO_API_KEY e EMAIL_REMETENTE para enviar.");
            return new EmailDesligado();
        }
        log.info("Envio de e-mail pela Brevo, com o remetente {}.", Mascaras.email(remetente));
        // Um provedor lento não pode prender a thread do envio por muito tempo
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return new EmailPelaBrevo(RestClient.builder().requestFactory(fabrica), chave, remetente, nomeDoRemetente);
    }
}
