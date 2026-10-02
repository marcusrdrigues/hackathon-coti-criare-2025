package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data para as empresas. Só a infraestrutura enxerga; a aplicação usa a porta. */
interface EmpresaJpa extends JpaRepository<Empresa, UUID> {

    Optional<Empresa> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);
}
