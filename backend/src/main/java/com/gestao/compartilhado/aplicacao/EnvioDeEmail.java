package com.gestao.compartilhado.aplicacao;

/**
 * Porta de envio de e-mail (spec 004). Quem envia não sabe qual é o provedor; trocar de
 * provedor é trocar o adaptador.
 */
public interface EnvioDeEmail {

    /**
     * Envia agora. Uma falha do provedor vira {@link FalhaNoEnvioDeEmail}: quem chama decide
     * se ela importa (na redefinição de senha, só vai para o log).
     */
    void enviar(Email email);

    /** Um e-mail para uma pessoa, em texto e em HTML. */
    record Email(String para, String nomeDeQuemRecebe, String assunto, String texto, String html) {

        /** Nunca mostra o conteúdo, que pode trazer um link de uso único. */
        @Override
        public String toString() {
            return "Email[assunto=" + assunto + "]";
        }
    }

    /** O provedor recusou ou não respondeu. */
    class FalhaNoEnvioDeEmail extends RuntimeException {

        public FalhaNoEnvioDeEmail(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
