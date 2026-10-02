package com.gestao.identidade.dominio;

/** O que a pessoa pode fazer: dentro da organização (proprietário ou membro) ou na plataforma (superadmin). */
public enum Papel {
    /** Criou a conta da organização: opera tudo e gerencia a equipe */
    PROPRIETARIO,
    /** Opera cotações, propostas e negociações, sem gerenciar a equipe */
    MEMBRO,
    /**
     * Administra a plataforma, sem pertencer a nenhuma organização. Nunca é o papel de um
     * membro (o banco só aceita os dois anteriores em tb_membro): vem de {@link Usuario#isSuperadmin()}.
     */
    SUPERADMIN
}
