package com.gestao.identidade.infraestrutura.seguranca;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.List;

/**
 * O que vai no access token além do {@code sub} (a pessoa), e como isso vira
 * permissões do Spring Security. Só identificadores: nada de dado pessoal.
 */
public final class ClaimsDoToken {

    /** A organização em nome de quem a pessoa age */
    public static final String ORGANIZACAO = "org";
    /** EMPRESA ou FORNECEDOR */
    public static final String TIPO = "tipo";
    /** PROPRIETARIO ou MEMBRO */
    public static final String PAPEL = "papel";

    private ClaimsDoToken() {
    }

    /** ROLE_EMPRESA / ROLE_FORNECEDOR, mais ROLE_PROPRIETARIO / ROLE_MEMBRO. */
    public static List<GrantedAuthority> autoridades(Jwt jwt) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (String claim : List.of(TIPO, PAPEL)) {
            String valor = jwt.getClaimAsString(claim);
            if (valor != null) {
                autoridades.add(new SimpleGrantedAuthority("ROLE_" + valor));
            }
        }
        return autoridades;
    }
}
