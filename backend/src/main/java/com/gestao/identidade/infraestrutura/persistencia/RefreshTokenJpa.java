package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Spring Data para as sessões. */
interface RefreshTokenJpa extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Sem {@code clearAutomatically}: quem chama costuma ter mudado a pessoa na mesma transação
     * (senha nova, desativação), e limpar o contexto descartaria a mudança feita depois.
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.usuarioId = :usuarioId")
    int apagarTodasDoUsuario(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiraEm < :limite")
    int apagarExpiradosAntesDe(@Param("limite") Instant limite);

    @Query("SELECT COUNT(r) FROM RefreshToken r WHERE r.usuarioId = :usuarioId AND r.revogadoEm IS NULL")
    long contarAtivos(@Param("usuarioId") UUID usuarioId);
}
