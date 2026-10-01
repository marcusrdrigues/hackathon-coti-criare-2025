package com.gestao.security;

import com.gestao.entities.Fornecedor;
import com.gestao.enums.TipoUsuario;
import com.gestao.exceptions.MuitasTentativasException;
import com.gestao.exceptions.UnauthorizedException;
import com.gestao.repositories.RefreshTokenRepository;
import com.gestao.services.AuthService;
import com.gestao.services.FornecedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AutenticacaoIntegrationTest {

    private static final String SENHA = "segredo123";

    @Autowired private AuthService authService;
    @Autowired private FornecedorService fornecedorService;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        Fornecedor f = new Fornecedor();
        f.setNomeCompleto("Tech Soluções");
        f.setCnpj("45.236.789/0001-12");
        f.setEmail("auth@fornecedor.com");
        f.setSenha(SENHA);
        fornecedor = fornecedorService.cadastrarFornecedor(f);
    }

    @Test
    void accessTokenTrazSoIdentidadeEPerfil() {
        String token = authService.login("auth@fornecedor.com", SENHA).resposta().accessToken();

        Jwt jwt = jwtDecoder.decode(token);
        assertEquals(fornecedor.getId().toString(), jwt.getSubject());
        assertEquals("FORNECEDOR", jwt.getClaimAsString(TokenService.CLAIM_TIPO));
        assertEquals("portal-criare", jwt.getClaimAsString("iss"));
        assertNotNull(jwt.getExpiresAt());
        assertNull(jwt.getClaim("email"), "o token não deve carregar dados pessoais");
    }

    @Test
    void tokenAdulteradoEhRecusado() {
        String token = authService.login("auth@fornecedor.com", SENHA).resposta().accessToken();
        String[] partes = token.split("\\.");
        // Troca a assinatura por outra qualquer
        String adulterado = partes[0] + "." + partes[1] + ".assinaturaFalsa";

        assertThrows(JwtException.class, () -> jwtDecoder.decode(adulterado));
    }

    @Test
    void refreshTokenEhGravadoComoHash() {
        String refresh = authService.login("auth@fornecedor.com", SENHA).refreshToken();

        assertTrue(refreshTokenRepository.findByTokenHash(refresh).isEmpty(), "o token em texto puro não pode estar no banco");
        assertTrue(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(refresh)).isPresent());
    }

    @Test
    void refreshTrocaOTokenACadaUso() {
        String primeiro = authService.login("auth@fornecedor.com", SENHA).refreshToken();

        AuthService.Sessao renovada = authService.renovar(primeiro);

        assertNotEquals(primeiro, renovada.refreshToken());
        assertEquals(TipoUsuario.FORNECEDOR, renovada.resposta().usuario().tipo());
        assertNotNull(renovada.resposta().accessToken());
    }

    @Test
    void reusoDeRefreshTokenEncerraTodasAsSessoes() {
        String primeiro = authService.login("auth@fornecedor.com", SENHA).refreshToken();
        String segundo = authService.renovar(primeiro).refreshToken();

        // Alguém reapresenta o token antigo (ex.: foi roubado)
        assertThrows(UnauthorizedException.class, () -> authService.renovar(primeiro));

        // O token legítimo mais novo também deixa de valer
        assertThrows(UnauthorizedException.class, () -> authService.renovar(segundo));
        assertEquals(0, refreshTokenRepository.contarAtivos(fornecedor.getId()));
    }

    @Test
    void logoutRevogaORefreshToken() {
        String refresh = authService.login("auth@fornecedor.com", SENHA).refreshToken();

        authService.logout(refresh);

        assertThrows(UnauthorizedException.class, () -> authService.renovar(refresh));
    }

    @Test
    void refreshInexistenteOuVazioEhRecusado() {
        assertThrows(UnauthorizedException.class, () -> authService.renovar(null));
        assertThrows(UnauthorizedException.class, () -> authService.renovar("token-inventado"));
    }

    @Test
    void bloqueiaLoginDepoisDeCincoSenhasErradas() {
        for (int i = 0; i < TentativasLoginService.MAXIMO_FALHAS; i++) {
            assertThrows(UnauthorizedException.class, () -> authService.login("auth@fornecedor.com", "errada"));
        }

        // Nem a senha certa entra enquanto o bloqueio durar
        MuitasTentativasException bloqueio = assertThrows(MuitasTentativasException.class,
                () -> authService.login("auth@fornecedor.com", SENHA));
        assertTrue(bloqueio.getSegundosParaLiberar() > 0);

        // Outros e-mails não são afetados
        assertFalse(bloqueio.getMessage().isBlank());
    }
}
