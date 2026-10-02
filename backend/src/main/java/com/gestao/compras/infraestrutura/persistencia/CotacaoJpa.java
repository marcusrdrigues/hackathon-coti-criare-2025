package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.dominio.Cotacao;
import com.gestao.compras.dominio.StatusCotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data para as cotações. */
interface CotacaoJpa extends JpaRepository<Cotacao, UUID>, JpaSpecificationExecutor<Cotacao> {

    /** Total de cotações numa situação. */
    interface ContagemPorSituacao {
        StatusCotacao getStatus();

        Long getTotal();
    }

    @Query("SELECT c.status AS status, COUNT(c) AS total FROM Cotacao c WHERE c.empresa.id = :empresaId GROUP BY c.status")
    List<ContagemPorSituacao> contarPorSituacao(@Param("empresaId") UUID empresaId);
}
