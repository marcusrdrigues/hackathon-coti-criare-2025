package com.gestao.entities;

import com.gestao.enums.StatusCotacao;  // ← MUDOU
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "tb_cotacao")
public class Cotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_servico", length = 200, nullable = false)
    private String nomeServico;

    @Column(name = "requisitos", length = 1000, nullable = false)
    private String requisitos;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_limite")
    private LocalDateTime dataLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusCotacao status;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToMany(mappedBy = "cotacao")
    private List<Proposta> propostas;
}