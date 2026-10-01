package com.gestao.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_fornecedor")
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome", length = 150, nullable = false)
    private String nomeCompleto;

    /** Armazenado apenas com dígitos (14 caracteres). */
    @Column(name = "cnpj", length = 14, nullable = false, unique = true)
    private String cnpj;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    /** Hash BCrypt da senha. */
    @Column(name = "senha", length = 100, nullable = false)
    private String senha;

    @ManyToOne
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil; // Sempre será "FORNECEDOR"
}
