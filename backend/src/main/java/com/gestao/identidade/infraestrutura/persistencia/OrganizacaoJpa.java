package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.TipoOrganizacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data para as organizações. */
interface OrganizacaoJpa extends JpaRepository<Organizacao, UUID> {

    boolean existsByCnpjAndTipo(String cnpj, TipoOrganizacao tipo);
}
