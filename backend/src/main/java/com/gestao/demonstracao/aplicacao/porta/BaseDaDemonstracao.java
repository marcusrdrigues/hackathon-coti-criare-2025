package com.gestao.demonstracao.aplicacao.porta;

/** Porta: o banco da demo pública, que pode ser apagado e populado de novo. */
public interface BaseDaDemonstracao {

    /** Já existe alguma empresa cadastrada (então a demo não é populada de novo). */
    boolean temDados();

    /** Apaga cotações, propostas, negociações, sessões e contas, na ordem das chaves estrangeiras. */
    void apagarTudo();
}
