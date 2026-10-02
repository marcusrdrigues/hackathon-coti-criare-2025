package com.gestao.identidade.dominio;

import java.util.UUID;

/**
 * Todas as sessões de uma pessoa acabaram de ser encerradas (ela saiu da organização, trocou
 * a senha por um link de redefinição ou um token de sessão foi reutilizado): o que estiver
 * aberto em nome dela, como as conexões em tempo real, precisa ser fechado.
 */
public record SessoesEncerradasEvento(UUID usuarioId) {
}
