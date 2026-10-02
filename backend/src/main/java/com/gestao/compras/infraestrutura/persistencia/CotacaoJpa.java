package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Spring Data para as cotações. */
interface CotacaoJpa extends JpaRepository<Cotacao, UUID> {

    @Query("SELECT c FROM Cotacao c WHERE c.empresa.id = :empresaId ORDER BY c.dataCriacao DESC")
    List<Cotacao> daEmpresa(@Param("empresaId") UUID empresaId);

    @Query("SELECT c FROM Cotacao c WHERE c.empresa.id = :empresaId AND c.status = :status ORDER BY c.dataCriacao DESC")
    List<Cotacao> daEmpresaPorStatus(@Param("empresaId") UUID empresaId, @Param("status") StatusCotacao status);

    @Query("SELECT c FROM Cotacao c WHERE c.status = :status ORDER BY c.dataCriacao DESC")
    List<Cotacao> porStatus(@Param("status") StatusCotacao status);

    @Query("""
            SELECT c FROM Cotacao c
            WHERE c.status = :status
            AND (c.dataLimite IS NULL OR c.dataLimite > :agora)
            ORDER BY c.dataCriacao DESC""")
    List<Cotacao> vigentesPorStatus(@Param("status") StatusCotacao status, @Param("agora") LocalDateTime agora);
}
