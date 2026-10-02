package com.gestao.identidade.aplicacao;

import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;

import java.util.UUID;

/**
 * Quem está fazendo a requisição, extraído do token JWT: a pessoa, a organização
 * em nome de quem ela age, o lado dessa organização no negócio e o papel dela.
 *
 * <p>O superadmin não tem organização nem tipo ({@code null}). A segurança da API só o deixa
 * chegar à área administrativa, onde nada depende desses dois campos.
 *
 * @param usuarioId     a pessoa (registro de autoria)
 * @param organizacaoId a organização (posse dos dados: cotações, propostas, negociações)
 */
public record UsuarioAutenticado(UUID usuarioId, UUID organizacaoId, TipoOrganizacao tipo, Papel papel) {

    public static UsuarioAutenticado de(Membro membro) {
        return new UsuarioAutenticado(membro.getUsuario().getId(), membro.getOrganizacao().getId(),
                membro.getOrganizacao().getTipo(), membro.getPapel());
    }

    public static UsuarioAutenticado superadmin(UUID usuarioId) {
        return new UsuarioAutenticado(usuarioId, null, null, Papel.SUPERADMIN);
    }

    public boolean ehSuperadmin() {
        return papel == Papel.SUPERADMIN;
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
