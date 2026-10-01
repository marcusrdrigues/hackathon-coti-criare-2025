package com.gestao.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_perfil")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome", length = 25, nullable = false, unique = true)
    private String nome; // "EMPRESA" ou "FORNECEDOR"
}
