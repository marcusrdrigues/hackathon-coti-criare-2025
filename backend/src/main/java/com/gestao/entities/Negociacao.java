package com.gestao.entities;

import com.gestao.enums.StatusNegociacao;  // ← MUDOU
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "tb_negociacao")
public class Negociacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "valor_final")
    private BigDecimal valorFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusNegociacao status;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "datafinalizacao")
    private LocalDate dataFinalizacao;

    @OneToOne
    @JoinColumn(name = "proposta_id", nullable = false, unique = true)
    private Proposta proposta;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @OneToMany(mappedBy = "negociacao")
    private List<MensagemNegociacao> mensagens;
}