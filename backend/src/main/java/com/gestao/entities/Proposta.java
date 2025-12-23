package com.gestao.entities;

import com.gestao.enums.StatusProposta;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@Table(name = "tb_proposta")
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "valor_proposto", nullable = false)
    private BigDecimal valor;

    @Column(name = "descricao", length = 1000, nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusProposta status;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne
    @JoinColumn(name = "cotacao_id", nullable = false)
    private Cotacao cotacao;

    @OneToOne(mappedBy = "proposta")
    private Negociacao negociacao;
}