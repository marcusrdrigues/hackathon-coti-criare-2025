package com.gestao.auditoria.dominio;

/** De onde veio uma alteração registrada no histórico. */
public enum OrigemDaRevisao {
    /** Uma pessoa autenticada: a revisão traz quem foi e de qual organização */
    PESSOA,
    /** Uma rota pública, sem login (cadastro, aceite de convite): a pessoa é a que acabou de entrar */
    PUBLICO,
    /** O próprio sistema, fora de uma requisição (reset da demo, rotinas agendadas, configuração) */
    SISTEMA
}
