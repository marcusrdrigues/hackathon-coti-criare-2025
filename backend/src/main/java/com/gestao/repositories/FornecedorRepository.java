package com.gestao.repositories;

import com.gestao.entities.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {

    // Buscar fornecedor por email
    @Query("SELECT f FROM Fornecedor f WHERE f.email = :email")
    Optional<Fornecedor> findByEmail(@Param("email") String email);

    // Buscar fornecedor por CNPJ
    @Query("SELECT f FROM Fornecedor f WHERE f.cnpj = :cnpj")
    Optional<Fornecedor> findByCnpj(@Param("cnpj") String cnpj);

    // Verificar se email já existe
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Fornecedor f WHERE f.email = :email")
    boolean existsByEmail(@Param("email") String email);

    // Verificar se CNPJ já existe
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Fornecedor f WHERE f.cnpj = :cnpj")
    boolean existsByCnpj(@Param("cnpj") String cnpj);

    // Buscar fornecedor por email e senha (se precisar depois)
    @Query("SELECT f FROM Fornecedor f WHERE f.email = :email AND f.senha = :senha")
    Optional<Fornecedor> findByEmailAndSenha(@Param("email") String email, @Param("senha") String senha);

    // Buscar fornecedores com perfil específico
    @Query("SELECT f FROM Fornecedor f WHERE f.perfil.nome = :perfilNome")
    List<Fornecedor> findByPerfilNome(@Param("perfilNome") String perfilNome);

    // Buscar fornecedores por parte do nome (busca parcial)
    @Query("SELECT f FROM Fornecedor f WHERE LOWER(f.nomeCompleto) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Fornecedor> findByNomeCompletoContaining(@Param("nome") String nome);
}