package com.gestao.compartilhado.dominio;

import java.util.UUID;

/**
 * Algo que importa para a segurança acabou de acontecer (spec 003, R2). Quem sabe do fato
 * publica; o módulo de auditoria grava, numa transação própria, mesmo que a operação de quem
 * publicou seja desfeita depois.
 *
 * @param tipo          o que aconteceu
 * @param usuarioId     quem agiu ou, sem login (uma tentativa de entrar), de quem é a conta;
 *                      {@code null} quando não se sabe. Se vier vazio, a auditoria usa a pessoa
 *                      autenticada na requisição
 * @param organizacaoId a organização dessa pessoa, quando ela tem uma
 * @param emailMascarado o e-mail envolvido, já mascarado ({@link Mascaras#email})
 * @param detalhe       complemento legível, sem dado pessoal nem credencial
 */
public record EventoDeSeguranca(Tipo tipo, UUID usuarioId, UUID organizacaoId, String emailMascarado,
                                String detalhe) {

    /** Os tipos registrados. Os nomes ficam no banco: só se acrescenta, nunca se renomeia. */
    public enum Tipo {
        LOGIN,
        LOGIN_FALHOU,
        LOGIN_BLOQUEADO,
        SESSAO_REVOGADA_POR_REUSO,
        LOGOUT,
        CONVITE_CRIADO,
        CONVITE_CANCELADO,
        CONVITE_ACEITO,
        MEMBRO_REMOVIDO,
        SUPERADMIN_CRIADO,
        SUPERADMIN_SENHA_TROCADA,
        SUPERADMIN_DESATIVADO,
        ACESSO_NEGADO,
        CONSULTA_DE_AUDITORIA,
        DADOS_PESSOAIS_ANONIMIZADOS
    }

    public EventoDeSeguranca {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo do evento de segurança é obrigatório.");
        }
    }

    /** Evento de quem está autenticado na requisição: a auditoria completa a pessoa e a organização. */
    public static EventoDeSeguranca daPessoaAtual(Tipo tipo, String detalhe) {
        return new EventoDeSeguranca(tipo, null, null, null, detalhe);
    }

    /** Evento com a pessoa e a organização conhecidas; o e-mail é mascarado aqui mesmo. */
    public static EventoDeSeguranca de(Tipo tipo, UUID usuarioId, UUID organizacaoId, String email,
                                       String detalhe) {
        return new EventoDeSeguranca(tipo, usuarioId, organizacaoId, mascarar(email), detalhe);
    }

    private static String mascarar(String email) {
        return email == null || email.isBlank() ? null : Mascaras.email(email);
    }
}
