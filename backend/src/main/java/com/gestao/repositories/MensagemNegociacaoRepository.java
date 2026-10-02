package com.gestao.repositories;

import com.gestao.entities.MensagemNegociacao;
import com.gestao.enums.TipoRemetente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // Mensagens da outra parte (quem ainda não leu nada)
    long countByNegociacaoIdAndTipoRemetenteNot(UUID negociacaoId, TipoRemetente leitor);

    // Mensagens da outra parte enviadas depois da última leitura
    long countByNegociacaoIdAndTipoRemetenteNotAndDataEnvioAfter(UUID negociacaoId, TipoRemetente leitor,
                                                                 LocalDateTime lidaEm);

    /** Mensagens do fornecedor que a empresa ainda não viu, agrupadas por negociação. */
    @Query("""
            SELECT n.id AS negociacaoId, COUNT(m) AS total
            FROM MensagemNegociacao m JOIN m.negociacao n
            WHERE n.empresa.id = :empresaId
              AND m.tipoRemetente = :remetente
              AND (n.lidaEmpresaEm IS NULL OR m.dataEnvio > n.lidaEmpresaEm)
            GROUP BY n.id
            """)
    List<NaoLidas> contarNaoLidasDaEmpresa(@Param("empresaId") UUID empresaId,
                                           @Param("remetente") TipoRemetente remetente);

    /** Mensagens da empresa que o fornecedor ainda não viu, agrupadas por negociação. */
    @Query("""
            SELECT n.id AS negociacaoId, COUNT(m) AS total
            FROM MensagemNegociacao m JOIN m.negociacao n
            WHERE n.fornecedor.id = :fornecedorId
              AND m.tipoRemetente = :remetente
              AND (n.lidaFornecedorEm IS NULL OR m.dataEnvio > n.lidaFornecedorEm)
            GROUP BY n.id
            """)
    List<NaoLidas> contarNaoLidasDoFornecedor(@Param("fornecedorId") UUID fornecedorId,
                                              @Param("remetente") TipoRemetente remetente);

    /** Projeção: quantas mensagens não lidas há em cada negociação. */
    interface NaoLidas {
        UUID getNegociacaoId();

        Long getTotal();
    }

    // Remover todas as mensagens de uma negociação
    @Modifying
    @Query("DELETE FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId")
    void deleteByNegociacaoId(@Param("negociacaoId") UUID negociacaoId);
}
