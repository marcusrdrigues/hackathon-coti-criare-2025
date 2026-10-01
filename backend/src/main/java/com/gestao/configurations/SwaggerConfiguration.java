package com.gestao.configurations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestão de Cotações")
                        .version("1.0.0")
                        .description("API para gerenciar cotações entre empresas e fornecedores. " +
                                "Permite que empresas criem cotações, fornecedores enviem propostas " +
                                "e ambos negociem através de um sistema de mensagens.")
                        .contact(new Contact()
                                .name("Hackathon2025")
                                .email("contato@cotiinformatica.com.br")
                                .url("https://www.cotiinformatica.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}