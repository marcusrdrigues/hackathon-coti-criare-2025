package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.dominio.TipoUsuario;

import java.util.UUID;

/** Porta: emite o access token de uma sessão. A implementação (JWT) fica na infraestrutura. */
public interface EmissorDeToken {

    TokenAcesso gerar(UUID usuarioId, TipoUsuario tipo);

    /** Token pronto para o cliente e em quantos segundos ele vence. */
    record TokenAcesso(String valor, long expiraEmSegundos) {}
}
