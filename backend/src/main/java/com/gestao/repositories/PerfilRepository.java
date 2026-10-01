package com.gestao.repositories;

import com.gestao.entities.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, UUID> {

    // Buscar perfil por nome
    @Query("""
              SELECT p FROM Perfil p 
              WHERE p.nome = :nome
            """)
    Optional<Perfil> findByNome(@Param("nome") String nome);

    // Verificar se perfil existe por nome
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Perfil p WHERE p.nome = :nome")
    boolean existsByNome(@Param("nome") String nome);
}