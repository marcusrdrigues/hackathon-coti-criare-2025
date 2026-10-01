package com.gestao.security;

import com.gestao.enums.TipoRemetente;
import com.gestao.enums.TipoUsuario;

import java.util.UUID;

/** Quem está fazendo a requisição, extraído do token JWT. */
public record UsuarioAutenticado(UUID id, TipoUsuario tipo) {

    public boolean ehEmpresa() {
        return tipo == TipoUsuario.EMPRESA;
    }

    public boolean ehFornecedor() {
        return tipo == TipoUsuario.FORNECEDOR;
    }

    public TipoRemetente comoRemetente() {
        return TipoRemetente.valueOf(tipo.name());
    }
}
