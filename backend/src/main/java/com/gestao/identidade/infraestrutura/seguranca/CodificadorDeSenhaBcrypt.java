package com.gestao.identidade.infraestrutura.seguranca;

import com.gestao.identidade.aplicacao.porta.CodificadorDeSenha;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Adaptador: BCrypt do Spring Security, usando só o módulo de criptografia. */
@Component
class CodificadorDeSenhaBcrypt implements CodificadorDeSenha {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String codificar(String senha) {
        return bcrypt.encode(senha);
    }

    @Override
    public boolean confere(String senhaDigitada, String hashGravado) {
        return bcrypt.matches(senhaDigitada, hashGravado);
    }
}
