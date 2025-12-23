package com.gestao.repositories;

import com.gestao.entities.MensagemNegociacao;
import com.gestao.enums.TipoRemetente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemNegociacaoRepository extends JpaRepository<MensagemNegociacao, UUID> {

    // Buscar mensagens de uma negociação (ordenadas por data)
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> findByNegociacaoIdOrderByDataEnvioAsc(@Param("negociacaoId") UUID negociacaoId);

    // Buscar mensagens de uma negociação por tipo de remetente
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId AND m.tipoRemetente = :tipoRemetente ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> findByNegociacaoIdAndTipoRemetente(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("tipoRemetente") TipoRemetente tipoRemetente
    );

    // Buscar mensagens de um remetente específico em uma negociação
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId AND m.remetenteId = :remetenteId ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> findByNegociacaoIdAndRemetenteId(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("remetenteId") UUID remetenteId
    );

    // Contar mensagens de uma negociação
    @Query("SELECT COUNT(m) FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId")
    long countByNegociacaoId(@Param("negociacaoId") UUID negociacaoId);

    // Contar mensagens por tipo de remetente
    @Query("SELECT COUNT(m) FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId AND m.tipoRemetente = :tipoRemetente")
    long countByNegociacaoIdAndTipoRemetente(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("tipoRemetente") TipoRemetente tipoRemetente
    );

    // Buscar mensagens por data
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId AND m.dataEnvio = :data ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> findByNegociacaoIdAndDataEnvio(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("data") LocalDate data
    );

    // Buscar mensagens por período
    @Query(""" 
                SELECT m FROM MensagemNegociacao m 
                WHERE m.negociacao.id = :negociacaoId 
                AND m.dataEnvio 
                BETWEEN :dataInicio 
                AND :dataFim 
                ORDER BY m.dataEnvio ASC""")
    List<MensagemNegociacao> findByNegociacaoIdAndDataEnvioBetween(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    // Buscar última mensagem de uma negociação
    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId ORDER BY m.dataEnvio DESC")
    List<MensagemNegociacao> findLastMessageByNegociacaoId(@Param("negociacaoId") UUID negociacaoId);

    // Buscar mensagens com texto específico (busca parcial)
    @Query("""
                SELECT m FROM MensagemNegociacao m 
                WHERE m.negociacao.id = :negociacaoId 
                AND LOWER(m.mensagem) 
                LIKE LOWER(CONCAT('%', :texto, '%')) 
                ORDER BY m.dataEnvio ASC
            """)
    List<MensagemNegociacao> findByNegociacaoIdAndMensagemContaining(
            @Param("negociacaoId") UUID negociacaoId,
            @Param("texto") String texto
    );

    // Deletar todas as mensagens de uma negociação
    @Query("DELETE FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId")
    void deleteByNegociacaoId(@Param("negociacaoId") UUID negociacaoId);
}