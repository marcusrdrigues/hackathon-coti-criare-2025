package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.dominio.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data para as propostas. */
interface PropostaJpa extends JpaRepository<Proposta, UUID>, JpaSpecificationExecutor<Proposta> {

    @Query("SELECT p FROM Proposta p WHERE p.cotacao.id = :cotacaoId ORDER BY p.valor ASC")
    List<Proposta> daCotacao(@Param("cotacaoId") UUID cotacaoId);

    boolean existsByFornecedorIdAndCotacaoId(UUID fornecedorId, UUID cotacaoId);
}
