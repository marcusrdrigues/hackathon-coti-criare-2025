package com.gestao.identidade.aplicacao.porta;

import com.gestao.identidade.aplicacao.UsuarioAutenticado;

/** Porta: emite o access token de uma sessão. A implementação (JWT) fica na infraestrutura. */
public interface EmissorDeToken {

    /** O token leva só identificadores (pessoa, organização, tipo e papel), nunca dado pessoal. */
    TokenAcesso gerar(UsuarioAutenticado usuario);

    /** Token pronto para o cliente e em quantos segundos ele vence. */
    record TokenAcesso(String valor, long expiraEmSegundos) {}
}
