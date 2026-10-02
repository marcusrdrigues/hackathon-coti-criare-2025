package com.gestao.identidade.aplicacao;

import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;

import java.util.UUID;

/**
 * Quem está fazendo a requisição, extraído do token JWT: a pessoa, a organização
 * em nome de quem ela age, o lado dessa organização no negócio e o papel dela.
 *
 * @param usuarioId     a pessoa (registro de autoria)
 * @param organizacaoId a organização (posse dos dados: cotações, propostas, negociações)
 */
public record UsuarioAutenticado(UUID usuarioId, UUID organizacaoId, TipoOrganizacao tipo, Papel papel) {

    public static UsuarioAutenticado de(Membro membro) {
        return new UsuarioAutenticado(membro.getUsuario().getId(), membro.getOrganizacao().getId(),
                membro.getOrganizacao().getTipo(), membro.getPapel());
    }

    public boolean ehEmpresa() {
        return tipo == TipoOrganizacao.EMPRESA;
    }

    public boolean ehFornecedor() {
        return tipo == TipoOrganizacao.FORNECEDOR;
    }

    public boolean ehProprietario() {
        return papel == Papel.PROPRIETARIO;
    }
}
