package com.gestao.identidade.aplicacao.porta;

/** Porta: transforma a senha num hash seguro e confere uma senha contra o hash gravado. */
public interface CodificadorDeSenha {

    String codificar(String senha);

    boolean confere(String senhaDigitada, String hashGravado);
}
