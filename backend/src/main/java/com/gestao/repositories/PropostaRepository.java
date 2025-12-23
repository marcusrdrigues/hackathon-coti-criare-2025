package com.gestao.repositories;

import com.gestao.entities.Proposta;
import com.gestao.enums.StatusProposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, UUID> {

    // Buscar propostas de uma cotação
    @Query("SELECT p FROM Proposta p WHERE p.cotacao.id = :cotacaoId ORDER BY p.valor ASC")
    List<Proposta> findByCotacaoId(@Param("cotacaoId") UUID cotacaoId);

    // Buscar propostas de um fornecedor
    @Query("SELECT p FROM Proposta p WHERE p.fornecedor.id = :fornecedorId ORDER BY p.valor DESC")
    List<Proposta> findByFornecedorId(@Param("fornecedorId") UUID fornecedorId);

    // Buscar proposta específica de um fornecedor para uma cotação
    @Query("SELECT p FROM Proposta p WHERE p.fornecedor.id = :fornecedorId AND p.cotacao.id = :cotacaoId")
    Optional<Proposta> findByFornecedorIdAndCotacaoId(
            @Param("fornecedorId") UUID fornecedorId,
            @Param("cotacaoId") UUID cotacaoId
    );

    // Buscar propostas por status
    @Query("SELECT p FROM Proposta p WHERE p.status = :status ORDER BY p.valor ASC")
    List<Proposta> findByStatus(@Param("status") StatusProposta status);

    // Buscar propostas de uma cotação por status
    @Query("SELECT p FROM Proposta p WHERE p.cotacao.id = :cotacaoId AND p.status = :status ORDER BY p.valor ASC")
    List<Proposta> findByCotacaoIdAndStatus(
            @Param("cotacaoId") UUID cotacaoId,
            @Param("status") StatusProposta status
    );

    // Buscar propostas de um fornecedor por status
    @Query("SELECT p FROM Proposta p WHERE p.fornecedor.id = :fornecedorId AND p.status = :status ORDER BY p.valor DESC")
    List<Proposta> findByFornecedorIdAndStatus(
            @Param("fornecedorId") UUID fornecedorId,
            @Param("status") StatusProposta status
    );

    // Verificar se fornecedor já enviou proposta para uma cotação
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Proposta p WHERE p.fornecedor.id = :fornecedorId AND p.cotacao.id = :cotacaoId")
    boolean existsByFornecedorIdAndCotacaoId(
            @Param("fornecedorId") UUID fornecedorId,
            @Param("cotacaoId") UUID cotacaoId
    );

    // Contar propostas de uma cotação
    @Query("SELECT COUNT(p) FROM Proposta p WHERE p.cotacao.id = :cotacaoId")
    long countByCotacaoId(@Param("cotacaoId") UUID cotacaoId);

    // Buscar propostas com valor menor ou igual (para filtros)
    @Query("SELECT p FROM Proposta p WHERE p.cotacao.id = :cotacaoId AND p.valor <= :valorMaximo ORDER BY p.valor ASC")
    List<Proposta> findByCotacaoIdAndValorLessThanEqual(
            @Param("cotacaoId") UUID cotacaoId,
            @Param("valorMaximo") BigDecimal valorMaximo
    );

    // Buscar menor proposta de uma cotação
    @Query("SELECT p FROM Proposta p WHERE p.cotacao.id = :cotacaoId AND p.status = :status ORDER BY p.valor ASC")
    List<Proposta> findTopByCotacaoIdAndStatusOrderByValorAsc(
            @Param("cotacaoId") UUID cotacaoId,
            @Param("status") StatusProposta status
    );

    // Buscar proposta com negociação (JOIN FETCH)
    @Query("SELECT p FROM Proposta p LEFT JOIN FETCH p.negociacao WHERE p.id = :id")
    Optional<Proposta> findByIdWithNegociacao(@Param("id") UUID id);
}