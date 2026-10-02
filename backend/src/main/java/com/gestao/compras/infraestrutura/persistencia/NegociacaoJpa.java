package com.gestao.compras.infraestrutura.persistencia;

import com.gestao.compras.dominio.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/** Spring Data para as negociações. */
interface NegociacaoJpa extends JpaRepository<Negociacao, UUID>, JpaSpecificationExecutor<Negociacao> {

    boolean existsByPropostaId(UUID propostaId);
}
