package com.gestao.configurations;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        // Habilita o botão "Authorize" no Swagger: cole o accessToken do /auth/login
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .info(new Info()
                        .title("API de Gestão de Cotações")
                        .version("1.0.0")
                        .description("API para gerenciar cotações entre empresas e fornecedores. " +
                                "Permite que empresas criem cotações, fornecedores enviem propostas " +
                                "e ambos negociem através de um sistema de mensagens. "
                                + "Autenticação via JWT: faça login em /api/v1/auth/login e use o accessToken no botão Authorize.")
                        .contact(new Contact()
                                .name("Hackathon2025")
                                .email("contato@cotiinformatica.com.br")
                                .url("https://www.cotiinformatica.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}