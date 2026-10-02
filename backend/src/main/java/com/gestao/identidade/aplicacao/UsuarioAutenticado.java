package com.gestao.identidade.aplicacao;

import com.gestao.identidade.dominio.TipoUsuario;

import java.util.UUID;

/** Quem está fazendo a requisição, extraído do token JWT. */
public record UsuarioAutenticado(UUID id, TipoUsuario tipo) {

    public boolean ehEmpresa() {
        return tipo == TipoUsuario.EMPRESA;
    }

    public boolean ehFornecedor() {
        return tipo == TipoUsuario.FORNECEDOR;
    }
}
