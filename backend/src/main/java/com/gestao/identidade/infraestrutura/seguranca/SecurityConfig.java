package com.gestao.identidade.infraestrutura.seguranca;

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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Segurança da API:
 * - stateless: cada requisição traz um access token JWT no header Authorization
 * - rotas públicas só para login (inclusive o de demonstração), renovação de sessão,
 *   cadastro, documentação e health check
 * - o perfil (EMPRESA/FORNECEDOR) vira a role usada nos @PreAuthorize
 * - o superadmin (sem organização) só alcança /api/v1/admin/** e /api/v1/auth/me
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final int TAMANHO_MINIMO_CHAVE = 32; // 256 bits, exigido pelo HS256
    private static final String SUPERADMIN = "SUPERADMIN";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, RespostasDeSeguranca respostas) throws Exception {
        http
                // CSRF desligado de propósito (revisado, ver docs/adr/0003):
                // - as rotas protegidas exigem o access token no cabeçalho Authorization, que o navegador
                //   não envia sozinho, então um site de terceiros não consegue forjar a requisição;
                // - o único cookie é o do refresh token: SameSite=Strict (o navegador não o manda em
                //   requisições vindas de outro site), HttpOnly e restrito ao caminho /api/v1/auth.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 - API sem estado, ver comentário acima
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout", "/api/v1/auth/demo/*", "/api/v1/auth/redefinicao",
                                "/api/v1/auth/redefinicao/consulta", "/api/v1/auth/redefinicao/confirmacao").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/demo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/cadastro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/convites/consulta", "/api/v1/convites/aceite").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cotacoes/categorias").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Handshake do WebSocket: a autenticação acontece no CONNECT do STOMP (AutenticacaoStomp)
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        // O superadmin só entra na área administrativa (e consulta quem é)...
                        .requestMatchers("/api/v1/admin/**").hasRole(SUPERADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        // ...e o resto da API é das organizações: um token sem organização não passa daqui
                        .requestMatchers("/api/v1/**").hasAnyRole("EMPRESA", "FORNECEDOR")
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
            ALEATORIO.nextBytes(bytes);
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

    /** Claims "tipo" e "papel" do token -> ROLE_EMPRESA / ROLE_FORNECEDOR e ROLE_PROPRIETARIO / ROLE_MEMBRO. */
    private JwtAuthenticationConverter conversorDePerfil() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(ClaimsDoToken::autoridades);
        return conversor;
    }
}
