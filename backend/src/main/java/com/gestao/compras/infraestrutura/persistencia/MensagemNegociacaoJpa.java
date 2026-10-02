package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.aplicacao.porta.MensagemNegociacaoRepositorio.NaoLidas;
import com.gestao.compras.dominio.MensagemNegociacao;
import com.gestao.compras.dominio.TipoRemetente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Spring Data para as mensagens de negociação. */
interface MensagemNegociacaoJpa extends JpaRepository<MensagemNegociacao, UUID> {

    @Query("SELECT m FROM MensagemNegociacao m WHERE m.negociacao.id = :negociacaoId ORDER BY m.dataEnvio ASC")
    List<MensagemNegociacao> daNegociacao(@Param("negociacaoId") UUID negociacaoId);

    long countByNegociacaoIdAndTipoRemetenteNot(UUID negociacaoId, TipoRemetente leitor);

    long countByNegociacaoIdAndTipoRemetenteNotAndDataEnvioAfter(UUID negociacaoId, TipoRemetente leitor,
                                                                 LocalDateTime lidaEm);

    @Query("""
            SELECT n.id AS negociacaoId, COUNT(m) AS total
            FROM MensagemNegociacao m JOIN m.negociacao n
            WHERE n.empresa.id = :empresaId
              AND m.tipoRemetente = :remetente
              AND (n.lidaEmpresaEm IS NULL OR m.dataEnvio > n.lidaEmpresaEm)
            GROUP BY n.id
            """)
    List<NaoLidas> naoLidasDaEmpresa(@Param("empresaId") UUID empresaId,
                                     @Param("remetente") TipoRemetente remetente);

    @Query("""
            SELECT n.id AS negociacaoId, COUNT(m) AS total
            FROM MensagemNegociacao m JOIN m.negociacao n
            WHERE n.fornecedor.id = :fornecedorId
              AND m.tipoRemetente = :remetente
              AND (n.lidaFornecedorEm IS NULL OR m.dataEnvio > n.lidaFornecedorEm)
            GROUP BY n.id
            """)
    List<NaoLidas> naoLidasDoFornecedor(@Param("fornecedorId") UUID fornecedorId,
                                        @Param("remetente") TipoRemetente remetente);
}
