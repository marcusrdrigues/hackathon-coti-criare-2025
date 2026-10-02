package com.gestao.identidade.infraestrutura.inicializacao;

import com.gestao.identidade.aplicacao.SuperadminService.Configuracao;
import com.gestao.identidade.aplicacao.SuperadminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Na subida do servidor, cria ou atualiza o superadmin a partir de
 * {@code SUPERADMIN_EMAIL} e {@code SUPERADMIN_SENHA}. Sem elas, nenhum superadmin fica ativo.
 */
@Component
@Order(1)
class SuperadminDaConfiguracao implements ApplicationRunner {

    private final SuperadminService superadminService;
    private final Configuracao configuracao;

    SuperadminDaConfiguracao(SuperadminService superadminService,
                             @Value("${app.superadmin.email:}") String email,
                             @Value("${app.superadmin.senha:}") String senha) {
        this.superadminService = superadminService;
        this.configuracao = new Configuracao(email, senha);
    }

    @Override
    public void run(ApplicationArguments args) {
        superadminService.sincronizar(Optional.of(configuracao));
    }
}
