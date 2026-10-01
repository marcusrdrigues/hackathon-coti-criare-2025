package com.gestao.repositories;

import com.gestao.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revogadoEm = :agora WHERE r.usuarioId = :usuarioId AND r.revogadoEm IS NULL")
    int revogarTodosDoUsuario(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiraEm < :limite")
    int apagarExpiradosAntesDe(@Param("limite") Instant limite);

    @Query("SELECT COUNT(r) FROM RefreshToken r WHERE r.usuarioId = :usuarioId AND r.revogadoEm IS NULL")
    long contarAtivos(@Param("usuarioId") UUID usuarioId);
}
