package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data para as pessoas. */
interface UsuarioJpa extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findBySuperadminTrue();
}
