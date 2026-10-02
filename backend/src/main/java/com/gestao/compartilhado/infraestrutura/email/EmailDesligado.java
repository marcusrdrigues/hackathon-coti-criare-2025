package com.gestao.compartilhado.infraestrutura.email;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import com.gestao.compartilhado.dominio.Mascaras;
import lombok.extern.slf4j.Slf4j;

/**
 * Sem provedor configurado: nada é enviado, e o log avisa, só com o e-mail mascarado. O
 * conteúdo nunca vai para o log, porque pode trazer um link de uso único.
 */
@Slf4j
class EmailDesligado implements EnvioDeEmail {

    @Override
    public void enviar(Email email) {
        log.warn("Envio de e-mail desligado: \"{}\" para {} não foi enviado.", email.assunto(),
                Mascaras.email(email.para()));
    }
}
