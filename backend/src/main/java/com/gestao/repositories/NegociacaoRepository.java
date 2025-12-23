package com.gestao.repositories;

import com.gestao.entities.Negociacao;
import com.gestao.enums.StatusNegociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NegociacaoRepository extends JpaRepository<Negociacao, UUID> {

    // Buscar negociação por proposta
    @Query("SELECT n FROM Negociacao n WHERE n.proposta.id = :propostaId")
    Optional<Negociacao> findByPropostaId(@Param("propostaId") UUID propostaId);

    // Buscar negociações de uma empresa
    @Query("SELECT n FROM Negociacao n WHERE n.empresa.id = :empresaId ORDER BY n.dataInicio DESC")
    List<Negociacao> findByEmpresaId(@Param("empresaId") UUID empresaId);

    // Buscar negociações de um fornecedor
    @Query("SELECT n FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId ORDER BY n.dataInicio DESC")
    List<Negociacao> findByFornecedorId(@Param("fornecedorId") UUID fornecedorId);

    // Buscar negociações por status
    @Query("SELECT n FROM Negociacao n WHERE n.status = :status ORDER BY n.dataInicio DESC")
    List<Negociacao> findByStatus(@Param("status") StatusNegociacao status);

    // Buscar negociações de uma empresa por status
    @Query("SELECT n FROM Negociacao n WHERE n.empresa.id = :empresaId AND n.status = :status ORDER BY n.dataInicio DESC")
    List<Negociacao> findByEmpresaIdAndStatus(
            @Param("empresaId") UUID empresaId,
            @Param("status") StatusNegociacao status
    );

    // Buscar negociações de um fornecedor por status
    @Query("SELECT n FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId AND n.status = :status ORDER BY n.dataInicio DESC")
    List<Negociacao> findByFornecedorIdAndStatus(
            @Param("fornecedorId") UUID fornecedorId,
            @Param("status") StatusNegociacao status
    );

    // Verificar se já existe negociação para uma proposta
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Negociacao n WHERE n.proposta.id = :propostaId")
    boolean existsByPropostaId(@Param("propostaId") UUID propostaId);

    // Buscar negociações em andamento de uma empresa
    @Query("SELECT n FROM Negociacao n WHERE n.empresa.id = :empresaId AND n.status = 'EM_ANDAMENTO' ORDER BY n.dataInicio DESC")
    List<Negociacao> findNegociacoesAtivasByEmpresaId(@Param("empresaId") UUID empresaId);

    // Buscar negociações em andamento de um fornecedor
    @Query("SELECT n FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId AND n.status = 'EM_ANDAMENTO' ORDER BY n.dataInicio DESC")
    List<Negociacao> findNegociacoesAtivasByFornecedorId(@Param("fornecedorId") UUID fornecedorId);

    // Buscar negociação com mensagens (JOIN FETCH para evitar N+1)
    @Query("SELECT DISTINCT n FROM Negociacao n LEFT JOIN FETCH n.mensagens WHERE n.id = :id")
    Optional<Negociacao> findByIdWithMensagens(@Param("id") UUID id);

    // Buscar negociação com proposta e cotação (JOIN FETCH)
    @Query("""
            SELECT n FROM Negociacao n
            JOIN FETCH n.proposta p
            JOIN FETCH p.cotacao c
            WHERE n.id = :id""")
    Optional<Negociacao> findByIdWithPropostaAndCotacao(@Param("id") UUID id);

    // Contar negociações de uma empresa
    @Query("SELECT COUNT(n) FROM Negociacao n WHERE n.empresa.id = :empresaId")
    long countByEmpresaId(@Param("empresaId") UUID empresaId);

    // Contar negociações de um fornecedor
    @Query("SELECT COUNT(n) FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId")
    long countByFornecedorId(@Param("fornecedorId") UUID fornecedorId);
}