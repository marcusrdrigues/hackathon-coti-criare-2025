package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.aplicacao.porta.PerfilRepositorio;
import com.gestao.identidade.dominio.Perfil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CriarPerfis implements CommandLineRunner {

    private final PerfilRepositorio perfilRepositorio;

    @Override
    public void run(String... args) {
        criarPerfisIniciais();
    }

    private void criarPerfisIniciais() {
        log.info("Verificando perfis no banco de dados...");

        // Criar perfil EMPRESA se não existir
        if (!perfilRepositorio.existeComNome("EMPRESA")) {
            Perfil perfilEmpresa = new Perfil();
            perfilEmpresa.setNome("EMPRESA");
            perfilRepositorio.salvar(perfilEmpresa);
            log.info("Perfil EMPRESA criado com sucesso!");
        } else {
            log.info("Perfil EMPRESA já existe");
        }

        // Criar perfil FORNECEDOR se não existir
        if (!perfilRepositorio.existeComNome("FORNECEDOR")) {
            Perfil perfilFornecedor = new Perfil();
            perfilFornecedor.setNome("FORNECEDOR");
            perfilRepositorio.salvar(perfilFornecedor);
            log.info("Perfil FORNECEDOR criado com sucesso!");
        } else {
            log.info("Perfil FORNECEDOR já existe");
        }
    }
}