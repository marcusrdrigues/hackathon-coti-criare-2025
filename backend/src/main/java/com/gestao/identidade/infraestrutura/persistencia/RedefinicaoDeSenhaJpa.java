package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.RedefinicaoDeSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Spring Data para os links de redefinição de senha. */
interface RedefinicaoDeSenhaJpa extends JpaRepository<RedefinicaoDeSenha, UUID> {

    Optional<RedefinicaoDeSenha> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RedefinicaoDeSenha r SET r.substituidaEm = :agora "
            + "WHERE r.usuarioId = :usuarioId AND r.usadaEm IS NULL AND r.substituidaEm IS NULL")
    int substituirAbertos(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RedefinicaoDeSenha r SET r.usadaEm = :agora "
            + "WHERE r.id = :id AND r.usadaEm IS NULL AND r.substituidaEm IS NULL")
    int usar(@Param("id") UUID id, @Param("agora") Instant agora);

    @Modifying
    @Query("DELETE FROM RedefinicaoDeSenha r WHERE r.expiraEm < :limite")
    int apagarVencidosAntesDe(@Param("limite") Instant limite);
}
