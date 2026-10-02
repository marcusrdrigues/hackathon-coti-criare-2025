package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.EventoDeSeguranca;
import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.identidade.aplicacao.porta.RefreshTokenRepositorio;
import com.gestao.identidade.dominio.RefreshToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokens opacos com rotação: cada uso gera um token novo e invalida o
 * anterior. Se um token já usado aparecer de novo, é sinal de roubo — todas
 * as sessões daquele usuário são encerradas.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private static final String SESSAO_EXPIRADA = "Sessão expirada. Faça login novamente.";

    private final RefreshTokenRepositorio repositorio;
    private final EventosDaIdentidade eventos;
    private final Duration validade;

    public RefreshTokenService(RefreshTokenRepositorio repositorio, EventosDaIdentidade eventos,
                               @Value("${app.jwt.expiracao-refresh}") Duration validade) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.validade = validade;
    }

    public record Rotacao(UUID usuarioId, String novoToken) {}

    @Transactional
    public String emitir(UUID usuarioId) {
        String tokenBruto = TokenAleatorio.gerar();

        Instant agora = Instant.now();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(tokenBruto));
        token.setUsuarioId(usuarioId);
        token.setCriadoEm(agora);
        token.setExpiraEm(agora.plus(validade));
        repositorio.salvar(token);

        return tokenBruto;
    }

    /**
     * Troca um refresh token válido por um novo. O noRollbackFor garante que a
     * revogação em massa (detecção de reuso) seja gravada mesmo lançando 401.
     */
    @Transactional(noRollbackFor = NaoAutenticadoException.class)
    public Rotacao rotacionar(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) {
            throw new NaoAutenticadoException(SESSAO_EXPIRADA);
        }

        RefreshToken token = repositorio.buscarPorHash(hash(tokenBruto))
                .orElseThrow(() -> new NaoAutenticadoException(SESSAO_EXPIRADA));
        Instant agora = Instant.now();

        if (token.isRevogado()) {
            repositorio.revogarTodosDoUsuario(token.getUsuarioId(), agora);
            log.warn("Refresh token reutilizado para o usuário {}. Todas as sessões foram encerradas.",
                    token.getUsuarioId());
            eventos.publicar(EventoDeSeguranca.Tipo.SESSAO_REVOGADA_POR_REUSO, token.getUsuarioId(),
                    "Um token de sessão já usado apareceu de novo.");
            throw new NaoAutenticadoException(SESSAO_EXPIRADA);
        }
        if (token.isExpirado(agora)) {
            throw new NaoAutenticadoException(SESSAO_EXPIRADA);
        }

        token.setRevogadoEm(agora);
        return new Rotacao(token.getUsuarioId(), emitir(token.getUsuarioId()));
    }

    /** Encerra uma sessão; devolve de quem ela era, se ainda estava valendo. */
    @Transactional
    public Optional<UUID> revogar(String tokenBruto) {
        if (tokenBruto == null || tokenBruto.isBlank()) {
            return Optional.empty();
        }
        return repositorio.buscarPorHash(hash(tokenBruto))
                .filter(t -> !t.isRevogado())
                .map(t -> {
                    t.setRevogadoEm(Instant.now());
                    return t.getUsuarioId();
                });
    }

    /** Limpeza diária dos tokens vencidos há mais de um dia. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void apagarExpirados() {
        int apagados = repositorio.apagarExpiradosAntesDe(Instant.now().minus(Duration.ofDays(1)));
        if (apagados > 0) {
            log.info("{} refresh tokens expirados removidos.", apagados);
        }
    }

    /** Encerra todas as sessões da pessoa (por exemplo, quando ela sai da organização). */
    @Transactional
    public void revogarTodas(UUID usuarioId) {
        repositorio.revogarTodosDoUsuario(usuarioId, Instant.now());
    }

    static String hash(String tokenBruto) {
        return TokenAleatorio.hash(tokenBruto);
    }
}
