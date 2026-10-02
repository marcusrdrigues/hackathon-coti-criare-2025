package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data para os perfis. */
interface PerfilJpa extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByNome(String nome);

    boolean existsByNome(String nome);
}
