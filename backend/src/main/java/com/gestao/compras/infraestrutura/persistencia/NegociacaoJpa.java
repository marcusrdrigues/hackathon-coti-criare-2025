package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.dominio.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data para as negociações. */
interface NegociacaoJpa extends JpaRepository<Negociacao, UUID> {

    boolean existsByPropostaId(UUID propostaId);

    @Query("SELECT n FROM Negociacao n WHERE n.empresa.id = :empresaId ORDER BY n.dataInicio DESC")
    List<Negociacao> daEmpresa(@Param("empresaId") UUID empresaId);

    @Query("SELECT n FROM Negociacao n WHERE n.fornecedor.id = :fornecedorId ORDER BY n.dataInicio DESC")
    List<Negociacao> doFornecedor(@Param("fornecedorId") UUID fornecedorId);
}
