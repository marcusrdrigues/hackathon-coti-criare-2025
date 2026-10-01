package com.gestao.configurations;

import com.gestao.entities.Perfil;
import com.gestao.repositories.PerfilRepository;
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

    private final PerfilRepository perfilRepository;

    @Override
    public void run(String... args) {
        criarPerfisIniciais();
    }

    private void criarPerfisIniciais() {
        log.info("Verificando perfis no banco de dados...");

        // Criar perfil EMPRESA se não existir
        if (!perfilRepository.existsByNome("EMPRESA")) {
            Perfil perfilEmpresa = new Perfil();
            perfilEmpresa.setNome("EMPRESA");
            perfilRepository.save(perfilEmpresa);
            log.info("Perfil EMPRESA criado com sucesso!");
        } else {
            log.info("Perfil EMPRESA já existe");
        }

        // Criar perfil FORNECEDOR se não existir
        if (!perfilRepository.existsByNome("FORNECEDOR")) {
            Perfil perfilFornecedor = new Perfil();
            perfilFornecedor.setNome("FORNECEDOR");
            perfilRepository.save(perfilFornecedor);
            log.info("Perfil FORNECEDOR criado com sucesso!");
        } else {
            log.info("Perfil FORNECEDOR já existe");
        }
    }
}