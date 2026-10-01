package com.gestao.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Segurança da API:
 * - stateless: cada requisição traz um access token JWT no header Authorization
 * - rotas públicas só para login (inclusive o de demonstração), renovação de sessão,
 *   cadastro, documentação e health check
 * - o perfil (EMPRESA/FORNECEDOR) vira a role usada nos @PreAuthorize
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final int TAMANHO_MINIMO_CHAVE = 32; // 256 bits, exigido pelo HS256

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, RespostasDeSeguranca respostas) throws Exception {
        http
                // Sem sessão e sem cookie de autenticação nas rotas protegidas, então não há CSRF a explorar.
                // O cookie do refresh token é SameSite=Strict e restrito a /api/v1/auth.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout", "/api/v1/auth/demo/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/demo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/empresas", "/api/v1/fornecedores").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cotacoes/categorias").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePerfil()))
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas));

        return http.build();
    }

    @Bean
    public SecretKey chaveJwt(JwtProperties propriedades) {
        String segredo = propriedades.secret();
        byte[] bytes;

        if (segredo == null || segredo.isBlank()) {
            bytes = new byte[TAMANHO_MINIMO_CHAVE];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET não definido: usando uma chave aleatória. Os access tokens deixam de valer "
                    + "quando a API reinicia. Defina JWT_SECRET em produção.");
        } else {
            bytes = Base64.getDecoder().decode(segredo.trim());
            if (bytes.length < TAMANHO_MINIMO_CHAVE) {
                throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 256 bits (32 bytes) em Base64.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt, JwtProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Confere assinatura, expiração e emissor
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propriedades.issuer()));
        return decoder;
    }

    /** Claim "tipo" do token -> ROLE_EMPRESA / ROLE_FORNECEDOR. */
    private JwtAuthenticationConverter conversorDePerfil() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {
            String tipo = jwt.getClaimAsString(TokenService.CLAIM_TIPO);
            return tipo == null
                    ? List.<GrantedAuthority>of()
                    : List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + tipo));
        });
        return conversor;
    }
}
