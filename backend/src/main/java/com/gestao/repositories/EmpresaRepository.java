package com.gestao.repositories;

import com.gestao.entities.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    // Buscar empresa por email
    @Query("SELECT e FROM Empresa e WHERE e.email = :email")
    Optional<Empresa> findByEmail(@Param("email") String email);

    // Buscar empresa por CNPJ
    @Query("SELECT e FROM Empresa e WHERE e.cnpj = :cnpj")
    Optional<Empresa> findByCnpj(@Param("cnpj") String cnpj);

    // Verificar se email já existe
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empresa e WHERE e.email = :email")
    boolean existsByEmail(@Param("email") String email);

    // Verificar se CNPJ já existe
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Empresa e WHERE e.cnpj = :cnpj")
    boolean existsByCnpj(@Param("cnpj") String cnpj);

    // Buscar empresa por email e senha (se precisar depois)
    @Query("SELECT e FROM Empresa e WHERE e.email = :email AND e.senha = :senha")
    Optional<Empresa> findByEmailAndSenha(@Param("email") String email, @Param("senha") String senha);

    // Buscar empresas com perfil específico
    @Query("SELECT e FROM Empresa e WHERE e.perfil.nome = :perfilNome")
    List<Empresa> findByPerfilNome(@Param("perfilNome") String perfilNome);

    // Buscar empresas por parte do nome (busca parcial)
    @Query("SELECT e FROM Empresa e WHERE LOWER(e.razaoSocial) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Empresa> findByRazaoSocialContaining(@Param("nome") String nome);
}