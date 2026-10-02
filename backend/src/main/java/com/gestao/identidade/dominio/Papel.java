package com.gestao.identidade.dominio;

/** O que a pessoa pode fazer dentro da organização. */
public enum Papel {
    /** Criou a conta da organização: opera tudo e gerencia a equipe */
    PROPRIETARIO,
    /** Opera cotações, propostas e negociações, sem gerenciar a equipe */
    MEMBRO
}
