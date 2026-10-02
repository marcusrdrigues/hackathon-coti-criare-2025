package com.gestao.painel.infraestrutura.persistencia;

import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;
import com.gestao.compras.dominio.StatusNegociacao;
import com.gestao.compras.dominio.StatusProposta;
import com.gestao.painel.aplicacao.porta.CategoriaResumoProjection;
import com.gestao.painel.aplicacao.porta.FornecedorResumoProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Consultas agregadas dos painéis, em JPQL, sobre as tabelas do módulo de compras. */
interface PainelJpa extends Repository<Cotacao, UUID> {

    @Query("SELECT COUNT(c) FROM Cotacao c WHERE c.empresa.id = :empresaId AND c.status = :status")
    long contarCotacoesDaEmpresa(@Param("empresaId") UUID empresaId, @Param("status") StatusCotacao status);

    @Query("SELECT COUNT(p) FROM Proposta p WHERE p.cotacao.empresa.id = :empresaId")
    long contarPropostasRecebidas(@Param("empresaId") UUID empresaId);

    @Query("""
            SELECT f.id AS id, f.razaoSocial AS nome, f.cnpj AS cnpj, COUNT(p) AS totalPropostas
            FROM Proposta p
            JOIN p.fornecedor f
            WHERE p.cotacao.empresa.id = :empresaId
            GROUP BY f.id, f.razaoSocial, f.cnpj
            ORDER BY COUNT(p) DESC""")
    List<FornecedorResumoProjection> fornecedoresQueMaisPropuseram(@Param("empresaId") UUID empresaId, Pageable pagina);

    @Query("""
            SELECT c.categoria AS categoria, COUNT(c) AS total
            FROM Cotacao c
            WHERE c.empresa.id = :empresaId
            GROUP BY c.categoria""")
    List<CategoriaResumoProjection> cotacoesPorCategoria(@Param("empresaId") UUID empresaId);

    @Query("""
            SELECT COUNT(c) FROM Cotacao c
            WHERE c.status = :status
            AND (c.dataLimite IS NULL OR c.dataLimite > :agora)""")
    long contarCotacoesEmVigor(@Param("status") StatusCotacao status, @Param("agora") LocalDateTime agora);

    @Query("SELECT COUNT(p) FROM Proposta p WHERE p.fornecedor.id = :fornecedorId AND p.status IN :status")
    long contarPropostasDoFornecedor(@Param("fornecedorId") UUID fornecedorId,
                                     @Param("status") Collection<StatusProposta> status);

    @Query("SELECT COUNT(n) FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId AND n.status = :status")
    long contarNegociacoesDoFornecedor(@Param("fornecedorId") UUID fornecedorId,
                                       @Param("status") StatusNegociacao status);

    @Query("SELECT SUM(n.valorFinal) FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId AND n.status = :status")
    BigDecimal somarValorFinalDoFornecedor(@Param("fornecedorId") UUID fornecedorId,
                                           @Param("status") StatusNegociacao status);
}
