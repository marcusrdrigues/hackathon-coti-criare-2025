package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data para os fornecedores. Só a infraestrutura enxerga; a aplicação usa a porta. */
interface FornecedorJpa extends JpaRepository<Fornecedor, UUID> {

    Optional<Fornecedor> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);
}
