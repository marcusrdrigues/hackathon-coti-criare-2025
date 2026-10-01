package com.gestao.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Usa apenas o módulo de criptografia do Spring Security (BCrypt),
 * sem ligar a cadeia de filtros de segurança.
 */
@Configuration
public class SenhaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
