package com.gestao.repositories;

import com.gestao.entities.Cotacao;
import com.gestao.enums.StatusCotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CotacaoRepository extends JpaRepository<Cotacao, UUID> {

    // Buscar todas as cotações de uma empresa
    @Query("SELECT c FROM Cotacao c WHERE c.empresa.id = :empresaId ORDER BY c.dataCriacao DESC")
    List<Cotacao> findByEmpresaId(@Param("empresaId") UUID empresaId);

    // Buscar cotações por status
    @Query("SELECT c FROM Cotacao c WHERE c.status = :status ORDER BY c.dataCriacao DESC")
    List<Cotacao> findByStatus(@Param("status") StatusCotacao status);

    // Buscar cotações abertas ordenadas por data
    @Query("SELECT c FROM Cotacao c WHERE c.status = :status ORDER BY c.dataCriacao DESC")
    List<Cotacao> findByStatusOrderByDataCriacaoDesc(@Param("status") StatusCotacao status);

    // Buscar cotações de uma empresa por status
    @Query("SELECT c FROM Cotacao c WHERE c.empresa.id = :empresaId AND c.status = :status ORDER BY c.dataCriacao DESC")
    List<Cotacao> findByEmpresaIdAndStatus(@Param("empresaId") UUID empresaId, @Param("status") StatusCotacao status);

    // Buscar cotações por categoria (busca parcial)
    @Query("SELECT c FROM Cotacao c WHERE LOWER(c.nomeServico) LIKE LOWER(CONCAT('%', :nomeServico, '%')) ORDER BY c.dataCriacao DESC")
    List<Cotacao> findByNomeServicoContaining(@Param("nomeServico") String nomeServico);

    // Buscar cotações com data limite próxima (ex: próximos 7 dias)
    @Query("SELECT c FROM Cotacao c WHERE c.status = :status AND c.dataLimite BETWEEN :dataInicio AND :dataFim ORDER BY c.dataLimite ASC")
    List<Cotacao> findByStatusAndDataLimiteBetween(
            @Param("status") StatusCotacao status,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    // Contar cotações de uma empresa
    @Query("SELECT COUNT(c) FROM Cotacao c WHERE c.empresa.id = :empresaId")
    long countByEmpresaId(@Param("empresaId") UUID empresaId);

    // Contar cotações por status
    @Query("SELECT COUNT(c) FROM Cotacao c WHERE c.status = :status")
    long countByStatus(@Param("status") StatusCotacao status);

    // Buscar cotações com propostas (JOIN FETCH para evitar N+1)
    @Query("SELECT DISTINCT c FROM Cotacao c LEFT JOIN FETCH c.propostas WHERE c.id = :id")
    Cotacao findByIdWithPropostas(@Param("id") UUID id);
}