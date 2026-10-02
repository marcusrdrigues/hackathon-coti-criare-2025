package com.gestao.identidade.aplicacao;

import java.time.Duration;

/**
 * Um link de redefinição acabou de ser criado e precisa chegar à pessoa. Carrega o token,
 * que só existe em memória até o envio: o banco guarda o hash.
 */
record LinkDeRedefinicaoCriado(String email, String nome, String token, Duration validade) {

    /** Nunca mostra o token nem o e-mail, para não irem parar num log. */
    @Override
    public String toString() {
        return "LinkDeRedefinicaoCriado[validade=" + validade + "]";
    }
}
