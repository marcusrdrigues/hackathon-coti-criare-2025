package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Convite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data para os convites. */
interface ConviteJpa extends JpaRepository<Convite, UUID> {

    Optional<Convite> findByTokenHash(String tokenHash);

    @Query("""
            SELECT c FROM Convite c
            WHERE c.organizacao.id = :organizacaoId AND c.aceitoEm IS NULL AND c.canceladoEm IS NULL
              AND c.expiraEm > :agora
            ORDER BY c.criadoEm DESC""")
    List<Convite> pendentes(@Param("organizacaoId") UUID organizacaoId, @Param("agora") Instant agora);
}
