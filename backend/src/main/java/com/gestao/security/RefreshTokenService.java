package com.gestao.security;

import com.gestao.entities.RefreshToken;
import com.gestao.enums.TipoUsuario;
import com.gestao.exceptions.UnauthorizedException;
import com.gestao.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens opacos com rotação: cada uso gera um token novo e invalida o
 * anterior. Se um token já usado aparecer de novo, é sinal de roubo — todas
 * as sessões daquele usuário são encerradas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String SESSAO_EXPIRADA = "Sessão expirada. Faça login novamente.";

    private final RefreshTokenRepository repository;
    private final JwtProperties propriedades;
    private final SecureRandom aleatorio = new SecureRandom();

    public record Rotacao(UUID usuarioId, TipoUsuario tipo, String novoToken) {}

    @Transactional
    public String emitir(UUID usuarioId, TipoUsuario tipo) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String tokenBruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant agora = Instant.now();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(tokenBruto));
        token.setUsuarioId(usuarioId);
        token.setTipoUsuario(tipo);
        token.setCriadoEm(agora);
        token.setExpiraEm(agora.plus(propriedades.expiracaoRefresh()));
        repository.save(token);

        return tokenBruto;
    }

    /**
     * Troca um refresh token válido por um novo. O noRollbackFor garante que a
     * revogação em massa (detecção de reuso) seja gravada mesmo lançando 401.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotacao rotacionar(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) {
            throw new UnauthorizedException(SESSAO_EXPIRADA);
        }

        RefreshToken token = repository.findByTokenHash(hash(tokenBruto))
                .orElseThrow(() -> new UnauthorizedException(SESSAO_EXPIRADA));
        Instant agora = Instant.now();

        if (token.isRevogado()) {
            repository.revogarTodosDoUsuario(token.getUsuarioId(), agora);
            log.warn("Refresh token reutilizado para o usuário {}. Todas as sessões foram encerradas.",
                    token.getUsuarioId());
            throw new UnauthorizedException(SESSAO_EXPIRADA);
        }
        if (token.isExpirado(agora)) {
            throw new UnauthorizedException(SESSAO_EXPIRADA);
        }

        token.setRevogadoEm(agora);
        return new Rotacao(token.getUsuarioId(), token.getTipoUsuario(),
                emitir(token.getUsuarioId(), token.getTipoUsuario()));
    }

    @Transactional
    public void revogar(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(tokenBruto))
                .filter(t -> !t.isRevogado())
                .ifPresent(t -> t.setRevogadoEm(Instant.now()));
    }

    /** Limpeza diária dos tokens vencidos há mais de um dia. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void apagarExpirados() {
        int apagados = repository.apagarExpiradosAntesDe(Instant.now().minus(Duration.ofDays(1)));
        if (apagados > 0) {
            log.info("{} refresh tokens expirados removidos.", apagados);
        }
    }

    static String hash(String tokenBruto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(tokenBruto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
