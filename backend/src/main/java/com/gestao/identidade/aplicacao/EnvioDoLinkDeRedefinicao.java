package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import com.gestao.compartilhado.dominio.Mascaras;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manda o e-mail com o link de redefinição (spec 004, R5). Só depois de o link estar gravado
 * ({@link TransactionalEventListener}) e numa thread própria: a resposta do pedido sai no
 * mesmo tempo com conta ou sem, e uma falha no provedor não desfaz nada nem aparece na tela.
 */
@Slf4j
@Component
class EnvioDoLinkDeRedefinicao {

    static final String ASSUNTO = "Redefinição de senha do Portal Criare";

    private final EnvioDeEmail envio;
    private final String enderecoDoPortal;
    private final ExecutorService envios = Executors.newVirtualThreadPerTaskExecutor();

    EnvioDoLinkDeRedefinicao(EnvioDeEmail envio,
                             @Value("${app.url-frontend:http://localhost:4200}") String enderecoDoPortal) {
        this.envio = envio;
        this.enderecoDoPortal = enderecoDoPortal.endsWith("/")
                ? enderecoDoPortal.substring(0, enderecoDoPortal.length() - 1)
                : enderecoDoPortal;
    }

    @TransactionalEventListener
    void aoCriarOLink(LinkDeRedefinicaoCriado criado) {
        // O rastreio da requisição segue para a thread do envio: uma falha aparece com o mesmo traceId
        Map<String, String> contexto = MDC.getCopyOfContextMap();
        envios.execute(() -> {
            if (contexto != null) {
                MDC.setContextMap(contexto);
            }
            try {
                envio.enviar(email(criado));
            } catch (RuntimeException e) {
                log.error("O e-mail de redefinição de senha para {} não foi enviado.", Mascaras.email(criado.email()), e);
            } finally {
                MDC.clear();
            }
        });
    }

    @PreDestroy
    void encerrar() {
        envios.close();
    }

    /**
     * O link leva o token no fragmento ({@code #token=}), que o navegador nunca manda a um
     * servidor: ele não chega a logs de acesso, proxies nem ao cabeçalho Referer.
     */
    EnvioDeEmail.Email email(LinkDeRedefinicaoCriado criado) {
        String link = enderecoDoPortal + "/redefinir-senha#token=" + criado.token();
        long minutos = criado.validade().toMinutes();
        String primeiroNome = primeiroNome(criado.nome());
        String saudacao = primeiroNome.isEmpty() ? "Olá," : "Olá, " + primeiroNome + ",";

        String texto = """
                %s

                Recebemos um pedido para redefinir a senha da sua conta no Portal Criare.
                Para criar uma senha nova, abra o link abaixo. Ele vale por %d minutos e pode ser usado uma vez:

                %s

                Se não foi você, ignore este e-mail: a sua senha continua a mesma.
                """.formatted(saudacao, minutos, link);

        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <body style="margin:0;padding:24px;background:#f5f5f7;font-family:-apple-system,'Segoe UI',Roboto,Arial,sans-serif;color:#1d1d1f;">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;">
                    <p style="margin:0 0 16px;font-size:16px;">%s</p>
                    <p style="margin:0 0 24px;font-size:16px;line-height:1.5;">Recebemos um pedido para redefinir a senha da sua conta no Portal Criare. O link vale por %d minutos e pode ser usado uma vez.</p>
                    <p style="margin:0 0 24px;"><a href="%s" style="display:inline-block;background:#1d1d1f;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:999px;font-size:15px;">Criar uma senha nova</a></p>
                    <p style="margin:0;font-size:14px;line-height:1.5;color:#6e6e73;">Se não foi você, ignore este e-mail: a sua senha continua a mesma.</p>
                  </div>
                </body>
                </html>
                """.formatted(escapar(saudacao), minutos, escapar(link));

        return new EnvioDeEmail.Email(criado.email(), criado.nome(), ASSUNTO, texto, html);
    }

    private static String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        return nome.trim().split("\\s+")[0];
    }

    /** O nome é digitado pela pessoa: no HTML, vira texto, nunca marcação. */
    static String escapar(String texto) {
        StringBuilder seguro = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '&' -> seguro.append("&amp;");
                case '<' -> seguro.append("&lt;");
                case '>' -> seguro.append("&gt;");
                case '"' -> seguro.append("&quot;");
                case '\'' -> seguro.append("&#39;");
                default -> seguro.append(c);
            }
        }
        return seguro.toString();
    }
}
