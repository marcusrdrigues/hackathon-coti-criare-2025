package com.gestao.compras.dominio;

import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_proposta")
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "valor_proposto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao", length = 1000, nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusProposta status;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    /** O fornecedor que propõe */
    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Organizacao fornecedor;

    /** Quem enviou */
    @ManyToOne
    @JoinColumn(name = "enviada_por", nullable = false)
    private Usuario enviadaPor;

    @ManyToOne
    @JoinColumn(name = "cotacao_id", nullable = false)
    private Cotacao cotacao;

    @OneToOne(mappedBy = "proposta")
    private Negociacao negociacao;
}
