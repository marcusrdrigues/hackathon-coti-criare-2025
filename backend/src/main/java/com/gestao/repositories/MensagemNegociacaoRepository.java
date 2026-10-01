package com.gestao.repositories;

import com.gestao.entities.MensagemNegociacao;
import com.gestao.enums.TipoRemetente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemNegociacaoRepository extends JpaRepository<MensagemNegociacao, UUID> {

    // Mensagens de uma negociação em ordem cronológica
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> findByNegociacaoIdOrderByDataEnvioAsc(@Param("negociacaoId") UUID negociacaoId);

    // Mensagens de uma negociação por tipo de remetente
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId AND m.tipoRemetente = :tipoRemetente ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> findByNegociacaoIdAndTipoRemetente(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("tipoRemetente") TipoRemetente tipoRemetente
    );

    // Contar mensagens de uma negociação
    @Query("SELECT COUNT(m) FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId")
    long countByNegociacaoId(@Param("negociacaoId") UUID negociacaoId);

    // Remover todas as mensagens de uma negociação
    @Modifying
    @Query("DELETE FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId")
    void deleteByNegociacaoId(@Param("negociacaoId") UUID negociacaoId);
}
